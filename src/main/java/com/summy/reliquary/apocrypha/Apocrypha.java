package com.summy.reliquary.apocrypha;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.effect.ArrowDamageBonuses;
import com.summy.reliquary.apocrypha.effect.Apoptosis;
import com.summy.reliquary.apocrypha.effect.FallenPlumeWings;
import com.summy.reliquary.apocrypha.effect.WitheredBrush;
import com.summy.reliquary.apocrypha.command.ApocryphaCommand;
import com.summy.reliquary.apocrypha.effect.NewTestamentEffect;
import com.summy.reliquary.apocrypha.effect.SacramentArrows;
import com.summy.reliquary.apocrypha.entity.NewTestamentArrow;
import com.summy.reliquary.apocrypha.entity.HolyArrow;
import com.summy.reliquary.apocrypha.integration.ReliquaryRef;
import com.summy.reliquary.apocrypha.loot.ApocryphaAcquisition;
import com.summy.reliquary.apocrypha.loot.ApocryphaItemProtection;
import com.summy.reliquary.apocrypha.loot.ApocryphaLootModifier;
import com.summy.reliquary.apocrypha.item.NewTestamentItem;
import com.summy.reliquary.apocrypha.item.SwiftFeatherCovenantItem;
import com.summy.reliquary.apocrypha.item.WingFeatherSacramentItem;
import com.summy.reliquary.apocrypha.item.InverseFeatherHoverItem;
import com.summy.reliquary.apocrypha.item.PairedWingsItem;
import com.summy.reliquary.apocrypha.item.FallenHeavenItem;
import com.summy.reliquary.apocrypha.item.FallenPlumeItem;
import com.summy.reliquary.apocrypha.item.ScourgedHeartItem;
import com.summy.reliquary.apocrypha.item.WitheredBrushItem;
import com.summy.reliquary.apocrypha.item.FinalSongItem;
import com.summy.reliquary.apocrypha.net.ApocryphaNetworking;
import com.summy.reliquary.apocrypha.slot.ApocryphaSlots;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Summy Reliquary: Apocrypha 主入口。
 *
 * <p>本模组是 Summy Reliquary（SR）的扩展包，SR 是硬前置（见 META-INF/mods.toml）：
 * 栏位体系、天使标记门槛、真实伤害口径、全伤害增幅乘区、圣心箭矢追踪等都由 SR 提供。
 *
 * <p>两个名字的区别与 SR 一致：
 * MOD_ID 是 Forge 的 modId（不允许连字符）；
 * NAMESPACE 是注册表 / 数据包 / 资源命名空间（用连字符，与 SR 的写法保持一致）。
 */
@Mod(Apocrypha.MOD_ID)
public class Apocrypha {
	/** Forge modId（不允许连字符） */
	public static final String MOD_ID = "summy_reliquary_apocrypha";
	/** 注册表 / 数据包 / 资源命名空间 */
	public static final String NAMESPACE = "summy-reliquary-apocrypha";
	public static final Logger LOGGER = LogUtils.getLogger();

