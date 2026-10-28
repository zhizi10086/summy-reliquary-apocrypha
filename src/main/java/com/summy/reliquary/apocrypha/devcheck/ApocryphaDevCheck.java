package com.summy.reliquary.apocrypha.devcheck;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.client.ApocryphaClientInput;
import com.summy.reliquary.apocrypha.client.ApoptosisBarState;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.effect.Apoptosis;
import com.summy.reliquary.apocrypha.effect.FallenPlumeWings;
import com.summy.reliquary.apocrypha.effect.NewTestamentEffect;
import com.summy.reliquary.apocrypha.effect.WitheredBrush;
import com.summy.reliquary.apocrypha.loot.ApocryphaAcquisition;
import com.summy.reliquary.apocrypha.slot.ApocryphaSlots;
import com.summy.reliquary.effect.PlayerFlags;
import com.summy.reliquary.effect.SlotSizing;
import com.summy.reliquary.util.CurioHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;

/**
 * SRA 真实客户端开发自检。
 *
 * <p>只在 {@code gradlew runClient -Pdevcheck} 时启用。脚本使用当前世界/玩家，
 * 会清理玩家物品并修改栏位状态，因此必须只在专用测试世界中运行。
 */
@Mod.EventBusSubscriber(modid = Apocrypha.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ApocryphaDevCheck {
	private static final boolean ENABLED = Boolean.getBoolean("summy_apocrypha.devcheck");
	private static final int CLIENT_IDLE_STOP_TICKS = 1200;
	private static final int CLIENT_TOTAL_STOP_TICKS = 6000;
	private static final String TARGET_TAG = "sra_devcheck_target";

	private static final List<String> FAILURES = new ArrayList<>();
	private static final AtomicInteger PENDING_SCREENSHOTS = new AtomicInteger();
	private static int serverTick;
	private static int idleTicks;
	private static int totalClientTicks;
	private static int visualStep;
	private static int visualWait;
	private static int checks;
	private static boolean finished;
	private static boolean worldPrepared;
	private static volatile boolean checksComplete;
	private static boolean clientCacheChecked;
	private static boolean reduceTestElement;
	private static volatile boolean requestClientRespawn;
	private static volatile boolean inventoryVisualReady;
	private static volatile boolean barVisualReady;
	private static Zombie barTarget;

	private ApocryphaDevCheck() {
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (!ENABLED || event.phase != TickEvent.Phase.END) {
			return;
		}
		List<ServerPlayer> players = event.getServer().getPlayerList().getPlayers();
		if (players.isEmpty()) {
			if (!worldPrepared) {
				serverTick = 0;
			}
			return;
		}
		ServerPlayer player = players.get(0);
		if (!player.isAlive()) {
			// 上一轮死亡测试可能把测试世界留在死亡界面；由客户端走原版重生命令。
			requestClientRespawn = true;
			return;
		}
		if (serverTick < 20 && !PlayerFlags.isDataReady(player)) {
			return;
		}
		serverTick++;
		try {
			switch (serverTick) {
				case 20 -> prepareWorld(player);
				case 40 -> setAngel(player);
				case 60 -> checkSlots(player, true);
				case 80 -> equipAngel(player);
				case 100 -> checkEquipped(player, true);
				case 120 -> setDemon(player);
				case 140 -> checkSlots(player, false);
				case 160 -> equipDemon(player);
				case 180 -> checkEquipped(player, false);
				case 200 -> prepareInventoryVisual(player);
				case 320 -> checkLootCommand(player);
				case 340 -> checkRealChest(player);
				case 360 -> checkItemProtection(player);
				case 400 -> setupApoptosisBar(player);
				case 420 -> checkApoptosisBar();
				case 440 -> checkProjectileRulesAndAirJumps(player);
				case 460 -> checkFear(player);
				case 480 -> checkElementAndExecute(player);
				case 500 -> checkStrictSwitch(player);
				case 520 -> checkBilingualTooltipTemplates();
				case 600 -> prepareDeathScenario(player, false);
				case 620 -> killTestPlayer(player);
				case 660 -> respawnTestPlayer(player);
				case 700 -> checkDeathRestore(player, false);
				case 760 -> prepareDeathScenario(player, true);
				case 780 -> killTestPlayer(player);
				case 820 -> respawnTestPlayer(player);
				case 860 -> {
					checkDeathRestore(player, true);
					run(player, "gamerule keepInventory false");
					player.setInvulnerable(true);
					player.setHealth(player.getMaxHealth());
					checksComplete = true;
				}
				default -> {
				}
			}
		} catch (Throwable throwable) {
			FAILURES.add("服务端步骤 " + serverTick + " 异常：" + throwable);
			log("服务端步骤 " + serverTick + " 异常：" + throwable);
			throwable.printStackTrace();
		}
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (!ENABLED || event.phase != TickEvent.Phase.END) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		totalClientTicks++;
		if (totalClientTicks > CLIENT_TOTAL_STOP_TICKS) {
			finish(client, "硬停止：客户端总 tick 超限");
			return;
		}
		if (client.player == null || client.level == null) {
			idleTicks++;
			if (idleTicks > CLIENT_IDLE_STOP_TICKS) {
				finish(client, "硬停止：等待测试世界超时");
			}
			return;
		}
		idleTicks = 0;
		if (client.player.isDeadOrDying()) {
			requestClientRespawn = true;
		}
		if (requestClientRespawn && client.player.isDeadOrDying()) {
			requestClientRespawn = false;
			client.player.respawn();
		}
		if (!clientCacheChecked) {
			ApoptosisBarState.accept(Integer.MAX_VALUE - 7, 50.0F, 0, 0);
			ApoptosisBarState.tick(client.level, ApocryphaConfig.apoptosisBarMax(),
					ApocryphaConfig.apoptosisInjuredSeconds() * 20);
			check("凋亡条会清理不存在实体的缓存",
					ApoptosisBarState.get(Integer.MAX_VALUE - 7) == null, "fake entity id");
			clientCacheChecked = true;
		}
		runVisualChecks(client);
	}

	/** 自检用：模拟其它模组在 NORMAL 阶段削减元素伤害，验证 LOWEST 仍钉回固定值。 */
	@SubscribeEvent
	public static void onTestElementReduction(LivingHurtEvent event) {
		if (ENABLED && reduceTestElement
				&& "summy-reliquary-apocrypha.apoptosis_element".equals(event.getSource().getMsgId())) {
			event.setAmount(event.getAmount() * 0.5F);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		if (!ENABLED) {
			return;
		}
		if (barTarget != null) {
			Apoptosis.clearForTest(barTarget);
			barTarget.discard();
			barTarget = null;
		}
	}

	private static void prepareWorld(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		run(player, "gamerule doMobSpawning false");
		run(player, "gamerule keepInventory false");
		run(player, "gamerule doDaylightCycle false");
		run(player, "difficulty easy");
		run(player, "time set night");
		player.setInvulnerable(true);
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.getInventory().clearContent();
		level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(32.0D),
				zombie -> zombie.getTags().contains(TARGET_TAG)).forEach(zombie -> {
					Apoptosis.clearForTest(zombie);
					zombie.discard();
				});
		CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
			for (var entry : handler.getCurios().entrySet()) {
				for (int slot = 0; slot < entry.getValue().getSlots(); slot++) {
					handler.setEquippedCurio(entry.getKey(), slot, ItemStack.EMPTY);
				}
			}
		});
		PlayerFlags.setAngel(player, false);
		PlayerFlags.setDemon(player, false);
		forceSlots(player, false);
		ApocryphaAcquisition.clearAcquisitionForTest(player);
		worldPrepared = true;
		check("测试世界已准备", level != null && player.isAlive(), "player=" + player.getName().getString());
	}

	/** 准备一轮真实死亡测试：背包 1 件、Curio 1 件。 */
	private static void prepareDeathScenario(ServerPlayer player, boolean keepInventory) {
		clearInventory(player);
		CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
			for (var entry : handler.getCurios().entrySet()) {
				for (int slot = 0; slot < entry.getValue().getSlots(); slot++) {
					handler.setEquippedCurio(entry.getKey(), slot, ItemStack.EMPTY);
				}
			}
		});
		PlayerFlags.setAngel(player, false);
		PlayerFlags.setDemon(player, true);
		forceSlots(player, false);
		equip(player, ApocryphaSlots.PERDITION_SYNECDOCHE, Apocrypha.FALLEN_HEAVEN.get());
		player.getInventory().add(new ItemStack(Apocrypha.NEW_TESTAMENT.get()));
		run(player, "gamerule keepInventory " + keepInventory);
		player.setInvulnerable(false);
		player.setHealth(player.getMaxHealth());
		Apoptosis.setStateForTest(player, 100, 0);
		int bag = countItem(player, Apocrypha.NEW_TESTAMENT.get());
		int worn = countItem(player, Apocrypha.FALLEN_HEAVEN.get());
		check("死亡测试已准备（keepInventory=" + keepInventory + "）", bag == 1 && worn == 1,
				"new_testament=" + bag + ", fallen_heaven=" + worn);
	}

	private static void killTestPlayer(ServerPlayer player) {
		player.hurt(player.serverLevel().damageSources().genericKill(), Float.MAX_VALUE);
		requestClientRespawn = true;
		check("测试玩家进入死亡状态", player.isDeadOrDying(),
				"health=" + player.getHealth()
						+ ", pending=" + ApocryphaAcquisition.pendingRestoreForTest(player));
	}

	private static void respawnTestPlayer(ServerPlayer player) {
		check("测试玩家已由客户端请求重生", player.isAlive(),
				"alive=" + player.isAlive() + ", health=" + player.getHealth());
	}

	private static void checkDeathRestore(ServerPlayer player, boolean keepInventory) {
		int bag = countItem(player, Apocrypha.NEW_TESTAMENT.get());
		int worn = countItem(player, Apocrypha.FALLEN_HEAVEN.get());
		boolean apoptosisCleared = !Apoptosis.isInjured(player) && Apoptosis.resistTicks(player) == 0;
		check("死亡恢复无丢失/复制且凋亡清空（keepInventory=" + keepInventory + "）",
				bag == 1 && worn == 1 && apoptosisCleared,
				"new_testament=" + bag + ", fallen_heaven=" + worn + ", apoptosisCleared=" + apoptosisCleared);
	}

	private static void setAngel(ServerPlayer player) {
		PlayerFlags.setDemon(player, false);
		PlayerFlags.setAngel(player, true);
		forceSlots(player, true);
		ApocryphaAcquisition.clearAcquisitionForTest(player);
		check("天使标记已设置", PlayerFlags.hasAngel(player), "angel=" + PlayerFlags.hasAngel(player));
	}

	private static void setDemon(ServerPlayer player) {
		PlayerFlags.setAngel(player, false);
		PlayerFlags.setDemon(player, true);
		forceSlots(player, false);
		ApocryphaAcquisition.clearAcquisitionForTest(player);
		check("恶魔标记已设置", PlayerFlags.isDemon(player), "demon=" + PlayerFlags.isDemon(player));
	}

	private static void checkSlots(ServerPlayer player, boolean angel) {
		List<String> slots = angel ? ApocryphaSlots.ANGEL_SERIES_SLOTS : ApocryphaSlots.DEMON_SERIES_SLOTS;
		boolean ok = true;
		for (String slot : slots) {
			int count = SlotSizing.slotsOf(player, slot);
			ok &= count == 1;
			log("栏位 " + slot + " = " + count + "（期望 1）");
		}
		check(angel ? "新约五个栏位各 1 格" : "凋亡五个栏位各 1 格", ok, "slots=" + slots);
	}

	private static void equipAngel(ServerPlayer player) {
		equip(player, ApocryphaSlots.NEW_TESTAMENT_HALO, Apocrypha.NEW_TESTAMENT.get());
		equip(player, ApocryphaSlots.NEW_TESTAMENT_TALENT, Apocrypha.SWIFT_FEATHER_COVENANT.get());
		equip(player, ApocryphaSlots.NEW_TESTAMENT_GRACE, Apocrypha.WING_FEATHER_SACRAMENT.get());
		equip(player, ApocryphaSlots.NEW_TESTAMENT_BLESSING, Apocrypha.INVERSE_FEATHER_HOVER.get());
		equip(player, ApocryphaSlots.NEW_TESTAMENT_REVELATION, Apocrypha.PAIRED_WINGS.get());
	}

	private static void equipDemon(ServerPlayer player) {
		equip(player, ApocryphaSlots.PERDITION_SYNECDOCHE, Apocrypha.FALLEN_HEAVEN.get());
		equip(player, ApocryphaSlots.PERDITION_EXEGESIS, Apocrypha.FALLEN_PLUME.get());
		equip(player, ApocryphaSlots.PERDITION_MASS, Apocrypha.SCOURGED_HEART.get());
		equip(player, ApocryphaSlots.PERDITION_MERCY, Apocrypha.WITHERED_BRUSH.get());
		equip(player, ApocryphaSlots.PERDITION_REQUIEM, Apocrypha.FINAL_SONG.get());
	}

	private static void checkEquipped(ServerPlayer player, boolean angel) {
		List<Item> items = angel
				? List.of(Apocrypha.NEW_TESTAMENT.get(), Apocrypha.SWIFT_FEATHER_COVENANT.get(),
						Apocrypha.WING_FEATHER_SACRAMENT.get(), Apocrypha.INVERSE_FEATHER_HOVER.get(),
						Apocrypha.PAIRED_WINGS.get())
				: List.of(Apocrypha.FALLEN_HEAVEN.get(), Apocrypha.FALLEN_PLUME.get(),
						Apocrypha.SCOURGED_HEART.get(), Apocrypha.WITHERED_BRUSH.get(),
						Apocrypha.FINAL_SONG.get());
		boolean ok = true;
		for (Item item : items) {
			boolean worn = CurioHelper.wears(player, item);
			ok &= worn;
			log("佩戴 " + item.getDescriptionId() + " = " + worn);
		}
		check(angel ? "新约五件均可佩戴" : "凋亡五件均可佩戴", ok, "items=" + items.size());
	}

	/** 准备物品图标与恶魔羽翼视检；客户端会等待本阶段完成后才开始截图。 */
	private static void prepareInventoryVisual(ServerPlayer player) {
		clearInventory(player);
		List<Item> items = List.of(
				Apocrypha.NEW_TESTAMENT.get(),
				Apocrypha.SWIFT_FEATHER_COVENANT.get(),
				Apocrypha.WING_FEATHER_SACRAMENT.get(),
				Apocrypha.INVERSE_FEATHER_HOVER.get(),
				Apocrypha.PAIRED_WINGS.get(),
				Apocrypha.FALLEN_HEAVEN.get(),
				Apocrypha.FALLEN_PLUME.get(),
				Apocrypha.SCOURGED_HEART.get(),
				Apocrypha.WITHERED_BRUSH.get(),
				Apocrypha.FINAL_SONG.get());
		for (Item item : items) {
			player.getInventory().add(new ItemStack(item));
		}
		FallenPlumeWings.flash(player);
		boolean allVisible = items.stream().allMatch(item ->
				player.getInventory().contains(new ItemStack(item)));
		check("10 件饰品已放入视检背包", allVisible, "items=" + items.size());
		inventoryVisualReady = true;
	}

	private static void checkLootCommand(ServerPlayer player) {
		clearInventory(player);
		PlayerFlags.setDemon(player, false);
		PlayerFlags.setAngel(player, true);
		forceSlots(player, true);
		ApocryphaAcquisition.clearAcquisitionForTest(player);
		int ownedBefore = ApocryphaAcquisition.ownedMaskForTest(player, true);
		run(player, "loot give @s loot minecraft:chests/simple_dungeon");
		int ownedAfter = ApocryphaAcquisition.ownedMaskForTest(player, true);
		int pityAfter = ApocryphaAcquisition.pityForTest(player, true);
		check("`/loot` 不触发 SRA 获取", ownedBefore == ownedAfter && ownedAfter == 0 && pityAfter == 0,
				"owned=" + ownedAfter + ", pity=" + pityAfter);
		clearInventory(player);
	}

	private static void checkRealChest(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		clearInventory(player);
		ApocryphaAcquisition.clearAcquisitionForTest(player);
		PlayerFlags.setDemon(player, false);
		PlayerFlags.setAngel(player, true);
		forceSlots(player, true);
		BlockPos pos = player.blockPosition().offset(2, 0, 0);
		level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
		if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
			check("真实箱子测试", false, "未找到 ChestBlockEntity");
			return;
		}
		chest.setLootTable(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon"), 0L);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(
				player, InteractionHand.MAIN_HAND, pos, hit));
		chest.unpackLootTable(player);
		int owned = ApocryphaAcquisition.ownedMaskForTest(player, true);
		check("真实箱子发放新约基础件", (owned & 1) != 0, "owned=" + owned);
		level.removeBlock(pos, false);
		clearInventory(player);

		checkDoubleChestConnectedHalf(player);
		checkDoubleChestSingleConsume(player);
	}

	/** 点击半边没有 LootTable、连接半边有时，仍必须按一次真实开箱结算。 */
	private static void checkDoubleChestConnectedHalf(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		clearInventory(player);
		ApocryphaAcquisition.clearAcquisitionForTest(player);
		BlockPos clicked = player.blockPosition().offset(4, 0, 0);
		BlockState left = Blocks.CHEST.defaultBlockState()
				.setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.NORTH)
				.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, ChestType.LEFT);
		Direction connectedDirection = net.minecraft.world.level.block.ChestBlock.getConnectedDirection(left);
		BlockPos other = clicked.relative(connectedDirection);
		BlockState right = Blocks.CHEST.defaultBlockState()
				.setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.NORTH)
				.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, ChestType.RIGHT);
		level.setBlockAndUpdate(clicked, left);
		level.setBlockAndUpdate(other, right);
		if (!(level.getBlockEntity(other) instanceof ChestBlockEntity otherChest)) {
			check("双箱另一半来源匹配", false, "连接半边不是 ChestBlockEntity");
			level.removeBlock(clicked, false);
			level.removeBlock(other, false);
			return;
		}
		otherChest.setLootTable(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon"), 0L);
		postBlockInteraction(player, clicked);
		otherChest.unpackLootTable(player);
		int owned = ApocryphaAcquisition.ownedMaskForTest(player, true);
		check("双箱另一半来源匹配", (owned & 1) != 0, "owned=" + owned);
		level.removeBlock(clicked, false);
		level.removeBlock(other, false);
		clearInventory(player);
	}

	/** 双箱两半都带表时，同一次交互只能消费一次来源窗口。 */
	private static void checkDoubleChestSingleConsume(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		clearInventory(player);
		ApocryphaAcquisition.clearAcquisitionForTest(player);
		BlockPos first = player.blockPosition().offset(6, 0, 0);
		BlockState left = Blocks.CHEST.defaultBlockState()
				.setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.NORTH)
				.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, ChestType.LEFT);
		Direction connectedDirection = net.minecraft.world.level.block.ChestBlock.getConnectedDirection(left);
		BlockPos second = first.relative(connectedDirection);
		BlockState right = Blocks.CHEST.defaultBlockState()
				.setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.NORTH)
				.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, ChestType.RIGHT);
		level.setBlockAndUpdate(first, left);
		level.setBlockAndUpdate(second, right);
		if (!(level.getBlockEntity(first) instanceof ChestBlockEntity firstChest)
				|| !(level.getBlockEntity(second) instanceof ChestBlockEntity secondChest)) {
			check("双箱只消费一次来源", false, "双箱方块实体不完整");
			level.removeBlock(first, false);
			level.removeBlock(second, false);
			return;
		}
		ResourceLocation table = ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon");
		firstChest.setLootTable(table, 0L);
		secondChest.setLootTable(table, 1L);
		postBlockInteraction(player, first);
		firstChest.unpackLootTable(player);
		secondChest.unpackLootTable(player);
		int count = countItem(player, Apocrypha.NEW_TESTAMENT.get());
		check("双箱只消费一次来源", count == 1, "new_testament=" + count);
		level.removeBlock(first, false);
		level.removeBlock(second, false);
		clearInventory(player);
	}

	private static void postBlockInteraction(ServerPlayer player, BlockPos pos) {
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(
				player, InteractionHand.MAIN_HAND, pos, hit));
	}

	private static void checkItemProtection(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 1.0D, player.getZ(),
				new ItemStack(Apocrypha.FALLEN_HEAVEN.get()));
		level.addFreshEntity(item);
		check("SRA 掉落实体不可摧毁", item.isInvulnerable(), "invulnerable=" + item.isInvulnerable());
		check("SRA 掉落实体无限寿命", item.getAge() == -32768, "age=" + item.getAge());
		item.discard();
	}

	private static void setupApoptosisBar(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos feet = player.blockPosition();
		// 在测试世界的当前位置搭一个短平台，保证镜头、目标与头顶条不会被地形遮挡。
		for (int x = -3; x <= 3; x++) {
			for (int z = -2; z <= 8; z++) {
				BlockPos floor = feet.offset(x, -1, z);
				level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
				for (int y = 0; y <= 3; y++) {
					BlockPos air = feet.offset(x, y, z);
					if (!air.equals(feet)) {
						level.setBlockAndUpdate(air, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
		Zombie zombie = EntityType.ZOMBIE.create(level);
		if (zombie == null) {
			check("凋亡条测试目标", false, "Zombie 创建失败");
			return;
		}
		double centerX = feet.getX() + 0.5D;
		double centerY = feet.getY();
		double centerZ = feet.getZ() + 0.5D;
		zombie.moveTo(centerX, centerY, centerZ + 5.0D, 180.0F, 0.0F);
		zombie.setNoAi(true);
		zombie.setInvulnerable(true);
		zombie.setPersistenceRequired();
		zombie.addTag(TARGET_TAG);
		level.addFreshEntity(zombie);
		Apoptosis.setStateForTest(zombie, 240, 0);
		barTarget = zombie;
		// yaw=0 面向 +Z；轻微抬头让目标与头顶条落在画面中央。
		player.teleportTo(level, centerX, centerY, centerZ, 0.0F, -8.0F);
		check("凋亡测试目标已生成", Apoptosis.isInjured(zombie), "target=" + zombie.getUUID());
		barVisualReady = true;
	}

	private static void checkApoptosisBar() {
		check("凋亡状态已同步", barTarget != null && Apoptosis.isInjured(barTarget), "barTarget=" + barTarget);
	}

	private static void checkProjectileRulesAndAirJumps(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Arrow arrow = new Arrow(level, player);
		SpectralArrow spectral = new SpectralArrow(level, player);
		ThrownTrident trident = new ThrownTrident(level, player, new ItemStack(Items.TRIDENT));
		check("普通箭被识别为弓箭", NewTestamentEffect.isPlayerBowArrowForTest(arrow), arrow.getType().toString());
		check("光灵箭被识别为弓箭", NewTestamentEffect.isPlayerBowArrowForTest(spectral),
				spectral.getType().toString());
		check("三叉戟不会误触发旧箭矢机制", !NewTestamentEffect.isPlayerBowArrowForTest(trident),
				trident.getType().toString());

		ApocryphaClientInput.resetAirJumpForTest();
		check("air_jumps=0 时无空中跳跃", !ApocryphaClientInput.consumeAirJumpForTest(0), "max=0");
		ApocryphaClientInput.resetAirJumpForTest();
		boolean firstOne = ApocryphaClientInput.consumeAirJumpForTest(1);
		boolean secondOne = ApocryphaClientInput.consumeAirJumpForTest(1);
		check("air_jumps=1 时只允许一次", firstOne && !secondOne, "first=" + firstOne + ", second=" + secondOne);
		ApocryphaClientInput.resetAirJumpForTest();
		boolean firstTwo = ApocryphaClientInput.consumeAirJumpForTest(2);
		boolean secondTwo = ApocryphaClientInput.consumeAirJumpForTest(2);
		boolean thirdTwo = ApocryphaClientInput.consumeAirJumpForTest(2);
		check("air_jumps=2 时允许两次", firstTwo && secondTwo && !thirdTwo,
				"results=" + firstTwo + "/" + secondTwo + "/" + thirdTwo);
	}

	private static void checkFear(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Zombie mob = EntityType.ZOMBIE.create(level);
		if (mob == null) {
			check("恐惧生物测试目标", false, "Zombie 创建失败");
			return;
		}
		mob.moveTo(player.position().add(2.0D, 0.0D, 0.0D));
		mob.setNoAi(false);
		level.addFreshEntity(mob);
		WitheredBrush.applyFear(level, player, mob);
		check("恐惧生物进入硬停状态", WitheredBrush.isFearedForTest(mob) && mob.isNoAi(),
				"feared=" + WitheredBrush.isFearedForTest(mob) + ", noAi=" + mob.isNoAi());
		WitheredBrush.clear(mob);
		check("恐惧结束后恢复生物 AI", !WitheredBrush.isFearedForTest(mob) && !mob.isNoAi(),
				"feared=" + WitheredBrush.isFearedForTest(mob) + ", noAi=" + mob.isNoAi());
		mob.discard();

		ApocryphaConfig.setFearAffectsPlayersForTest(true);
		Minecart minecart = EntityType.MINECART.create(level);
		if (minecart != null) {
			minecart.moveTo(player.position());
			level.addFreshEntity(minecart);
			boolean mounted = player.startRiding(minecart, true);
			WitheredBrush.applyFear(level, player, player);
			double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
			check("玩家恐惧会强制下马并停止移动", mounted && !player.isPassenger()
							&& WitheredBrush.isFearedForTest(player) && speed <= 1.0E-9D,
					"mounted=" + mounted + ", passenger=" + player.isPassenger() + ", speed=" + speed);
			WitheredBrush.clear(player);
			check("玩家恐惧清理后恢复移动", !WitheredBrush.isFearedForTest(player)
							&& player.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0D,
					"speed=" + player.getAttributeValue(Attributes.MOVEMENT_SPEED));
			minecart.discard();
		} else {
			check("玩家恐惧下马测试", false, "Minecart 创建失败");
		}
		ApocryphaConfig.setFearAffectsPlayersForTest(false);
	}

	private static void checkElementAndExecute(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Zombie fixedTarget = EntityType.ZOMBIE.create(level);
		if (fixedTarget == null) {
			check("固定元素伤害测试", false, "Zombie 创建失败");
			return;
		}
		fixedTarget.moveTo(player.position().add(3.0D, 0.0D, 0.0D));
		fixedTarget.setNoAi(true);
		fixedTarget.setHealth(fixedTarget.getMaxHealth());
		level.addFreshEntity(fixedTarget);
		reduceTestElement = true;
		boolean landed = Apoptosis.dealElementDamageForTest(player, fixedTarget, 10.0F);
		reduceTestElement = false;
		float lost = fixedTarget.getMaxHealth() - fixedTarget.getHealth();
		check("元素伤害固定值覆盖减伤", landed && Math.abs(lost - 10.0F) < 0.05F,
				"lost=" + lost + "（期望 10）");
		Apoptosis.clearForTest(fixedTarget);
		fixedTarget.discard();

		PlayerFlags.setAngel(player, false);
		PlayerFlags.setDemon(player, true);
		forceSlots(player, false);
		equip(player, ApocryphaSlots.PERDITION_MASS, Apocrypha.SCOURGED_HEART.get());
		boolean wearsHeart = CurioHelper.wears(player, Apocrypha.SCOURGED_HEART.get());
		check("笞心已佩戴（固定伤害斩杀测试）", wearsHeart, "wears=" + wearsHeart);
		Zombie executeTarget = EntityType.ZOMBIE.create(level);
		if (executeTarget == null) {
			check("固定伤害激活斩杀", false, "Zombie 创建失败");
			return;
		}
		executeTarget.moveTo(player.position().add(3.0D, 0.0D, 0.0D));
		executeTarget.setNoAi(true);
		executeTarget.setHealth(1.5F);
		level.addFreshEntity(executeTarget);
		Apoptosis.setStateForTest(executeTarget, 100, 0);
		ResourceKey<DamageType> holyLightKey = ResourceKey.create(Registries.DAMAGE_TYPE,
				ResourceLocation.fromNamespaceAndPath("summy-reliquary", "holy_light"));
		var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(holyLightKey).orElse(null);
		if (holder == null) {
			check("固定伤害激活斩杀", false, "缺少 summy-reliquary:holy_light");
		} else {
			DamageSource fixedSource = new DamageSource(holder, null, player);
			executeTarget.invulnerableTime = 0;
			executeTarget.hurt(fixedSource, 0.5F);
			check("玩家归属固定伤害可激活笞心斩杀", !executeTarget.isAlive(),
					"health=" + executeTarget.getHealth() + ", wearsHeart=" + wearsHeart
							+ ", executeUsed=" + Apoptosis.executeUsedForTest(executeTarget));
		}
		Apoptosis.clearForTest(executeTarget);
		executeTarget.discard();
	}

	private static void checkStrictSwitch(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		ApocryphaConfig.setEnableForTest(false);
		FallenPlumeWings.clear(player);
		FallenPlumeWings.flash(player);
		check("总开关关闭时不能展开恶魔翅膀", !FallenPlumeWings.isActiveForTest(player),
				"active=" + FallenPlumeWings.isActiveForTest(player));

		Zombie mob = EntityType.ZOMBIE.create(level);
		if (mob != null) {
			mob.moveTo(player.position().add(2.0D, 0.0D, 0.0D));
			level.addFreshEntity(mob);
			WitheredBrush.applyFear(level, player, mob);
			check("总开关关闭时不能施加恐惧", !WitheredBrush.isFearedForTest(mob),
					"feared=" + WitheredBrush.isFearedForTest(mob));
			WitheredBrush.clear(mob);
			mob.discard();
		}
		ApocryphaClientInput.resetAirJumpForTest();
		check("总开关关闭时客户端不消费二段跳", !ApocryphaClientInput.consumeAirJumpForTest(1),
				"max=1");
		ApocryphaConfig.setEnableForTest(true);
		check("总开关已恢复", ApocryphaConfig.enable(), "enable=" + ApocryphaConfig.enable());
	}

	private static void checkBilingualTooltipTemplates() {
		try {
			JsonObject zh = readLanguage("zh_cn");
			JsonObject en = readLanguage("en_us");
			String zhNew = renderTemplate(zh.get("item.summy-reliquary-apocrypha.new_testament.shift.2")
					.getAsString(), 35, 8, 25, 3, 5);
			String enNew = renderTemplate(en.get("item.summy-reliquary-apocrypha.new_testament.shift.2")
					.getAsString(), 35, 8, 25, 3, 5);
			String zhWing = renderTemplate(zh.get("item.summy-reliquary-apocrypha.wing_feather_sacrament.shift.7")
					.getAsString(), 10, 2);
			String enWing = renderTemplate(en.get("item.summy-reliquary-apocrypha.wing_feather_sacrament.shift.7")
					.getAsString(), 10, 2);
			zhNew = stripInlineMarkers(zhNew);
			enNew = stripInlineMarkers(enNew);
			zhWing = stripInlineMarkers(zhWing);
			enWing = stripInlineMarkers(enWing);
			check("中文新约提示参数顺序", zhNew.contains("35%") && zhNew.contains("8 格")
							&& zhNew.contains("0~25°") && zhNew.contains("3~5"),
					zhNew);
			check("英文新约提示参数顺序", enNew.contains("35%") && enNew.contains("8 blocks")
							&& enNew.contains("3~5") && enNew.contains("0~25°"),
					enNew);
			check("中文翼羽衰减参数顺序", zhWing.contains("10 秒后") && zhWing.contains("每 2 秒"),
					zhWing);
			check("英文翼羽衰减参数顺序", enWing.contains("after 10 s") && enWing.contains("every 2 s"),
					enWing);
		} catch (Throwable throwable) {
			check("双语提示模板读取", false, throwable.toString());
		}
	}

	private static JsonObject readLanguage(String locale) throws Exception {
		String path = "/assets/summy-reliquary-apocrypha/lang/" + locale + ".json";
		try (var stream = ApocryphaDevCheck.class.getResourceAsStream(path)) {
			if (stream == null) {
				throw new IllegalStateException("找不到语言文件 " + path);
			}
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static String renderTemplate(String template, Object... args) {
		String result = template;
		for (Object arg : args) {
			result = result.replaceFirst("%s", java.util.regex.Matcher.quoteReplacement(String.valueOf(arg)));
		}
		return result;
	}

	private static String stripInlineMarkers(String value) {
		return value.replaceAll("\\{[^|}]+\\|([^}]*)}", "$1");
	}

	private static void equip(ServerPlayer player, String slot, Item item) {
		CuriosApi.getCuriosInventory(player).ifPresent(handler ->
				handler.setEquippedCurio(slot, 0, new ItemStack(item)));
	}

	private static void clearInventory(ServerPlayer player) {
		player.getInventory().clearContent();
	}

	private static int countItem(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		if (CurioHelper.wears(player, item)) {
			count++;
		}
		return count;
	}

	private static void forceSlots(ServerPlayer player, boolean angel) {
		for (String slot : ApocryphaSlots.ANGEL_SERIES_SLOTS) {
			SlotSizing.sync(player, slot, angel ? 1 : 0);
		}
		for (String slot : ApocryphaSlots.DEMON_SERIES_SLOTS) {
			SlotSizing.sync(player, slot, angel ? 0 : 1);
		}
	}

	private static void run(ServerPlayer player, String command) {
		player.getServer().getCommands().performPrefixedCommand(
				player.createCommandSourceStack().withPermission(3), command);
	}

	/** 按服务端测试阶段推进视检，避免客户端在栏位、饰品和目标准备完成前抢拍。 */
	private static void runVisualChecks(Minecraft client) {
		if (visualStep == 0) {
			if (inventoryVisualReady) {
				visualStep = 1;
				visualWait = 10;
			}
			return;
		}
		if (visualStep == 6) {
			if (!barVisualReady) {
				return;
			}
			client.options.setCameraType(CameraType.FIRST_PERSON);
			visualStep = 7;
			visualWait = 20;
			return;
		}
		if (visualWait > 0) {
			visualWait--;
			return;
		}
		switch (visualStep) {
			case 1 -> {
				client.setScreen(new InventoryScreen(client.player));
				visualStep = 2;
				visualWait = 10;
			}
			case 2 -> {
				screenshot(client, "sra_devcheck_inventory.png");
				client.setScreen(null);
				visualStep = 3;
				visualWait = 10;
			}
			case 3 -> {
				client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
				visualStep = 4;
				visualWait = 10;
			}
			case 4 -> {
				screenshot(client, "sra_devcheck_wings.png");
				visualStep = 5;
				visualWait = 10;
			}
			case 5 -> {
				client.options.setCameraType(CameraType.FIRST_PERSON);
				visualStep = 6;
			}
			case 7 -> {
				screenshot(client, "sra_devcheck_apoptosis.png");
				visualStep = 8;
				visualWait = 40;
			}
			case 8 -> {
				if (PENDING_SCREENSHOTS.get() == 0 && checksComplete) {
					finish(client, "客户端自检结束");
				}
			}
			default -> {
			}
		}
	}

	private static void screenshot(Minecraft client, String name) {
		PENDING_SCREENSHOTS.incrementAndGet();
		Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(),
				message -> {
					log("截图 " + name + "：" + message.getString());
					PENDING_SCREENSHOTS.decrementAndGet();
				});
	}

	private static synchronized void check(String label, boolean ok, String detail) {
		checks++;
		if (!ok) {
			FAILURES.add(label + "｜" + detail);
		}
		log((ok ? "OK  " : "FAIL") + " " + label + "｜" + detail);
	}

	private static void finish(Minecraft client, String reason) {
		if (finished) {
			return;
		}
		finished = true;
		log("===== " + reason + "：检查 " + checks + " 项，失败 " + FAILURES.size() + " 项 =====");
		for (String failure : FAILURES) {
			log("FAILED｜" + failure);
		}
		client.stop();
	}

	private static void log(String message) {
		Apocrypha.LOGGER.info("[DEVCHECK] {}", message);
	}
}
