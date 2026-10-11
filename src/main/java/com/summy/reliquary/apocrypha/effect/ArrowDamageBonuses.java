package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.entity.NewTestamentArrow;
import com.summy.reliquary.entity.ThrownRazor;
import com.summy.reliquary.entity.ThrownSpear;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 箭矢命中的伤害加成（**同一个 HIGHEST 处理器**，排在 SR 的增伤乘区 NORMAL 之前）。
 *
 * <p>把「圣餐之矢 +N%」与「逆羽·滞空（下落中）+M%」放在**同一乘区里相加**再一次性相乘
 * （同发即 ×(1 + N% + M%)），因此后续的 SR 增伤会基于放大后的值 —— 也就是"箭矢伤害本身 +X%"。
 *
 * <p>只作用于玩家射出的**普通箭矢**（弓 / 弩）；SR 的幻影矛 / 金刀片与本模组的落箭都不在此列
 * （落箭的伤害口径由 NewTestamentEffect 与钉住逻辑单独决定）。
 */
public final class ArrowDamageBonuses {
	private ArrowDamageBonuses() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide() || event.isCanceled()) {
			return;
		}
		if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
			return;
		}
		if (arrow instanceof ThrownSpear || arrow instanceof ThrownRazor || arrow instanceof NewTestamentArrow) {
			return;
		}
		if (!(arrow.getOwner() instanceof ServerPlayer shooter)) {
			return;
		}
		int percent = 0;
		if (SacramentArrows.isSacrament(event.getSource())) {
			percent += ApocryphaConfig.sacramentDamageBonusPercent();
		}
		// 逆羽·滞空：命中那一刻射手仍在下落
		if (HoverFlight.wears(shooter) && HoverFlight.isFalling(shooter)) {
			percent += ApocryphaConfig.hoverFallingDamageBonusPercent();
		}
		if (percent <= 0) {
			return;
		}
		event.setAmount(event.getAmount() * (1.0F + percent / 100.0F));
	}
}
