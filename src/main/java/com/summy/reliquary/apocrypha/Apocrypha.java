package com.summy.reliquary.apocrypha;

import com.mojang.logging.LogUtils;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.effect.NewTestamentEffect;
import com.summy.reliquary.apocrypha.entity.NewTestamentArrow;
import com.summy.reliquary.apocrypha.integration.ReliquaryRef;
import com.summy.reliquary.apocrypha.item.NewTestamentItem;
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

	/** 落箭实体：与原版箭矢同档的同步范围 */
	public static final RegistryObject<EntityType<NewTestamentArrow>> NEW_TESTAMENT_ARROW =
			ENTITY_TYPES.register("new_testament_arrow",
					() -> EntityType.Builder
							.<NewTestamentArrow>of(NewTestamentArrow::new, MobCategory.MISC)
							.sized(0.5F, 0.5F)
							.clientTrackingRange(4)
							.updateInterval(20)
							.build(id("new_testament_arrow").toString()));

	/** 新约：装入「新约-光环」栏位的饰品（需要天使标记） */
	public static final RegistryObject<Item> NEW_TESTAMENT =
			ITEMS.register("new_testament", () -> new NewTestamentItem(new Item.Properties().stacksTo(1)));

	/** 本模组专属创造标签页 */
	public static final RegistryObject<CreativeModeTab> ITEM_GROUP = TABS.register("main", () ->
			CreativeModeTab.builder()
					.title(Component.translatable("itemGroup." + NAMESPACE + ".main"))
					.icon(() -> new ItemStack(NEW_TESTAMENT.get()))
					.displayItems((parameters, output) -> output.accept(NEW_TESTAMENT.get()))
					.build());

	public Apocrypha() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ITEMS.register(modBus);
		TABS.register(modBus);
		ENTITY_TYPES.register(modBus);

		// Curios 栏位校验谓词：本模组的栏位只接受指定物品
		ApocryphaSlots.registerValidators();

		// 配置：config/summy_reliquary_apocrypha-common.toml
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ApocryphaConfig.SPEC);

		// 游戏事件总线（命中判定与散射）。显式注册，不依赖注解自动注册：
		// 与 SR 的做法一致，避免注解扫描在这个环境里的不确定性。
		MinecraftForge.EVENT_BUS.register(NewTestamentEffect.class);

		LOGGER.info("[Summy Reliquary: Apocrypha] 初始化完成（前置 Summy Reliquary {}、协议 {}）",
				ReliquaryRef.version(), ReliquaryRef.protocolVersion());
	}

	/** 构造本模组命名空间下的标识符 */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
	}
}
