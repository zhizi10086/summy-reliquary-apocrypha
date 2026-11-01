package com.summy.reliquary.apocrypha.integration;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * JEI 原生“信息”分类集成。
 *
 * <p>本类只在客户端安装 JEI 时由 JEI 扫描并加载；JEI 只是编译期依赖，不写进
 * {@code mods.toml}，也不随正式 jar 打包。十件饰品各自生成一页“获取 + 保护”说明，
 * 通过 JEI 已内置的 {@code RecipeTypes.INFORMATION} 分类显示，不自定义 GUI。
 */
@JeiPlugin
public final class ApocryphaJeiPlugin implements IModPlugin {
	private static final String KEY_PREFIX = "jei.summy-reliquary-apocrypha.info.";

	private static final List<InfoEntry> ANGEL_ITEMS = List.of(
			new InfoEntry(Apocrypha.NEW_TESTAMENT, 0),
			new InfoEntry(Apocrypha.SWIFT_FEATHER_COVENANT, 1),
			new InfoEntry(Apocrypha.WING_FEATHER_SACRAMENT, 2),
			new InfoEntry(Apocrypha.INVERSE_FEATHER_HOVER, 3),
			new InfoEntry(Apocrypha.PAIRED_WINGS, 4)
	);

	private static final List<InfoEntry> DEMON_ITEMS = List.of(
			new InfoEntry(Apocrypha.FALLEN_HEAVEN, 0),
			new InfoEntry(Apocrypha.FALLEN_PLUME, 1),
			new InfoEntry(Apocrypha.SCOURGED_HEART, 2),
			new InfoEntry(Apocrypha.WITHERED_BRUSH, 3),
			new InfoEntry(Apocrypha.FINAL_SONG, 4)
	);

	@Override
	public ResourceLocation getPluginUid() {
		return Apocrypha.id("jei");
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		int chancePercent = ApocryphaConfig.acquisitionChancePercent();
		int pityChests = ApocryphaConfig.acquisitionPityChests();
		for (InfoEntry entry : ANGEL_ITEMS) {
			registerInfo(registration, entry, "angel", Apocrypha.NEW_TESTAMENT, chancePercent, pityChests);
		}
		for (InfoEntry entry : DEMON_ITEMS) {
			registerInfo(registration, entry, "demon", Apocrypha.FALLEN_HEAVEN, chancePercent, pityChests);
		}
	}

	private static void registerInfo(IRecipeRegistration registration, InfoEntry entry, String routeKey,
			Supplier<? extends Item> baseItem, int chancePercent, int pityChests) {
		List<Component> lines = new ArrayList<>();
		lines.add(text("position", text(routeKey), entry.index() + 1));
		lines.add(text("containers"));
		lines.add(text("marks"));
		if (entry.index() == 0) {
			lines.add(text("base_guarantee"));
			lines.add(text("base_unlock"));
		} else {
			lines.add(text("later_unlock", new ItemStack(baseItem.get()).getHoverName()));
			lines.add(text("later_chance", chancePercent + "%"));
			lines.add(pityChests > 0
					? text("pity_on", pityChests)
					: text("pity_off"));
		}
		lines.add(text("protection"));
		registration.addItemStackInfo(new ItemStack(entry.item().get()), lines.toArray(Component[]::new));
	}

	private static Component text(String suffixKey, Object... arguments) {
		return Component.translatable(KEY_PREFIX + suffixKey, arguments);
	}

	private record InfoEntry(Supplier<? extends Item> item, int index) {
	}
}
