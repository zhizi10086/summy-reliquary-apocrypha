package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.entity.NewTestamentArrow;
import com.summy.reliquary.entity.ThrownRazor;
import com.summy.reliquary.entity.ThrownSpear;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// 「新约」的命中判定、掷点、音效与落箭调度，外加给 SR 真伤补的「命中后清帧」。
//
// 触发条件：弓或弩射出的箭矢命中目标（AbstractArrow + owner 是佩戴「新约」的玩家、命中的不是自己）。
// 明确排除 SR 的幻影矛（ThrownSpear）与金刀片（ThrownRazor），以及本模组的落箭（避免重复触发与递归）。
//
// 用 LOWEST 优先级：此时其它模组（含 SR）的取消与数值改写都已完成，我们只读事实、不回写伤害。
public final class NewTestamentEffect {
	private NewTestamentEffect() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurt(LivingHurtEvent event) {
		// 只在服务端处理（LivingHurtEvent 双端都会触发）
		if (!(event.getEntity().level() instanceof ServerLevel level)) {
			return;
		}

		// ① 本模组落箭的命中：开关打开时记一行；落箭自身不再触发新约
		if (event.getSource().getDirectEntity() instanceof NewTestamentArrow) {
			logArrowHit(event);
			return;
		}

		// ② SR 真伤的「命中后清帧」补强（独立开关，不受 [general] enable 影响）
		if (!event.isCanceled() && ApocryphaConfig.clearSrIframesAfterHit()) {
			TrueDamageFrames.handle(event, level);
		}

		// ③ 新约本体：弓/弩箭矢命中目标
		if (event.isCanceled() || !ApocryphaConfig.enable()) {
			return;
		}
		if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
			return;
		}
		if (arrow instanceof ThrownSpear || arrow instanceof ThrownRazor) {
			return;
		}
		if (!(arrow.getOwner() instanceof ServerPlayer shooter)) {
			return;
		}
		LivingEntity target = event.getEntity();
		if (target == shooter) {
			return;
		}
		if (!CurioHelper.wears(shooter, Apocrypha.NEW_TESTAMENT.get())) {
			return;
		}

		// 掷点：每命中恰好消耗一次随机数（与是否触发、是否记日志都无关）
		double roll = level.random.nextDouble() * 100.0D;
		int chance = ApocryphaConfig.scatterChancePercent();
		boolean triggered = roll < chance;

		// 命中回血
		shooter.heal((float) ApocryphaConfig.healAmount());

		int count = 0;
		if (triggered) {
			count = rollArrowCount(level);
			// 听觉反馈：只发给触发者本人
			shooter.playNotifySound(SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
			ScatterVolley.enqueue(level, shooter, target, count);
		}
		logTrigger(shooter, target, roll, chance, triggered, count);
	}

	// 服务端 tick 末尾：放出待落的箭 + 处理推迟到此刻的清帧（Kilt 侧）
	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		ScatterVolley.tick(event.getServer());
		TrueDamageFrames.tickServer();
	}

	// 服务器停止：清空待落队列与清帧登记，避免残留到下一个存档
	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ScatterVolley.clear();
		TrueDamageFrames.clear();
	}

	// 本次触发的落箭支数
	private static int rollArrowCount(ServerLevel level) {
		int min = Math.min(ApocryphaConfig.scatterArrowMin(), ApocryphaConfig.scatterArrowMax());
		int max = Math.max(ApocryphaConfig.scatterArrowMin(), ApocryphaConfig.scatterArrowMax());
		return min + level.random.nextInt(max - min + 1);
	}

	// 触发行：每次本体命中一行（含未触发的那几次），用来看 35% 是否符合预期
	private static void logTrigger(ServerPlayer shooter, LivingEntity target, double roll, int chance,
			boolean triggered, int count) {
		if (!ApocryphaConfig.logTrigger()) {
			return;
		}
		String outcome = triggered
				? String.format("触发，落箭 %d 支（间隔 %d tick）", count,
						ApocryphaConfig.scatterSpawnIntervalTicks())
				: "未触发";
		Apocrypha.LOGGER.info(String.format("[新约] %s 箭矢命中 %s：掷点 %.1f（阈值 %d）→ %s；回血 %.2f",
				shooter.getName().getString(), target.getName().getString(), roll, chance, outcome,
				ApocryphaConfig.healAmount()));
	}

	// 命中行：每支落箭命中一行，金额取「各类倍率结算后」（含 SR 的七罪/七德与最终倍率乘区）
	private static void logArrowHit(LivingHurtEvent event) {
		if (!ApocryphaConfig.logTrigger() || event.isCanceled()) {
			return;
		}
		LivingEntity victim = event.getEntity();
		Apocrypha.LOGGER.info(String.format("[新约] 落箭命中 %s：结算 %.2f 点（各类倍率后）、命中前血量 %.1f/%.1f",
				victim.getName().getString(), event.getAmount(), victim.getHealth(), victim.getMaxHealth()));
	}
}
