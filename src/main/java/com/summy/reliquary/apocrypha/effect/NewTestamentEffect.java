package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.entity.HolyArrow;
import com.summy.reliquary.apocrypha.entity.NewTestamentArrow;
import com.summy.reliquary.entity.ThrownRazor;
import com.summy.reliquary.entity.ThrownSpear;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// 「新约」（落箭）与「比翼·连携」（穿透圣光矢）的命中判定、掷点、音效与调度，
// 外加给 SR 真伤补的「命中后清帧」。
//
// 触发条件：弓或弩射出的箭矢命中目标（AbstractArrow + owner 是佩戴这两件之一的玩家、命中的不是自己）。
// 明确排除 SR 的幻影矛（ThrownSpear）与金刀片（ThrownRazor），以及本模组的落箭与圣光矢（避免重复触发与递归）。
// 两件同戴时两者永远协同触发（详见 onLivingHurt 的步骤③）。
//
// 用 LOWEST 优先级：此时其它模组（含 SR）的取消与数值改写都已完成，我们只读事实、不回写伤害。
public final class NewTestamentEffect {
	// 调试日志用：最近一次「本模组落箭」命中在 LivingHurtEvent（LOWEST）里看到的金额（= 各类倍率结算后）。
	// 事件是在 NewTestamentArrow.onHitEntity → hurt() 内部同步触发的，所以同一击内读它一定是本次的值；
	// 只在服务端主线程读写，且只服务调试日志，不参与任何玩法判定。
	private static float lastArrowHitAmount;
	// 同上：这一击是否来自「圣光矢」（只影响命中行的口径措辞）
	private static boolean lastArrowHitHoly;
	// 同上：这一击箭上携带的钉住值（只为日志措辞用，不参与任何玩法判定）
	private static float lastArrowHitPinnedValue;

	private NewTestamentEffect() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurt(LivingHurtEvent event) {
		// 只在服务端处理（LivingHurtEvent 双端都会触发）
		if (!(event.getEntity().level() instanceof ServerLevel level)) {
			return;
		}

		// ① 本模组投掷物（落箭 / 圣光矢）的命中：先把金额钉回携带值，再记下本击金额供实体打日志；
		//    这两者自身都不再触发新约
		Entity directEntity = event.getSource().getDirectEntity();
		if (directEntity instanceof NewTestamentArrow arrow) {
			recordOurArrowHit(event, arrow.pinnedDamage(), false);
			return;
		}
		if (directEntity instanceof HolyArrow arrow) {
			recordOurArrowHit(event, arrow.pinnedDamage(), true);
			return;
		}

		// ② SR 真伤的「命中后清帧」补强（独立开关，不受 [general] enable 影响）
		if (!event.isCanceled() && ApocryphaConfig.clearSrIframesAfterHit()) {
			TrueDamageFrames.handle(event, level);
		}

		// ③ 本体箭矢命中目标：新约（落箭）与比翼·连携（穿透圣光矢）各自掷点，两件同戴时协同触发
		if (event.isCanceled() || !ApocryphaConfig.enable()) {
			return;
		}
		if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
			return;
		}
		// SR 的幻影矛 / 金刀片，以及本模组的落箭与圣光矢都不算"本体箭矢"
		if (arrow instanceof ThrownSpear || arrow instanceof ThrownRazor
				|| arrow instanceof NewTestamentArrow || arrow instanceof HolyArrow) {
			return;
		}
		if (!(arrow.getOwner() instanceof ServerPlayer shooter)) {
			return;
		}
		LivingEntity target = event.getEntity();
		if (target == shooter) {
			return;
		}
		boolean newTestament = CurioHelper.wears(shooter, Apocrypha.NEW_TESTAMENT.get());
		boolean pairedWings = PairedWings.wears(shooter);
		if (!newTestament && !pairedWings) {
			return;
		}

		// 触发之箭在「各类倍率结算后」的金额：新约落箭的钉住基准与圣光矢的伤害基准都用它
		float hitAmount = event.getAmount();

