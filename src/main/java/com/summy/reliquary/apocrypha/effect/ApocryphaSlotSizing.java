package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.slot.ApocryphaSlots;
import com.summy.reliquary.effect.PlayerFlags;
import com.summy.reliquary.effect.SlotSizing;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// 本模组「新约系列」栏位的动态格数：仅天使线开放（有天使标记 1 格、否则 0 格）。
//
// 直接复用 SR 的公开方法 SlotSizing.sync（缩格前先把格内物品退回背包、满了掉脚下），
// 缩格行为因此与 SR 自家栏位完全一致；登录读档窗口（SR 数据未就绪）跳过，避免把刚读出来的饰品误判。
public final class ApocryphaSlotSizing {
	// 每秒校正一次（与 SR 的 SlotSizing 节奏一致）
	private static final int INTERVAL_TICKS = 20;

	private ApocryphaSlotSizing() {
	}

	public static void tickServer(MinecraftServer server) {
		if (server.getTickCount() % INTERVAL_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!PlayerFlags.isDataReady(player)) {
				continue;
			}
			int target = PlayerFlags.hasAngel(player) ? 1 : 0;
			for (String slotId : ApocryphaSlots.ANGEL_SERIES_SLOTS) {
				SlotSizing.sync(player, slotId, target);
			}
		}
	}
}
