package com.summy.reliquary.apocrypha.loot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 两套 10 件独占饰品的掉落实体保护。
 *
 * <p>任何来源生成的这些饰品的 {@link ItemEntity} 都不可被破坏，也不会自然消失；
 * 玩家死亡时则从普通掉落列表移除，实际恢复由 {@link ApocryphaAcquisition} 在重生后完成。
 */
public final class ApocryphaItemProtection {
	private ApocryphaItemProtection() {
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item)) {
			return;
		}
		if (ApocryphaAcquisition.isManaged(item.getItem())) {
			protect(item);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDrops(LivingDropsEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer)) {
			return;
		}
		event.getDrops().removeIf(drop -> ApocryphaAcquisition.isManaged(drop.getItem()));
	}

	/** 给掉落实体加不可摧毁与无限寿命；传 null 时保持 null，方便直接接到 player.drop(...) */
	public static ItemEntity protect(ItemEntity entity) {
		if (entity != null) {
			entity.setInvulnerable(true);
			entity.setUnlimitedLifetime();
		}
		return entity;
	}
}