		// 比翼·连携：圣光矢的独立掷点（各自佩戴各自掷、各自消耗一次随机数）
		float holyDamage = (float) (hitAmount * ApocryphaConfig.holyArrowDamageRatioPercent() / 100.0D);
		double holyRoll = pairedWings ? level.random.nextDouble() * 100.0D : 0.0D;
		boolean holySuccess = pairedWings && holyRoll < ApocryphaConfig.holyArrowChancePercent();

		// 新约侧：完全沿用原有口径（圣餐必定触发、迅羽替换概率、逆羽下落 ×1.5）
		boolean ntSuccess = false;
		boolean sacrament = false;
		boolean hovering = false;
		double speedMultiplier = 1.0D;
		float pinnedDamage = -1.0F;
		double roll = 0.0D;
		double chance = 0.0D;
		if (newTestament) {
			// 迅羽·协约：与「新约」同戴时改写落箭的三项参数（概率 / 初速 / 伤害口径）
			boolean upgraded = CurioHelper.wears(shooter, Apocrypha.SWIFT_FEATHER_COVENANT.get());
			speedMultiplier = upgraded ? 1.0D + ApocryphaConfig.swiftSpeedBonusPercent() / 100.0D : 1.0D;
			// 落箭的单支伤害**一律钉住**（不吃 SR 的增伤乘区）：
			// 同戴迅羽 → 触发之箭金额 × 比例；未同戴 → 固定用 arrow_damage。
			// 钉住值 <= 0 时才退化回"不钉住"（例如把 arrow_damage 配成 0）。
			pinnedDamage = upgraded
					? (float) (hitAmount * ApocryphaConfig.swiftDamageRatioPercent() / 100.0D)
					: (float) ApocryphaConfig.arrowDamage();
			// 圣餐之矢（翼羽·圣餐）：这一发被标记过 → 必定触发、落箭数量固定
			sacrament = SacramentArrows.isSacrament(event.getSource());
			// 逆羽·滞空：命中那一刻射手仍在下落 → 触发概率倍率 + 落箭伤害改为瞬时结算
			hovering = HoverFlight.wears(shooter) && HoverFlight.isFalling(shooter);
			// 掷点：每命中恰好消耗一次随机数（与是否触发、是否记日志都无关）
			roll = level.random.nextDouble() * 100.0D;
			double baseChance = upgraded
					? ApocryphaConfig.swiftChancePercent()
					: ApocryphaConfig.scatterChancePercent();
			// 逆羽的 ×1.5 作用在"生效基础值"上（同戴迅羽 → 50% × 1.5 = 75%）
			chance = hovering
					? baseChance * ApocryphaConfig.hoverFallingChancePercent() / 100.0D
					: baseChance;
			ntSuccess = sacrament || roll < chance;

			// 命中回血（这一次就是「新约治疗触发」）
			shooter.heal((float) ApocryphaConfig.healAmount());
			// 翼羽·圣餐：每次治疗触发在背后凝聚一对光羽
			if (CurioHelper.wears(shooter, Apocrypha.WING_FEATHER_SACRAMENT.get())) {
				FeatherCharge.addPair(shooter);
			}
		}

		// 协同（仅两件同戴）：任一自然触发都必定带出另一个（延迟 0.1 秒）；
		// 两次掷点都成功则圣光矢与落箭各 +1 —— 被强制带出的那一次不再掷点、也不计 +1
		boolean coupled = newTestament && pairedWings;
		boolean bothSuccess = coupled && ntSuccess && holySuccess;
		int holyNow = holySuccess ? (bothSuccess ? 1 + PairedWings.COUPLED_BONUS_ARROWS : 1) : 0;
		int holyLater = coupled && ntSuccess && !holySuccess ? 1 : 0;
		boolean volleyNow = ntSuccess;
		boolean volleyLater = coupled && !ntSuccess && holySuccess;