	private static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, NAMESPACE);
	private static final DeferredRegister<CreativeModeTab> TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NAMESPACE);
	private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
			DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NAMESPACE);
	private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_CODECS =
			DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NAMESPACE);

	/** 落箭实体：与原版箭矢同档的同步范围 */
	public static final RegistryObject<EntityType<NewTestamentArrow>> NEW_TESTAMENT_ARROW =
			ENTITY_TYPES.register("new_testament_arrow",
					() -> EntityType.Builder
							.<NewTestamentArrow>of(NewTestamentArrow::new, MobCategory.MISC)
							.sized(0.5F, 0.5F)
							.clientTrackingRange(4)
							.updateInterval(20)
							.build(id("new_testament_arrow").toString()));

	/**
	 * 穿透圣光矢（比翼·连携）：与原版箭矢同档的同步范围。
	 *
	 * <p>外观是光灵箭（渲染器换贴图），飞行时无重力、无限穿透。
	 */
	public static final RegistryObject<EntityType<HolyArrow>> HOLY_ARROW =
			ENTITY_TYPES.register("holy_arrow",
					() -> EntityType.Builder
							.<HolyArrow>of(HolyArrow::new, MobCategory.MISC)
							.sized(0.5F, 0.5F)
							.clientTrackingRange(4)
							.updateInterval(20)
							.build(id("holy_arrow").toString()));

	/** 新约：装入「新约-光环」栏位的饰品（需要天使标记） */
	public static final RegistryObject<Item> NEW_TESTAMENT =
			ITEMS.register("new_testament", () -> new NewTestamentItem(new Item.Properties().stacksTo(1)));

	/** 迅羽·协约：装入「新约-才能」栏位的饰品（需要天使标记；只改写「新约」的落箭参数） */
	public static final RegistryObject<Item> SWIFT_FEATHER_COVENANT =
			ITEMS.register("swift_feather_covenant",
					() -> new SwiftFeatherCovenantItem(new Item.Properties().stacksTo(1)));

	/** 翼羽·圣餐：装入「新约-恩典」栏位的饰品（需要天使标记；光羽凝聚 → 圣餐之矢） */
	public static final RegistryObject<Item> WING_FEATHER_SACRAMENT =
			ITEMS.register("wing_feather_sacrament",
					() -> new WingFeatherSacramentItem(new Item.Properties().stacksTo(1)));

	/** 逆羽·滞空：装入「新约-祝福」栏位的饰品（需要天使标记；下落强化 + 缓降 + 额外跳跃） */
	public static final RegistryObject<Item> INVERSE_FEATHER_HOVER =
			ITEMS.register("inverse_feather_hover",
					() -> new InverseFeatherHoverItem(new Item.Properties().stacksTo(1)));

	/** 比翼·连携：装入「新约-启示」栏位的饰品（需要天使标记；追加穿透圣光矢 + 与新约协同） */
	public static final RegistryObject<Item> PAIRED_WINGS =
			ITEMS.register("paired_wings",
					() -> new PairedWingsItem(new Item.Properties().stacksTo(1)));

	/** 堕天：装入「殁亡之提喻」栏位的饰品（需要恶魔标记；开启凋亡套系的核心机制） */
	public static final RegistryObject<Item> FALLEN_HEAVEN =
			ITEMS.register("fallen_heaven",
					() -> new FallenHeavenItem(new Item.Properties().stacksTo(1)));

	/** 坠羽：装入「殁亡之释经」栏位的饰品（需要恶魔标记；凋亡套系 #2，4 刀爆条 + 比例爆伤 + 爆条展翅） */
	public static final RegistryObject<Item> FALLEN_PLUME =
			ITEMS.register("fallen_plume",
					() -> new FallenPlumeItem(new Item.Properties().stacksTo(1)));

	/** 笞心：装入「殁亡之弥撒」栏位的饰品（需要恶魔标记；凋亡套系 #3，爆伤与附加 ×1.5 + 斩杀） */
	public static final RegistryObject<Item> SCOURGED_HEART =
			ITEMS.register("scourged_heart",
					() -> new ScourgedHeartItem(new Item.Properties().stacksTo(1)));

	/** 枯笔：装入「殁亡之悲悯」栏位的饰品（需要恶魔标记；凋亡套系 #4，抵抗削减 + 恐惧） */
	public static final RegistryObject<Item> WITHERED_BRUSH =
			ITEMS.register("withered_brush",
					() -> new WitheredBrushItem(new Item.Properties().stacksTo(1)));

	/** 终歌：装入「殁亡之终歌」栏位的饰品（需要恶魔标记；凋亡套系 #5 收官，每秒灼伤 + 减疗 80% + 受伤 +10%） */
	public static final RegistryObject<Item> FINAL_SONG =
			ITEMS.register("final_song",
					() -> new FinalSongItem(new Item.Properties().stacksTo(1)));

	/** 战利品修饰器 codec：数据文件只负责把 Forge 的调用转进获取状态机 */
	public static final RegistryObject<Codec<ApocryphaLootModifier>> ACQUISITION_LOOT_MODIFIER =
			LOOT_MODIFIER_CODECS.register("acquisition_reward", () -> ApocryphaLootModifier.CODEC);

	/** 本模组专属创造标签页 */
	public static final RegistryObject<CreativeModeTab> ITEM_GROUP = TABS.register("main", () ->
			CreativeModeTab.builder()
					.title(Component.translatable("itemGroup." + NAMESPACE + ".main"))
					// 创造页图标用「比翼·连携」（作者指定）
					.icon(() -> new ItemStack(PAIRED_WINGS.get()))
					.displayItems((parameters, output) -> {
						output.accept(NEW_TESTAMENT.get());
						output.accept(SWIFT_FEATHER_COVENANT.get());
						output.accept(WING_FEATHER_SACRAMENT.get());
						output.accept(INVERSE_FEATHER_HOVER.get());
						output.accept(PAIRED_WINGS.get());
						output.accept(FALLEN_HEAVEN.get());
						output.accept(FALLEN_PLUME.get());
						output.accept(SCOURGED_HEART.get());
						output.accept(WITHERED_BRUSH.get());
						output.accept(FINAL_SONG.get());
					})
					.build());

	public Apocrypha() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ITEMS.register(modBus);
		TABS.register(modBus);
		ENTITY_TYPES.register(modBus);
		LOOT_MODIFIER_CODECS.register(modBus);

		// Curios 栏位校验谓词：本模组的栏位只接受指定物品
		ApocryphaSlots.registerValidators();

		// 配置：config/summy_reliquary_apocrypha-common.toml
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ApocryphaConfig.SPEC);

		// 游戏事件总线（命中判定与散射）。显式注册，不依赖注解自动注册：
		// 与 SR 的做法一致，避免注解扫描在这个环境里的不确定性。
		MinecraftForge.EVENT_BUS.register(NewTestamentEffect.class);
		// 圣餐之矢：放箭登记 + 给新生成的箭打标记 + HIGHEST 放大伤害
		MinecraftForge.EVENT_BUS.register(SacramentArrows.class);
		// 箭矢伤害加成（圣餐之矢 +N% 与 逆羽·滞空 +M% 同轴相加）
		MinecraftForge.EVENT_BUS.register(ArrowDamageBonuses.class);
		// 凋亡（恶魔线）：损伤条状态机、元素伤害钉住、减疗与玩家侧 Boss 条
		MinecraftForge.EVENT_BUS.register(Apoptosis.class);
		// 凋亡 #2「坠羽」：爆条时在引爆者背后展开的翅膀（标志写进饰品 NBT，客户端用 Curios 渲染层画）
		MinecraftForge.EVENT_BUS.register(FallenPlumeWings.class);
		// 凋亡 #4「枯笔」：恐惧（生物冻结 / 玩家定身与封禁近战·右击）
		MinecraftForge.EVENT_BUS.register(WitheredBrush.class);
		// 两套饰品的野箱获取状态机、死亡恢复与登录兼容扫描
		MinecraftForge.EVENT_BUS.register(ApocryphaAcquisition.class);
		// 10 件独占饰品的掉落物不可摧毁 / 永不消失，并从死亡掉落中剔除
		MinecraftForge.EVENT_BUS.register(ApocryphaItemProtection.class);
		// 本模组的指令（/summyapocrypha fear players <on|off>）
		MinecraftForge.EVENT_BUS.register(ApocryphaCommand.class);
		// 本模组自己的网络通道（协议 "1"：目前只有"空中按下跳跃键"一条 C2S 包）
		ApocryphaNetworking.register();

		LOGGER.info("[Summy Reliquary: Apocrypha] 初始化完成（前置 Summy Reliquary {}、本模组协议 {}、SR 协议 {}）",
				ReliquaryRef.version(), ApocryphaNetworking.protocolVersion(), ReliquaryRef.protocolVersion());
	}

	/** 构造本模组命名空间下的标识符 */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
	}
}
