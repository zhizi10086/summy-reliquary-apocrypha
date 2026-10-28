package com.summy.reliquary.apocrypha.loot;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.effect.PlayerFlags;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 新约 / 凋亡两套饰品的个人战利品获取状态机。
 *
 * <p>只处理由玩家首次生成的 {@code chests/} 战利品表：没有标记不发奖，双标记暂停，
 * 基础件首次必得，基础件佩戴后才有 15% + 十箱保底的四件抽取。奖励直接进入背包，
 * 背包满则掉在玩家脚下并交给 {@link ApocryphaItemProtection} 保护。
 */
public final class ApocryphaAcquisition {
	private static final String ROOT = "summy_reliquary_apocrypha";
	private static final String STATE = "acquisition";
	private static final String ANGEL_OWNED = "angel_owned";
	private static final String DEMON_OWNED = "demon_owned";
	private static final String ANGEL_PITY = "angel_pity";
	private static final String DEMON_PITY = "demon_pity";
	private static final String PENDING_RESTORE = "pending_restore";

	private static final List<Entry> ANGEL_ITEMS = List.of(
			new Entry("new_testament", Apocrypha.NEW_TESTAMENT),
			new Entry("swift_feather_covenant", Apocrypha.SWIFT_FEATHER_COVENANT),
			new Entry("wing_feather_sacrament", Apocrypha.WING_FEATHER_SACRAMENT),
			new Entry("inverse_feather_hover", Apocrypha.INVERSE_FEATHER_HOVER),
			new Entry("paired_wings", Apocrypha.PAIRED_WINGS)
	);
	private static final List<Entry> DEMON_ITEMS = List.of(
			new Entry("fallen_heaven", Apocrypha.FALLEN_HEAVEN),
			new Entry("fallen_plume", Apocrypha.FALLEN_PLUME),
			new Entry("scourged_heart", Apocrypha.SCOURGED_HEART),
			new Entry("withered_brush", Apocrypha.WITHERED_BRUSH),
			new Entry("final_song", Apocrypha.FINAL_SONG)
	);
	private static final List<Entry> ALL_ITEMS = List.of(
			ANGEL_ITEMS.get(0), ANGEL_ITEMS.get(1), ANGEL_ITEMS.get(2), ANGEL_ITEMS.get(3), ANGEL_ITEMS.get(4),
			DEMON_ITEMS.get(0), DEMON_ITEMS.get(1), DEMON_ITEMS.get(2), DEMON_ITEMS.get(3), DEMON_ITEMS.get(4)
	);

	/** 已写入待恢复 NBT、但当前连接仍需在下一 tick 核对数量的玩家 */
	private static final Set<UUID> PENDING_RESTORES = new HashSet<>();
	/** 玩家本 tick 实际交互过的未开箱容器；防止 `/loot` 和模组直接调用伪造开箱 */
	private static final Map<UUID, ContainerWindow> CONTAINER_WINDOWS = new HashMap<>();

	private record ContainerWindow(ResourceKey<Level> dimension, BlockPos blockPos, net.minecraft.world.phys.Vec3 origin,
			ResourceLocation table, long tick) {
		private boolean matches(ServerLevel level, ResourceLocation actualTable,
				net.minecraft.world.phys.Vec3 actualOrigin) {
			if (!dimension.equals(level.dimension()) || !table.equals(actualTable) || tick != level.getServer().getTickCount()) {
				return false;
			}
			if (blockPos != null) {
				return BlockPos.containing(actualOrigin).equals(blockPos);
			}
			return origin.distanceToSqr(actualOrigin) < 0.01D;
		}
	}

	private ApocryphaAcquisition() {
	}