		int count = 0;
		if (volleyNow || volleyLater) {
			count = (sacrament ? ApocryphaConfig.sacramentArrowCount() : rollArrowCount(level))
					+ (bothSuccess ? 1 : 0);
			if (volleyNow) {
				// 听觉反馈：只发给触发者本人
				shooter.playNotifySound(SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
				if (hovering) {
					// 逆羽·滞空：伤害在触发这一刻逐笔瞬时结算，箭矢只作视觉
					settleInstantly(level, shooter, target, count, speedMultiplier, pinnedDamage);
				} else {
					ScatterVolley.enqueue(level, shooter, target, count, speedMultiplier, pinnedDamage, false, 0);
				}
			} else {
				// 被圣光矢协同带出的落箭：整体延后 0.1 秒（逆羽下落中仍是瞬时结算口径，只是延后）
				PairedWings.enqueueVolley(level, shooter, target, ScatterVolley.centerOf(target), count,
						speedMultiplier, pinnedDamage, hovering);
			}
		}
		if (holyNow > 0) {
			PairedWings.fire(level, shooter, holyNow, holyDamage);
		}
		if (holyLater > 0) {
			PairedWings.enqueueHoly(level, shooter, holyLater, holyDamage);
		}
		if (sacrament) {
			onSacramentHit(shooter, target);
		}
		if (newTestament) {
			logTrigger(shooter, target, roll, chance,
					volleyOutcome(sacrament, ntSuccess, volleyLater, bothSuccess, count));
		}
		if (pairedWings) {
			logHolyRoll(shooter, holyRoll, holySuccess);
		}
		if (coupled) {
			logCoupling(shooter, ntSuccess, holySuccess, bothSuccess, holyLater, volleyLater);
		}
	}

