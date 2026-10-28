package com.summy.reliquary.apocrypha.devcheck;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.effect.Apoptosis;
import com.summy.reliquary.apocrypha.effect.FallenPlumeWings;
import com.summy.reliquary.apocrypha.loot.ApocryphaAcquisition;
import com.summy.reliquary.apocrypha.slot.ApocryphaSlots;
import com.summy.reliquary.effect.PlayerFlags;
import com.summy.reliquary.effect.SlotSizing;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
			serverTick = 0;
			return;
		}
		ServerPlayer player = players.get(0);
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
		runVisualChecks(client);
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
		run(player, "gamerule keepInventory true");
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
		check("测试世界已准备", level != null && player.isAlive(), "player=" + player.getName().getString());
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

	private static void equip(ServerPlayer player, String slot, Item item) {
		CuriosApi.getCuriosInventory(player).ifPresent(handler ->
				handler.setEquippedCurio(slot, 0, new ItemStack(item)));
	}

	private static void clearInventory(ServerPlayer player) {
		player.getInventory().clearContent();
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
				if (PENDING_SCREENSHOTS.get() == 0) {
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

	private static void check(String label, boolean ok, String detail) {
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
