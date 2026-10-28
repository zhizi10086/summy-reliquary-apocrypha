package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.slot.ApocryphaSlots;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 「坠羽」爆条时背后展开的翅膀（服务端）。
 *
 * <p>做法与光羽（{@link FeatherCharge}）完全同款：**显隐状态写在饰品堆栈的 NBT 上**（键
 * {@link #FLAG}），客户端由 Curios 的渲染回调读取（见 {@code client/WingFeatherSacramentRenderer}），
 * 因此不需要新的实体，也不需要新的网络包 —— Curios 每 tick 会比对饰品堆栈并把 NBT 变化同步给客户端。
 *
 * <p>服务端只做两件事：爆条时写标志 + 记一个剩余 tick；之后每 tick 递减，到期清标志。
 * 玩家登出 / 服务器停止都会立刻清标志，避免「重登后翅膀常驻」。
 */
public final class FallenPlumeWings {
	/**
	 * 翅膀的显隐标志（写在「坠羽」的 ItemStack NBT 上）。
	 *
	 * <p>刻意与光羽的 {@code ApocryphaWings} 用不同的键：两件饰品可以同时佩戴，
	 * 分开写才不会互相覆盖标志。
	 */
	public static final String FLAG = "ApocryphaDemonWings";

	/** 玩家 → 翅膀剩余 tick；只有正在展开的玩家才有条目 */
	private static final Map<UUID, Integer> REMAINING = new HashMap<>();

	private FallenPlumeWings() {
	}

	/** 爆条时调用：给引爆者的坠羽写上标志，并把计时重置为完整时长（再次爆条即刷新） */
	public static void flash(ServerPlayer player) {
		if (!ApocryphaConfig.enable()) {
			return;
		}
		int ticks = Math.max(0, ApocryphaConfig.apoptosisWingSeconds() * 20);
		if (ticks <= 0) {
			return;
		}
		REMAINING.put(player.getUUID(), ticks);
		setFlag(player, true);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (!ApocryphaConfig.enable()) {
			clearAll(event.getServer());
			return;
		}
		if (REMAINING.isEmpty()) {
			return;
		}
		MinecraftServer server = event.getServer();
		Iterator<Map.Entry<UUID, Integer>> iterator = REMAINING.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Integer> entry = iterator.next();
			int left = entry.getValue() - 1;
			if (left > 0) {
				entry.setValue(left);
				continue;
			}
			iterator.remove();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) {
				setFlag(player, false);
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		clear(event.getEntity());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		clearAll(event.getServer());
	}

	/** 清掉全部在线玩家的临时恶魔翅膀。 */
	private static void clearAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			setFlag(player, false);
		}
		REMAINING.clear();
	}

	/** 清掉某个玩家身上的翅膀（登出 / 手动清理） */
	public static void clear(Player player) {
		REMAINING.remove(player.getUUID());
		if (player instanceof ServerPlayer serverPlayer) {
			setFlag(serverPlayer, false);
		}
	}

	/** 自检用：判断某玩家当前是否登记了临时恶魔翅膀。 */
	public static boolean isActiveForTest(Player player) {
		return Boolean.getBoolean("summy_apocrypha.devcheck") && REMAINING.containsKey(player.getUUID());
	}

	/**
	 * 把标志写到该玩家「殁之释经」栏位里的坠羽堆栈上（有变化才写）。
	 *
	 * <p>写法与光羽一致：取堆栈副本 → 改 NBT → {@code setStackInSlot} 写回，
	 * 用副本写回而不是原地改，Curios 才会检测到变化并在下一 tick 同步给客户端。
	 */
	private static void setFlag(ServerPlayer player, boolean on) {
		CuriosApi.getCuriosInventory(player).ifPresent(handler ->
				handler.getStacksHandler(ApocryphaSlots.PERDITION_EXEGESIS).ifPresent(stacksHandler -> {
					IDynamicStackHandler stacks = stacksHandler.getStacks();
					for (int index = 0; index < stacks.getSlots(); index++) {
						ItemStack stack = stacks.getStackInSlot(index);
						if (!stack.is(Apocrypha.FALLEN_PLUME.get())) {
							continue;
						}
						boolean current = stack.hasTag() && stack.getTag().getBoolean(FLAG);
						if (current == on) {
							continue;
						}
						ItemStack updated = stack.copy();
						if (on) {
							updated.getOrCreateTag().putBoolean(FLAG, true);
						} else if (updated.hasTag()) {
							updated.getTag().remove(FLAG);
						}
						stacks.setStackInSlot(index, updated);
					}
				}));
	}
}