	// 服务端 tick 末尾：放出待落的箭 + 处理推迟到此刻的清帧（Kilt 侧）
	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		ScatterVolley.tick(event.getServer());
		TrueDamageFrames.tickServer();
		// 新约系列栏位的动态格数：仅天使线开放（每秒校正一次）
		ApocryphaSlotSizing.tickServer(event.getServer());
		// 光羽：脱战衰减 + 实体实体校正（每秒一次）
		FeatherCharge.tickServer(event.getServer());
		// 逆羽·滞空：缓降 + 落地重置空中跳数（每 tick）
		HoverFlight.tickServer(event.getServer());
		// 比翼·连携：放出延迟补发的圣光矢 / 落箭批次
		PairedWings.tick(event.getServer());
	}

	// 服务器停止：清空待落队列与清帧登记，避免残留到下一个存档
	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ScatterVolley.clear();
		TrueDamageFrames.clear();
		SacramentArrows.clear();
		HoverFlight.clear();
		PairedWings.clear();
	}

	/**
	 * 本模组投掷物（落箭 / 圣光矢）的命中：把金额钉回携带值，并记下本击金额供实体打日志用。
	 *
	 * <p>钉住的原理：SR 的七罪 / 七德与最终倍率乘区在 NORMAL 优先级已经把它放大过，
	 * 这里（LOWEST，且排在同优先级的 SR 处理器之前）改回钉住值 —— 于是最终落地金额
	 * 正好是触发那一刻算出来的数（迅羽档 = 触发箭金额的 N%；圣光矢 = 触发箭金额 + 配置加成）。
	 */
	private static void recordOurArrowHit(LivingHurtEvent event, float pinned, boolean holy) {
		if (pinned > 0.0F && !event.isCanceled()) {
			event.setAmount(pinned);
		}
		lastArrowHitHoly = holy;
		lastArrowHitPinnedValue = pinned;
		lastArrowHitAmount = event.isCanceled() ? 0.0F : event.getAmount();
	}

	/**
	 * 命中的口径说明（只服务调试日志）：本模组的落箭与圣光矢都**钉住**，不再吃 SR 的增伤乘区。
	 *
	 * <p>钉住值 {@code <= 0} 是"没有钉住"的退化情形（例如把 {@code arrow_damage} 配成 0），
	 * 那时仍按"各类倍率后"措辞。
	 */
	private static String hitBasis(boolean holy, float pinned) {
		if (holy) {
			return String.format("钉住：触发箭伤害的 %d%%", ApocryphaConfig.holyArrowDamageRatioPercent());
		}
		if (pinned > 0.0F) {
			return String.format("钉住：%.1f 点（不吃 SR 增伤乘区）", pinned);
		}
		return "各类倍率后";
	}

	/** 退服：清掉该玩家的空中跳计数（只存内存，不写存档） */
	@SubscribeEvent
	public static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			HoverFlight.forget(player);
		}
	}

	/** 死亡：同样清掉空中跳计数（重生后是一段新的离地） */
	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			HoverFlight.forget(player);
		}
	}

	/**
	 * 瞬时结算（逆羽·滞空）：按箭数逐笔当场结算，口径与物理命中完全一致
	 * （同一真实伤害类型、命中前后各清一次无敌帧、无击退的速度还原）。
	 *
	 * <p>先生成一支落箭当"伤害来源"并把它作为**直接来源**：这样 SR 仍按「远程」口径处理（与物理落箭一致），
	 * 迅羽档的钉住值也照旧生效（日志由本方法汇总成一行：×N 笔、合计多少点）。
	 * 其余的箭交回错峰队列慢慢落下来，只作视觉。
	 */
	static void settleInstantly(ServerLevel level, ServerPlayer shooter, LivingEntity target, int count,
			double speedMultiplier, float pinnedDamage) {
		NewTestamentArrow source = ScatterVolley.spawnOne(level, shooter, target, speedMultiplier, pinnedDamage,
				true, true);
		float perHit = pinnedDamage > 0.0F ? pinnedDamage : (float) ApocryphaConfig.arrowDamage();
		int landedCount = 0;
		double total = 0.0D;
		for (int index = 0; index < count; index++) {
			Vec3 velocityBefore = target.getDeltaMovement();
			target.invulnerableTime = 0;
			boolean landed = target.hurt(source.damageSource(level, shooter), perHit);
			if (!landed) {
				continue;
			}
			// 无击退：把速度还原并同步客户端（与物理落箭同一手法）
			target.setDeltaMovement(velocityBefore);
			target.hurtMarked = true;
			target.invulnerableTime = 0;
			landedCount++;
			// 每笔的金额由 LOWEST 处理器写进 lastArrowHitAmount（各类倍率后 / 钉住值）
			total += lastArrowHitAmount;
		}
		if (ApocryphaConfig.logTrigger()) {
			String basis = hitBasis(lastArrowHitHoly, lastArrowHitPinnedValue);
			Apocrypha.LOGGER.info(String.format("[新约] 落箭瞬时结算：%s ×%d 笔、合计 %.2f 点（口径：%s）",
					target.getName().getString(), landedCount, total, basis));
		}
		if (count > 1) {
			ScatterVolley.enqueue(level, shooter, target, count - 1, speedMultiplier, pinnedDamage, true, 0);
		}
	}

	// 本次触发的落箭支数
	private static int rollArrowCount(ServerLevel level) {
		int min = Math.min(ApocryphaConfig.scatterArrowMin(), ApocryphaConfig.scatterArrowMax());
		int max = Math.max(ApocryphaConfig.scatterArrowMin(), ApocryphaConfig.scatterArrowMax());
		return min + level.random.nextInt(max - min + 1);
	}

	/** 新约侧的掷点结果措辞（供触发行使用） */
	private static String volleyOutcome(boolean sacrament, boolean triggered, boolean forced, boolean bonus,
			int count) {
		String suffix = bonus ? "（共同触发 +1 支）" : "";
		if (sacrament) {
			return String.format("必定触发（圣餐之矢），落箭 %d 支（间隔 %d tick）%s", count,
					ScatterVolley.SPAWN_INTERVAL_TICKS, suffix);
		}
		if (triggered) {
			return String.format("触发，落箭 %d 支（间隔 %d tick）%s", count,
					ScatterVolley.SPAWN_INTERVAL_TICKS, suffix);
		}
		if (forced) {
			return String.format("未触发（被「比翼·连携」协同带出，延迟 %d tick）→ 落箭 %d 支%s",
					PairedWings.COUPLED_DELAY_TICKS, count, suffix);
		}
		return "未触发";
	}

	// 触发行：每次本体命中一行（含未触发的那几次），用来看 35% 是否符合预期
	private static void logTrigger(ServerPlayer shooter, LivingEntity target, double roll, double chance,
			String outcome) {
		if (!ApocryphaConfig.logTrigger()) {
			return;
		}
		Apocrypha.LOGGER.info(String.format("[新约] %s 箭矢命中 %s：掷点 %.1f（阈值 %.1f）→ %s；回血 %.2f",
				shooter.getName().getString(), target.getName().getString(), roll, chance, outcome,
				ApocryphaConfig.healAmount()));
	}

	// 圣光矢掷点行：与佩戴者无关的独立概率，用来看 20% 是否符合预期
	private static void logHolyRoll(ServerPlayer shooter, double roll, boolean success) {
		if (!ApocryphaConfig.logTrigger()) {
			return;
		}
		Apocrypha.LOGGER.info(String.format("[新约] 圣光矢：%s 掷点 %.1f（阈值 %d）→ %s",
				shooter.getName().getString(), roll, ApocryphaConfig.holyArrowChancePercent(),
				success ? "触发" : "未触发"));
	}

	// 协同行：只记事实（谁自然成功、谁被带出、有没有共同加成的 +1）
	private static void logCoupling(ServerPlayer shooter, boolean ntSuccess, boolean holySuccess,
			boolean bothSuccess, int holyLater, boolean volleyLater) {
		if (!ApocryphaConfig.logTrigger()) {
			return;
		}
		String nt = ntSuccess ? "自然" : (volleyLater ? "强制(" + PairedWings.COUPLED_DELAY_TICKS + " tick)" : "未触发");
		String holy = holySuccess ? "自然" : (holyLater > 0 ? "强制(" + PairedWings.COUPLED_DELAY_TICKS + " tick)" : "未触发");
		String bonus = bothSuccess
				? String.format("+%d 道 / +1 支", PairedWings.COUPLED_BONUS_ARROWS)
				: "无";
		Apocrypha.LOGGER.info(String.format("[新约] 协同触发：%s 新约=%s、圣光矢=%s、共同加成=%s",
				shooter.getName().getString(), nt, holy, bonus));
	}

	/**
	 * 圣餐之矢命中后的扩展点。
	 *
	 * <p>TODO（比翼·连携）：那件饰品要在这里接「圣餐之矢同时触发圣光矢」——判断玩家是否佩戴它、
	 * 然后触发对应的圣光矢逻辑即可，其它地方不需要改动。
	 */
	private static void onSacramentHit(ServerPlayer shooter, LivingEntity target) {
	}

	/**
	 * 命中行：每支落箭命中一行（由落箭实体在 hurt() 落地、且击退已被还原之后调用，因此能给出还原后的真值）。
	 *
	 * <p>金额取 {@code LivingHurtEvent}（LOWEST）里那次的值 = 各类倍率结算后（含 SR 的七罪/七德与最终倍率乘区）。
	 * 「击退被抵消」= 原版施加击退后的速度与命中前速度之差的模长 —— 非 0 就说明原版确实施加了击退、又被我们抵消掉了。
	 */
	public static void logArrowHit(LivingEntity victim, float healthBefore, Vec3 velocityBefore,
			Vec3 velocityKnocked, Vec3 velocityAfter) {
		if (!ApocryphaConfig.logTrigger()) {
			return;
		}
		// 钉住档不能写「各类倍率后」（那是被改回之前的口径），按实际来源措辞
		String basis = hitBasis(lastArrowHitHoly, lastArrowHitPinnedValue);
		Apocrypha.LOGGER.info(String.format(
				"[新约] 落箭命中 %s：结算 %.2f 点（%s）、命中前血量 %.1f/%.1f、"
						+ "速度 命中前(%.2f,%.2f,%.2f) → 击退后(%.2f,%.2f,%.2f) → 还原后(%.2f,%.2f,%.2f)、击退被抵消 %.3f",
				victim.getName().getString(), lastArrowHitAmount, basis, healthBefore, victim.getMaxHealth(),
				velocityBefore.x, velocityBefore.y, velocityBefore.z,
				velocityKnocked.x, velocityKnocked.y, velocityKnocked.z,
				velocityAfter.x, velocityAfter.y, velocityAfter.z,
				velocityKnocked.subtract(velocityBefore).length()));
	}
}
