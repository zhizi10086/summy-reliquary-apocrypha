package com.summy.reliquary.apocrypha.item;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 本模组独占饰品的统一 Curio 行为。
 *
 * <p>这些饰品后续改为战利品获取，每件都应当死亡不掉落。集中覆写掉落规则，
 * 避免在 10 个物品类里重复实现同一段逻辑。
 */
public interface ApocryphaKeptCurioItem extends ICurioItem {
	@Override
	default ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source, int lootingLevel,
			boolean recentlyHit, ItemStack stack) {
		return ICurio.DropRule.ALWAYS_KEEP;
	}
}
