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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 「新约」的命中判定与落箭散射（服务端）。
 *
 * <p>触发条件：弓或弩射出的箭矢命中目标（AbstractArrow + owner 是佩戴「新约」的玩家），
 * 并且命中的不是自己。明确排除 SR 的幻影矛（ThrownSpear）与金刀片（ThrownRazor）
 * 以及本模组的落箭，避免重复触发与自我递归。
 *
 * <p>效果：自身回复配置点数的生命；按配置概率掷点，命中则
 * (1) 给佩戴者本人播一次箭矢命中音效（只有本人听得见）、
 * (2) 在目标碰撞箱顶部上方生成若干支散射降落的箭矢（伤害与出伤口径见 NewTestamentArrow）。
 */
public final class NewTestamentEffect {
	private NewTestamentEffect() {
	}

	/**
	 * 用 LOWEST 优先级：此时其它模组（含 SR）的取消与数值改写都已经做完，
	 * 我们只读「这一击是否真的落地」这个事实，不回写伤害。
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.isCanceled()) {
			return;
		}
		if (!ApocryphaConfig.enable()) {
			return;
		}
		// 只在服务端处理（LivingHurtEvent 双端都会触发）
		if (!(event.getEntity().level() instanceof ServerLevel level)) {
			return;
		}
		// 只认「箭矢」这一类直接来源，并排除 SR 的投掷物与本模组的落箭
		if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
			return;
		}
		if (arrow instanceof ThrownSpear || arrow instanceof ThrownRazor
				|| arrow instanceof NewTestamentArrow) {
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

		// (1) 命中回血
		shooter.heal((float) ApocryphaConfig.healAmount());

		// (2) 掷点：未触发时不做任何额外表现
		if (level.random.nextDouble() * 100.0D >= ApocryphaConfig.scatterChancePercent()) {
			return;
		}

		// (3) 散射判定成功：先给本人一次听觉反馈（只发给触发者自己）
		shooter.playNotifySound(SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);

		// (4) 落箭
		scatter(level, shooter, target);
	}

	/**
	 * 在目标碰撞箱顶部上方生成 3~5 支散射降落的箭矢。
	 *
	 * <p>每支箭：以竖直向下为中心轴，随机方位角、随机偏角 0~配置角度，
	 * 给一个较小的初速度后交给原版箭矢重力自由下落。
	 *
	 * <p>注意 owner 必须设为玩家：既是「出伤归玩家」的前提，也让 SR 的圣心追踪能自动接管这些箭矢
	 * （SR 的筛选条件就是「AbstractArrow + owner 是该玩家」）。
	 */
	private static void scatter(ServerLevel level, ServerPlayer shooter, LivingEntity target) {
		int min = Math.min(ApocryphaConfig.scatterArrowMin(), ApocryphaConfig.scatterArrowMax());
		int max = Math.max(ApocryphaConfig.scatterArrowMin(), ApocryphaConfig.scatterArrowMax());
		int count = min + level.random.nextInt(max - min + 1);

		AABB box = target.getBoundingBox();
		Vec3 origin = new Vec3(target.getX(), box.maxY + ApocryphaConfig.scatterSpawnHeight(), target.getZ());
		double maxAngle = Math.toRadians(ApocryphaConfig.scatterAngleMaxDegrees());
		double speed = ApocryphaConfig.scatterInitialSpeed();

		for (int index = 0; index < count; index++) {
			double angle = maxAngle * level.random.nextDouble();
			double azimuth = level.random.nextDouble() * Math.PI * 2.0D;
			// 以竖直向下 (0, -1, 0) 为中心轴：按方位角偏出，再按偏角绕轴旋转
			Vec3 direction = new Vec3(
					Math.sin(angle) * Math.cos(azimuth),
					-Math.cos(angle),
					Math.sin(angle) * Math.sin(azimuth));

			NewTestamentArrow arrow = new NewTestamentArrow(level, shooter);
			arrow.setPos(origin.x, origin.y, origin.z);
			arrow.setDeltaMovement(direction.scale(speed));
			level.addFreshEntity(arrow);
		}
	}
}
