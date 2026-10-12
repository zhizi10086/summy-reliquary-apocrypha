package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.net.ApocryphaNetworking;
import com.summy.reliquary.apocrypha.net.ApoptosisBarMessage;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 恶魔线「凋亡」套系的核心机制（服务端）：凋亡损伤条、爆条、损伤状态、损伤抵抗、元素伤害。
 *
 * <p>**循环**：佩戴「堕天」或「坠羽」的玩家用**近战直击**命中目标 → 损伤值 +`bar_max / 刀数`
 * （堕天 6 刀、坠羽 4 刀）；满 `bar_max` → **爆条**（元素伤害）→ 条置满并随**损伤状态**（默认 5 秒）
 * 线性回退到 0 —— 期间目标每次受伤额外附加元素伤害、治疗被削减；条归 0 后进入**损伤抵抗**（默认 5 秒）。
 * **损伤状态与损伤抵抗期间都不再累积损伤值**，所以一次爆条后 10 秒内同一目标不会再爆条。
 *
 * <p>**两件饰品的差别**（口径全部由「引爆者」的佩戴决定，并落到目标状态上）：戴「坠羽」时爆伤改为
 * 「本轮推条那几刀伤害之和 × 比例」、附加比例换成坠羽那一档，且爆条瞬间在引爆者背后展开翅膀
 * （见 {@link FallenPlumeWings}）；只戴「堕天」时是固定的 {@code burst_damage} 与基础附加比例；
 * 戴「笞心」时最终爆伤与附加比例再各 ×1.5，并给本段损伤状态开启**斩杀**（见 {@link #tryExecute}）。
 *
 * <p>**时间口径**：损伤状态 / 抵抗都存"到期时刻"（关卡 gameTime），所以读档、长时间离线、
 * 甚至状态中途退出游戏都不会留下卡死的残留（旧键不再读取 = 旧存档等价于已结束）。
 * 累积阶段若连续 {@code idle_clear_seconds}（默认 30）秒没被推过条，进度与累计伤害一起清零。
 *
 * <p>**元素伤害**：独立伤害类型，自带四个 {@code bypasses_*} 标签（无视护甲 / 保护附魔 / 抗性 / 效果），
 * 并且金额**钉住** —— 在 {@link LivingHurtEvent}（LOWEST）把金额改回我们发出的固定值，因此不吃
 * SR 的七罪七德与全伤害乘区（"伤害减免"类效果仍照常生效）。与 ER 的配合：该类型挂在
 * {@code #enchantment_reforged:no_dodge} 标签里，因此不会被「灵动步伐」闪避。
 *
 * <p>数据存目标实体的持久数据（SRA 根键），随存档保存；计时只遍历 {@link #ACTIVE} 登记表，
 * 不做全实体扫描。目标是玩家时，额外用原版 Boss 条在屏幕顶部显示（见 {@link #updateBossBar}）。
 */
public final class Apoptosis {
	private static final String ROOT = "summy_reliquary_apocrypha";
	private static final String VALUE = "apoptosis_value";
	/**
	 * 损伤状态的**到期时刻**（取目标所在关卡的时间）。
	 *
	 * <p>为什么存"到期时刻"而不是"剩余 tick"：倒计时只有被 {@link #ACTIVE} 遍历到才会递减，
	 * 而这张表只在爆条那一刻入表 —— 一旦在状态中途退出游戏，读档后那个目标会**永远**停在
	 * 「损伤状态」里（一直吃附加伤害、而且再也爆不了条）。换成绝对时间后，读档 / 长时间离线
	 * 都会自然过期，等价于自愈；旧键（`apoptosis_injured_ticks` / `apoptosis_resist_ticks`）不再读取。
	 */
	private static final String INJURED_UNTIL = "apoptosis_injured_until";
	/** 损伤抵抗的到期时刻（同一套时间口径） */
	private static final String RESIST_UNTIL = "apoptosis_resist_until";
	/** 本轮累计里「推条那几刀」的伤害之和：2 号件（坠羽）的爆伤口径要用它 */
	private static final String PUSH_DAMAGE = "apoptosis_push_damage";
	/** 本段损伤状态的附加伤害比例（%）：爆条那一刻由引爆者的佩戴决定并写下来 */
	private static final String EXTRA_PERCENT = "apoptosis_extra_percent";
	/** 最近一次被推条的时刻：用于「连续 N 秒没有被推过条就清空进度」 */
	private static final String LAST_PUSH = "apoptosis_last_push";
	/** 本段损伤状态是否已经斩杀过（每段状态只落地一次） */
	private static final String EXECUTE_USED = "apoptosis_execute_used";

	/** 本模组元素伤害类型的 id：钉住与「防递归」判定都用它 */
	private static final String ELEMENT_ID = "summy-reliquary-apocrypha.apoptosis_element";
	/** 同步节流：客户端会本地衰减，所以每 N tick 补发一次即可 */
	private static final int SYNC_INTERVAL_TICKS = 5;

	/** 元素伤害的钉住值：发伤前写入，LivingHurtEvent（LOWEST）里改回去 */
	private static float pendingElementDamage;
	/** 最近一次元素伤害实际落地金额（只服务调试日志） */
	private static float lastElementAmount;

	/** 活跃目标 → 所在关卡（只在这里推计时与同步） */
	private static final Map<UUID, ServerLevel> ACTIVE = new HashMap<>();
	/** 玩家受害者 → 屏幕顶部的 Boss 条 */
	private static final Map<UUID, ServerBossEvent> BOSS_BARS = new HashMap<>();

	private Apoptosis() {
	}

	// ==================== 数据读写 ====================

	private static CompoundTag data(LivingEntity target) {
		CompoundTag persistent = target.getPersistentData();
		if (!persistent.contains(ROOT, CompoundTag.TAG_COMPOUND)) {
			persistent.put(ROOT, new CompoundTag());
		}
		return persistent.getCompound(ROOT);
	}

	/** 世界时间（目标所在关卡的 gameTime：随存档持久，各维度同 tick 推进） */
	private static long now(LivingEntity target) {
		return target.level().getGameTime();
	}

	/** 损伤状态的总时长（tick） */
	private static int injuredTotalTicks() {
		return Math.max(1, ApocryphaConfig.apoptosisInjuredSeconds() * 20);
	}

	/**
	 * 当前凋亡损伤值（0 ~ bar_max）。
	 *
	 * <p>损伤状态期间按**剩余时间派生**（条从满线性回退到 0），其余时候读累计值。
	 */
	public static double value(LivingEntity target) {
		CompoundTag data = data(target);
		long now = now(target);
		long injuredUntil = data.getLong(INJURED_UNTIL);
		if (injuredUntil <= now) {
			return Math.max(0.0D, data.getDouble(VALUE));
		}
		double barMax = Math.max(1.0D, ApocryphaConfig.apoptosisBarMax());
		double fraction = (injuredUntil - now) / (double) injuredTotalTicks();
		return barMax * Math.max(0.0D, Math.min(1.0D, fraction));
	}

	/** 损伤状态剩余 tick（&gt; 0 表示处于损伤状态） */
	public static int injuredTicks(LivingEntity target) {
		return (int) Math.max(0L, data(target).getLong(INJURED_UNTIL) - now(target));
	}

	/** 损伤抵抗剩余 tick（&gt; 0 表示处于抵抗状态） */
	public static int resistTicks(LivingEntity target) {
		return (int) Math.max(0L, data(target).getLong(RESIST_UNTIL) - now(target));
	}

	/** 是否处于损伤状态 */
	public static boolean isInjured(LivingEntity target) {
		return data(target).getLong(INJURED_UNTIL) > now(target);
	}

	// ==================== 事件 ====================

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (!(event.getEntity().level() instanceof ServerLevel level)) {
			return;
		}
		LivingEntity target = event.getEntity();

		// ① 本模组的元素伤害：把金额钉回固定值（不吃 SR 的七罪七德与全伤害乘区）
		if (ELEMENT_ID.equals(event.getSource().getMsgId())) {
			if (!event.isCanceled()) {
				event.setAmount(pendingElementDamage);
			}
			lastElementAmount = event.isCanceled() ? 0.0F : event.getAmount();
			return;
		}

		if (event.isCanceled() || !ApocryphaConfig.enable()) {
			return;
		}

		// 取「触发那一刻」的损伤状态快照：这一刀若正好推满触发爆条，它本身不该再吃附加伤害
		// （损伤状态是爆条之后才开始的，口径与天使线「在下落状态的那一刻判定」一致）
		boolean wasInjured = isInjured(target);
		// 读档 / 长时间离线后残留的状态或进度：只要目标被碰过，就重新纳入计时表（自愈 + 让头顶条重新同步）
		resumeIfCarrying(level, target);

		// ② 佩戴「堕天」或「坠羽」的玩家近战直击 → 推条
		//（弓箭 / 投掷物 / 落箭 / 宠物：直接来源都不是玩家本人，天然被排除）
		if (event.getSource().getDirectEntity() instanceof ServerPlayer attacker && attacker != target
				&& wearsElementCurio(attacker)) {
			push(level, attacker, target, event.getAmount());
		}

		// ②b 佩戴「枯笔」的玩家近战直击「损伤抵抗中」的目标 → 抵抗时间削减（4 号件）
		if (event.getSource().getDirectEntity() instanceof ServerPlayer cutter && cutter != target
				&& CurioHelper.wears(cutter, Apocrypha.WITHERED_BRUSH.get())) {
			cutResistance(cutter, target);
		}

		// ③ 损伤状态：这一次受伤额外附加元素伤害（比例由引爆那一刻定死在目标数据里）
		if (wasInjured) {
			float extra = event.getAmount() * (float) (extraPercent(target) / 100.0D);
			Entity causing = event.getSource().getEntity();
			dealElementDamage(level, causing instanceof ServerPlayer player ? player : null, target, extra, "附加");
			// ④ 斩杀：本次伤害结算完附加伤害之后再判 —— 只有「佩戴笞心的伤害造成者」才会激活这个判断
			tryExecute(level, target, event);
		}
	}

	/** 目标身上还留着未过期的状态或未清空的进度时，把它重新纳入计时表（读档自愈） */
	private static void resumeIfCarrying(ServerLevel level, LivingEntity target) {
		CompoundTag data = data(target);
		long now = now(target);
		if (data.getLong(INJURED_UNTIL) > now || data.getLong(RESIST_UNTIL) > now
				|| data.getDouble(VALUE) > 0.0D) {
			markActive(level, target);
		}
	}

	/**
	 * 抵抗削减（4 号件「枯笔」）：近战直击「损伤抵抗中」的目标，把它的抵抗到期时刻往前挪若干秒。
	 *
	 * <p>每刀最多减一次；减到 ≤ 现在即抵抗立刻结束（目标马上又能被推条）；不在抵抗期就什么也不做。
	 */
	private static void cutResistance(ServerPlayer cutter, LivingEntity target) {
		int seconds = Math.max(0, ApocryphaConfig.apoptosisResistanceCutSecondsWithFourth());
		if (seconds <= 0) {
			return;
		}
		CompoundTag data = data(target);
		long now = now(target);
		long before = data.getLong(RESIST_UNTIL);
		if (before <= now) {
			return; // 不在抵抗期
		}
		long after = Math.max(now, before - seconds * 20L);
		data.putLong(RESIST_UNTIL, after);
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info(String.format("[凋亡] 抵抗削减：%s ← %s（抵抗 %.1f 秒 → %.1f 秒）",
					target.getName().getString(), cutter.getName().getString(),
					(before - now) / 20.0D, (after - now) / 20.0D));
		}
	}

	/**
	 * 斩杀（3 号件「笞心」）：**目标受伤后**若生命值 ≤ 最大生命 × 阈值%，补上一击「最大生命 × 比例%」的元素伤害。
	 *
	 * <p>激活条件：本次伤害的**造成者**是佩戴「笞心」的玩家（近战 / 弓箭 / 落箭都算）——
	 * 没有这个激活者就完全不判断。每段损伤状态只落地一次；这一刀本身就打死目标时不补
	 * （避免抢走原本的死亡信息）。
	 */
	private static void tryExecute(ServerLevel level, LivingEntity target, LivingHurtEvent event) {
		CompoundTag data = data(target);
		Entity causing = event.getSource().getEntity();
		if (!(causing instanceof ServerPlayer player)
				|| !CurioHelper.wears(player, Apocrypha.SCOURGED_HEART.get())) {
			return; // 没有被佩戴笞心的人激活 —— 连判断都不做（也不记日志）
		}
		float maxHealth = target.getMaxHealth();
		// 「受伤后」的血量：我们的附加伤害是嵌套即时落地的，所以读数已含那一笔；这里再扣掉本次尚未落地的金额
		float remaining = target.getHealth() - event.getAmount();
		double threshold = maxHealth * (ApocryphaConfig.apoptosisExecuteThresholdPercent() / 100.0D);
		boolean used = data.getBoolean(EXECUTE_USED);
		String reason = used ? "本段已斩"
				: remaining <= 0.0F ? "这一刀已致死"
				: remaining > threshold ? "血量未达阈值" : "";
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info(String.format(
					"[凋亡] 斩杀判定：%s ← %s 预计血量 %.1f/%.1f（阈值 %.0f%%，本段已斩=%s）→ %s",
					target.getName().getString(), player.getName().getString(), Math.max(0.0F, remaining), maxHealth,
					ApocryphaConfig.apoptosisExecuteThresholdPercent(), used ? "是" : "否",
					reason.isEmpty() ? "触发" : "未触发（" + reason + "）"));
		}
		if (!reason.isEmpty()) {
			return;
		}
		float executeDamage = (float) (maxHealth
				* (ApocryphaConfig.apoptosisExecuteDamagePercentOfMax() / 100.0D));
		if (executeDamage <= 0.0F) {
			return;
		}
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info(String.format("[凋亡] 斩杀：%s ← %s（最大生命 %.2f × %.0f%% = %.2f 点元素伤害）",
					target.getName().getString(), player.getName().getString(), maxHealth,
					ApocryphaConfig.apoptosisExecuteDamagePercentOfMax(), executeDamage));
		}
		if (dealElementDamage(level, player, target, executeDamage, "斩杀")) {
			data.putBoolean(EXECUTE_USED, true);
		}
	}

	/** 是否佩戴恶魔线「凋亡」的两件之一（堕天 / 坠羽）—— 两件都能推条 */
	private static boolean wearsElementCurio(ServerPlayer player) {
		return CurioHelper.wears(player, Apocrypha.FALLEN_HEAVEN.get())
				|| CurioHelper.wears(player, Apocrypha.FALLEN_PLUME.get());
	}

	/** 本段损伤状态的附加伤害比例（%）：爆条时写死，读不到就退回配置默认值 */
	private static double extraPercent(LivingEntity target) {
		CompoundTag data = data(target);
		return data.contains(EXTRA_PERCENT) ? data.getDouble(EXTRA_PERCENT)
				: ApocryphaConfig.apoptosisExtraDamagePercent();
	}

	@SubscribeEvent
	public static void onLivingHeal(LivingHealEvent event) {
		if (!(event.getEntity().level() instanceof ServerLevel) || !ApocryphaConfig.enable()) {
			return;
		}
		if (!isInjured(event.getEntity())) {
			return;
		}
		double factor = 1.0D - ApocryphaConfig.apoptosisHealReductionPercent() / 100.0D;
		float clamped = (float) Math.max(0.0D, Math.min(1.0D, factor));
		event.setAmount(event.getAmount() * clamped);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		tickServer(event.getServer());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		clear();
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			removeBar(player.getUUID());
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			// 换维度时先收掉，下一次 tick 若状态仍在会自动重建
			removeBar(player.getUUID());
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			removeBar(player.getUUID());
		}
	}

	// ==================== 推条 / 爆条 ====================

	private static void push(ServerLevel level, ServerPlayer attacker, LivingEntity target, float hitDamage) {
		CompoundTag data = data(target);
		long now = now(target);
		// 损伤状态与损伤抵抗一样：期间既不累计、也不能再次爆条（一次爆条后 10 秒内同一目标不会再爆）
		if (data.getLong(INJURED_UNTIL) > now || data.getLong(RESIST_UNTIL) > now) {
			return;
		}
		double barMax = Math.max(1.0D, ApocryphaConfig.apoptosisBarMax());
		double gain = barMax / requiredHits(attacker);
		double next = data.getDouble(VALUE) + gain;
		// 先把这一刀的金额记进本轮累计：2 号件（坠羽）的爆伤就是这几刀之和 × 比例
		double pushDamage = data.getDouble(PUSH_DAMAGE) + Math.max(0.0F, hitDamage);
		data.putDouble(PUSH_DAMAGE, pushDamage);
		// 空闲清空的计时起点：每次成功推条都刷新
		data.putLong(LAST_PUSH, now);
		if (next >= barMax - 1.0E-6D) {
			burst(level, attacker, target, barMax, pushDamage);
			return;
		}
		data.putDouble(VALUE, next);
		markActive(level, target);
		sync(target);
	}

	/** 打满损伤条需要的近战次数：佩戴「坠羽」时为 hits_with_second（4），否则为 hits_base（6） */
	private static int requiredHits(ServerPlayer attacker) {
		return CurioHelper.wears(attacker, Apocrypha.FALLEN_PLUME.get())
				? Math.max(1, ApocryphaConfig.apoptosisHitsWithSecond())
				: Math.max(1, ApocryphaConfig.apoptosisHitsBase());
	}

	/**
	 * 爆条：口径（刀数 / 爆伤 / 附加比例）全部由**引爆者**的佩戴决定，并把附加比例落到目标状态上。
	 *
	 * <p>戴「坠羽」→ 爆伤 = 本轮推条伤害之和 × 比例（取代固定的 burst_damage）、附加比例取坠羽那一档；
	 * 只戴「堕天」→ 固定 burst_damage 与基础附加比例；戴「笞心」→ 最终爆伤与附加比例再各 ×1.5。
	 */
	private static void burst(ServerLevel level, ServerPlayer attacker, LivingEntity target, double barMax,
			double pushDamage) {
		CompoundTag data = data(target);
		long now = now(target);
		boolean plume = CurioHelper.wears(attacker, Apocrypha.FALLEN_PLUME.get());
		boolean third = CurioHelper.wears(attacker, Apocrypha.SCOURGED_HEART.get());
		double burstDamage = plume
				? pushDamage * (ApocryphaConfig.apoptosisBurstRatioWithSecondPercent() / 100.0D)
				: ApocryphaConfig.apoptosisBurstDamage();
		double extraPercent = plume
				? ApocryphaConfig.apoptosisExtraDamagePercentWithSecond()
				: ApocryphaConfig.apoptosisExtraDamagePercent();
		if (third) {
			// 3 号件「笞心」：最终爆伤与附加比例各 ×1.5（固定 30 → 45；坠羽 60% → 90%）
			burstDamage *= ApocryphaConfig.apoptosisBurstMultiplierWithThird();
			extraPercent *= ApocryphaConfig.apoptosisExtraMultiplierWithThird();
		}
		// 条置满，并在损伤状态期间线性回退到 0
		// 进入损伤状态：条值改为按剩余时间派生，所以累计值清零
		data.putDouble(VALUE, 0.0D);
		data.putLong(INJURED_UNTIL, now + injuredTotalTicks());
		data.putLong(RESIST_UNTIL, 0L);
		// 本段损伤状态的附加比例定下来；本轮推条累计清零（下一轮重新攒）；斩杀每段只算一次
		data.putDouble(EXTRA_PERCENT, extraPercent);
		data.putDouble(PUSH_DAMAGE, 0.0D);
		data.putLong(LAST_PUSH, now);
		data.putBoolean(EXECUTE_USED, false);
		markActive(level, target);
		sync(target);
		// 2 号件：爆条瞬间在引爆者背后展开翅膀（再次爆条刷新计时）
		if (plume) {
			FallenPlumeWings.flash(attacker);
		}
		// 4 号件：爆条瞬间让目标陷入恐惧（无法移动与攻击；玩家档默认关闭）
		if (CurioHelper.wears(attacker, Apocrypha.WITHERED_BRUSH.get())) {
			WitheredBrush.applyFear(level, attacker, target);
		}
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info(String.format("[凋亡] 爆条：%s ← %s（元素伤害 %.2f 点%s%s）",
					target.getName().getString(), attacker.getName().getString(),
					burstDamage,
					plume ? String.format("，口径：坠羽 %d 刀推条之和 %.2f × %.0f%%、附加 %.0f%%",
							requiredHits(attacker), pushDamage,
							ApocryphaConfig.apoptosisBurstRatioWithSecondPercent(), extraPercent) : "",
					third ? String.format("，笞心 ×%.1f", ApocryphaConfig.apoptosisBurstMultiplierWithThird()) : ""));
		}
		dealElementDamage(level, attacker, target, (float) burstDamage, "爆条");
	}

	// ==================== 元素伤害 ====================

	/**
	 * 打出一笔元素伤害：清无敌帧 → 结算 → 不留帧 → 还原速度（无击退）。
	 *
	 * <p>金额会在 {@link #onLivingHurt} 的 LOWEST 里被钉回 {@code amount}，所以外边套的增伤乘区一律无效。
	 *
	 * @return 这一笔是否真的落地（斩杀要靠它决定「本段状态是否已经斩过」）
	 */
	private static boolean dealElementDamage(ServerLevel level, ServerPlayer attacker, LivingEntity target, float amount,
			String kind) {
		if (amount <= 0.0F || !target.isAlive()) {
			return false;
		}
		pendingElementDamage = amount;
		Vec3 velocityBefore = target.getDeltaMovement();
		// 无视无敌帧：先清零
		target.invulnerableTime = 0;
		boolean landed = target.hurt(elementSource(level, attacker), amount);
		if (landed) {
			target.setDeltaMovement(velocityBefore);
			target.hurtMarked = true;
			target.invulnerableTime = 0;
		}
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info(String.format("[凋亡] 元素伤害（%s）：%s 结算 %.2f 点（发出 %.2f，钉住后 %.2f）",
					kind, target.getName().getString(), landed ? lastElementAmount : 0.0F, amount,
					landed ? lastElementAmount : 0.0F));
		}
		return landed;
	}

	/**
	 * 元素伤害源：**直接来源留空**（这样 SR 不会把它当成"玩家近战命中"去做圣光等掷点），
	 * 造成者仍是玩家；数据包缺失时退回原版魔法伤害，避免抛异常打断世界 tick。
	 */
	private static DamageSource elementSource(ServerLevel level, ServerPlayer attacker) {
		var registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		Holder<DamageType> holder = registry
				.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, Apocrypha.id("apoptosis_element")))
				.orElse(null);
		if (holder == null) {
			return level.damageSources().magic();
		}
		return new DamageSource(holder, null, attacker);
	}

	// ==================== 计时与同步 ====================

	private static void tickServer(MinecraftServer server) {
		if (ACTIVE.isEmpty()) {
			return;
		}
		long serverTick = server.getTickCount();
		Iterator<Map.Entry<UUID, ServerLevel>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ServerLevel> active = iterator.next();
			Entity entity = active.getValue().getEntity(active.getKey());
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				iterator.remove();
				removeBar(active.getKey());
				continue;
			}
			CompoundTag data = data(target);
			long now = now(target);
			boolean phaseChanged = false;
			long injuredUntil = data.getLong(INJURED_UNTIL);
			long resistUntil = data.getLong(RESIST_UNTIL);
			if (injuredUntil <= now && resistUntil <= now) {
				// 两种状态都不在了：把过期的状态收尾（损伤状态刚结束 → 进入损伤抵抗）
				if (injuredUntil > 0L) {
					data.putLong(INJURED_UNTIL, 0L);
					data.putDouble(VALUE, 0.0D);
					data.putDouble(PUSH_DAMAGE, 0.0D);
					data.putBoolean(EXECUTE_USED, false);
					data.putLong(RESIST_UNTIL,
							now + Math.max(0, ApocryphaConfig.apoptosisResistanceSeconds()) * 20L);
					phaseChanged = true;
				} else if (resistUntil > 0L) {
					data.putLong(RESIST_UNTIL, 0L);
					phaseChanged = true;
				} else {
					// 累积阶段：连续 N 秒没有被推过条 → 进度与已累计的推条伤害一起清零
					int idleSeconds = Math.max(0, ApocryphaConfig.apoptosisIdleClearSeconds());
					if (data.getDouble(VALUE) > 0.0D && idleSeconds > 0
							&& now - data.getLong(LAST_PUSH) >= idleSeconds * 20L) {
						data.putDouble(VALUE, 0.0D);
						data.putDouble(PUSH_DAMAGE, 0.0D);
						data.putLong(LAST_PUSH, 0L);
						phaseChanged = true;
						if (ApocryphaConfig.logTrigger()) {
							// 注意：idleSeconds 是 int，这里只能用 %d（用 %.0f 会抛 IllegalFormatConversionException 并崩服）
							Apocrypha.LOGGER.info(String.format("[凋亡] 进度清空（空闲 %d 秒）：%s",
									idleSeconds, target.getName().getString()));
						}
					}
				}
			}
			if (data.getLong(INJURED_UNTIL) <= now && data.getLong(RESIST_UNTIL) <= now
					&& data.getDouble(VALUE) <= 0.0D) {
				// 彻底结束：移出登记表并让客户端把条清掉
				iterator.remove();
				removeBar(active.getKey());
				sync(target);
				continue;
			}
			updateBossBar(target);
			if (phaseChanged || serverTick % SYNC_INTERVAL_TICKS == 0) {
				sync(target);
			}
		}
	}

	private static void markActive(ServerLevel level, LivingEntity target) {
		ACTIVE.put(target.getUUID(), level);
	}

	private static void sync(LivingEntity target) {
		ApocryphaNetworking.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> target),
				new ApoptosisBarMessage(target.getId(), (float) value(target), injuredTicks(target),
						resistTicks(target)));
	}

	// ==================== 玩家侧 Boss 条 ====================

	/** 目标是玩家时，用原版 Boss 条在其屏幕顶部显示同一份状态（零自定义渲染） */
	private static void updateBossBar(LivingEntity target) {
		if (!(target instanceof ServerPlayer victim)) {
			return;
		}
		int injured = injuredTicks(target);
		int resist = resistTicks(target);
		double value = value(target);
		if (injured <= 0 && resist <= 0 && value <= 0.0D) {
			removeBar(victim.getUUID());
			return;
		}
		ServerBossEvent bar = BOSS_BARS.computeIfAbsent(victim.getUUID(), id -> {
			ServerBossEvent created = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE,
					BossEvent.BossBarOverlay.NOTCHED_6);
			created.addPlayer(victim);
			return created;
		});
		double barMax = Math.max(1.0D, ApocryphaConfig.apoptosisBarMax());
		if (injured > 0) {
			bar.setColor(BossEvent.BossBarColor.RED);
			bar.setProgress(clamp01(value / barMax));
			bar.setName(Component.translatable("message.summy-reliquary-apocrypha.apoptosis.injured",
					String.format("%.1f", injured / 20.0D)));
		} else if (resist > 0) {
			int total = Math.max(1, ApocryphaConfig.apoptosisResistanceSeconds() * 20);
			bar.setColor(BossEvent.BossBarColor.WHITE);
			bar.setProgress(clamp01(resist / (double) total));
			bar.setName(Component.translatable("message.summy-reliquary-apocrypha.apoptosis.resist",
					String.format("%.1f", resist / 20.0D)));
		} else {
			bar.setColor(BossEvent.BossBarColor.PURPLE);
			bar.setProgress(clamp01(value / barMax));
			bar.setName(Component.translatable("message.summy-reliquary-apocrypha.apoptosis.stack",
					(int) Math.round(value / barMax * 100.0D)));
		}
	}

	private static float clamp01(double value) {
		return (float) Math.max(0.0D, Math.min(1.0D, value));
	}

	private static void removeBar(UUID victim) {
		ServerBossEvent bar = BOSS_BARS.remove(victim);
		if (bar != null) {
			bar.removeAllPlayers();
		}
	}

	/** 服务器停止：清空登记与所有 Boss 条 */
	public static void clear() {
		ACTIVE.clear();
		for (ServerBossEvent bar : BOSS_BARS.values()) {
			bar.removeAllPlayers();
		}
		BOSS_BARS.clear();
	}
}