	/** Global Loot Modifier 的唯一业务入口 */
	public static void tryGrant(LootContext context) {
		if (!ApocryphaConfig.enable()) {
			return;
		}
		ServerLevel level = context.getLevel();
		ResourceLocation table = context.getQueriedLootTableId();
		if (table == null || !table.getPath().startsWith("chests/")) {
			return;
		}
		if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)
				|| player.isSpectator()) {
			return;
		}
		net.minecraft.world.phys.Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
		ContainerWindow window = CONTAINER_WINDOWS.get(player.getUUID());
		if (origin == null || window == null || !window.matches(level, table, origin)) {
			return;
		}
		// 一次实际开箱最多消费一个来源窗口，后续同名战利品表不会再被当作开箱。
		CONTAINER_WINDOWS.remove(player.getUUID());

		boolean angel = PlayerFlags.hasAngel(player);
		boolean demon = PlayerFlags.isDemon(player);
		// 双标记或完全无标记都不结算；这也避免异常状态被误判成任一战线。
		if (angel == demon) {
			return;
		}

		List<Entry> route = angel ? ANGEL_ITEMS : DEMON_ITEMS;
		CompoundTag state = state(player);
		reconcileOwned(player, state, ANGEL_ITEMS, ANGEL_OWNED);
		reconcileOwned(player, state, DEMON_ITEMS, DEMON_OWNED);

		String ownedKey = angel ? ANGEL_OWNED : DEMON_OWNED;
		String pityKey = angel ? ANGEL_PITY : DEMON_PITY;
		int owned = state.getInt(ownedKey);

		// 1 号件是路线基础件：未获取时，下一个合格野箱必定发放。
		if ((owned & 1) == 0) {
			grant(player, route.get(0).item().get());
			state.putInt(ownedKey, owned | 1);
			return;
		}

		// 未佩戴基础件只锁住后续四件，不影响已发放的基础件状态。
		if (!CurioHelper.wears(player, route.get(0).item().get())) {
			return;
		}

		List<Integer> missing = new ArrayList<>(4);
		for (int index = 1; index < route.size(); index++) {
			if ((owned & (1 << index)) == 0) {
				missing.add(index);
			}
		}
		if (missing.isEmpty()) {
			return;
		}

		int pity = state.getInt(pityKey);
		int pityLimit = ApocryphaConfig.acquisitionPityChests();
		boolean guaranteed = pityLimit > 0 && pity >= pityLimit;
		boolean success = guaranteed || context.getRandom().nextInt(100) < ApocryphaConfig.acquisitionChancePercent();
		if (!success) {
			state.putInt(pityKey, pity == Integer.MAX_VALUE ? pity : pity + 1);
			return;
		}

		int selected = missing.get(context.getRandom().nextInt(missing.size()));
		grant(player, route.get(selected).item().get());
		state.putInt(ownedKey, owned | (1 << selected));
		state.putInt(pityKey, 0);
	}

	@SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)
				|| !(event.getLevel() instanceof ServerLevel level) || player.isSpectator()) {
			return;
		}
		ResourceLocation table = blockContainerLootTable(level, event.getPos());
		if (table != null) {
			rememberContainer(player, level, event.getPos(), net.minecraft.world.phys.Vec3.atCenterOf(event.getPos()), table);
		}
	}

	@SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)
				|| !(event.getLevel() instanceof ServerLevel level) || player.isSpectator()
				|| !(event.getTarget() instanceof AbstractMinecartContainer cart) || cart.getLootTable() == null) {
			return;
		}
		rememberContainer(player, level, null, cart.position(), cart.getLootTable());
	}

	/** 读取方块容器尚未生成的 LootTable；双箱会检查连接的另一半 */
	private static ResourceLocation blockContainerLootTable(ServerLevel level, BlockPos pos) {
		ResourceLocation table = readContainerLootTable(level.getBlockEntity(pos));
		if (table != null) {
			return table;
		}
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
			return null;
		}
		Direction connected = ChestBlock.getConnectedDirection(state);
		return readContainerLootTable(level.getBlockEntity(pos.relative(connected)));
	}

	private static ResourceLocation readContainerLootTable(BlockEntity blockEntity) {
		if (!(blockEntity instanceof RandomizableContainerBlockEntity container)) {
			return null;
		}
		CompoundTag saved = container.saveWithoutMetadata();
		if (!saved.contains(RandomizableContainerBlockEntity.LOOT_TABLE_TAG, CompoundTag.TAG_STRING)) {
			return null;
		}
		return ResourceLocation.tryParse(saved.getString(RandomizableContainerBlockEntity.LOOT_TABLE_TAG));
	}

	private static void rememberContainer(ServerPlayer player, ServerLevel level, BlockPos blockPos,
			net.minecraft.world.phys.Vec3 origin, ResourceLocation table) {
		if (!table.getPath().startsWith("chests/")) {
			return;
		}
		CONTAINER_WINDOWS.put(player.getUUID(), new ContainerWindow(level.dimension(), blockPos, origin, table,
				level.getServer().getTickCount()));
	}

	/** 判断物品是否属于本获取系统的 10 件独占饰品 */
	public static boolean isManaged(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		for (Entry entry : ALL_ITEMS) {
			if (stack.is(entry.item().get())) {
				return true;
			}
		}
		return false;
	}

	/** 自检用：读取某一路线的获取位图 */
	public static int ownedMaskForTest(ServerPlayer player, boolean angel) {
		if (!Boolean.getBoolean("summy_apocrypha.devcheck")) {
			return 0;
		}
		return state(player).getInt(angel ? ANGEL_OWNED : DEMON_OWNED);
	}

	/** 自检用：读取某一路线的连续失败保底计数 */
	public static int pityForTest(ServerPlayer player, boolean angel) {
		if (!Boolean.getBoolean("summy_apocrypha.devcheck")) {
			return 0;
		}
		return state(player).getInt(angel ? ANGEL_PITY : DEMON_PITY);
	}

	/** 自检用：清掉获取位图与保底，不触碰死亡恢复和其他根数据 */
	public static void clearAcquisitionForTest(ServerPlayer player) {
		if (!Boolean.getBoolean("summy_apocrypha.devcheck")) {
			return;
		}
		CompoundTag state = state(player);
		state.remove(ANGEL_OWNED);
		state.remove(DEMON_OWNED);
		state.remove(ANGEL_PITY);
		state.remove(DEMON_PITY);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		CompoundTag state = state(player);
		reconcileOwned(player, state, ANGEL_ITEMS, ANGEL_OWNED);
		reconcileOwned(player, state, DEMON_ITEMS, DEMON_OWNED);
		if (state.contains(PENDING_RESTORE, CompoundTag.TAG_COMPOUND)) {
			PENDING_RESTORES.add(player.getUUID());
		}
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		CompoundTag oldPersistent = event.getOriginal().getPersistentData();
		if (oldPersistent.contains(ROOT, CompoundTag.TAG_COMPOUND)) {
			event.getEntity().getPersistentData().put(ROOT,
					oldPersistent.getCompound(ROOT).copy());
		}
		if (!event.isWasDeath()) {
			return;
		}

		PENDING_RESTORES.remove(player.getUUID());
		state(player).remove(PENDING_RESTORE);
		CompoundTag savedCounts = new CompoundTag();
		for (Entry entry : ALL_ITEMS) {
			int count = countEverywhere(event.getOriginal(), entry.item().get());
			if (count > 0) {
				savedCounts.putInt(entry.key(), count);
			}
		}
		if (!savedCounts.isEmpty()) {
			state(player).put(PENDING_RESTORE, savedCounts);
			PENDING_RESTORES.add(player.getUUID());
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			CONTAINER_WINDOWS.clear();
			return;
		}
		if (event.phase != TickEvent.Phase.END || PENDING_RESTORES.isEmpty()) {
			return;
		}
		MinecraftServer server = event.getServer();
		for (UUID id : new ArrayList<>(PENDING_RESTORES)) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null && player.isAlive() && !player.isRemoved()) {
				restorePending(player);
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PENDING_RESTORES.clear();
		CONTAINER_WINDOWS.clear();
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			CONTAINER_WINDOWS.remove(player.getUUID());
		}
	}

	private static void restorePending(ServerPlayer player) {
		CompoundTag state = state(player);
		if (!state.contains(PENDING_RESTORE, CompoundTag.TAG_COMPOUND)) {
			PENDING_RESTORES.remove(player.getUUID());
			return;
		}
		CompoundTag savedCounts = state.getCompound(PENDING_RESTORE);
		for (Entry entry : ALL_ITEMS) {
			int expected = savedCounts.getInt(entry.key());
			if (expected <= 0) {
				continue;
			}
			int current = countEverywhere(player, entry.item().get());
			for (int index = current; index < expected; index++) {
				grant(player, entry.item().get());
			}
		}
		state.remove(PENDING_RESTORE);
		PENDING_RESTORES.remove(player.getUUID());
	}

	private static void reconcileOwned(ServerPlayer player, CompoundTag state, List<Entry> route, String key) {
		int mask = state.getInt(key);
		for (int index = 0; index < route.size(); index++) {
			if (countEverywhere(player, route.get(index).item().get()) > 0) {
				mask |= 1 << index;
			}
		}
		state.putInt(key, mask);
	}

	private static int countEverywhere(Player player, Item item) {
		int count = 0;
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		if (CurioHelper.wears(player, item)) {
			count++;
		}
		return count;
	}

	private static void grant(ServerPlayer player, Item item) {
		ItemStack stack = new ItemStack(item);
		if (!player.getInventory().add(stack)) {
			ApocryphaItemProtection.protect(player.drop(stack, false));
		}
	}

	private static CompoundTag state(ServerPlayer player) {
		CompoundTag persistent = player.getPersistentData();
		CompoundTag root;
		if (persistent.contains(ROOT, CompoundTag.TAG_COMPOUND)) {
			root = persistent.getCompound(ROOT);
		} else {
			root = new CompoundTag();
			persistent.put(ROOT, root);
		}
		CompoundTag state;
		if (root.contains(STATE, CompoundTag.TAG_COMPOUND)) {
			state = root.getCompound(STATE);
		} else {
			state = new CompoundTag();
			root.put(STATE, state);
		}
		return state;
	}

	private record Entry(String key, Supplier<Item> item) {
	}
}
