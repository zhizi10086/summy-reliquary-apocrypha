package com.summy.reliquary.apocrypha.effect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;

/**
 * 本模组与旧套系共用的投射物分类规则。
 *
 * <p>弓与弩真正射出的是 {@link Arrow} 或 {@link SpectralArrow}；不能使用
 * {@code AbstractArrow} 泛判，否则原版三叉戟、SR 幻影矛等都会被误认成普通箭矢。
 */
final class ProjectileRules {
	private ProjectileRules() {
	}

	/** 是否为本模组认可的普通弓箭投射物。 */
	static boolean isPlayerBowArrow(Entity entity) {
		return entity instanceof Arrow || entity instanceof SpectralArrow;
	}
}
