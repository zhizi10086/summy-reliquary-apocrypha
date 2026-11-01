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

import java.util.List;
import java.util.function.Supplier;

/**
 * JEI 原生“信息”分类集成。
 *
 * <p>JEI 只是编译期依赖，不写进 {@code mods.toml}，也不随正式 jar 打包。
 * 两个基础件各占一页；两条路线的后续四件分别共用一页，避免重复且冗长的说明。
 */
@JeiPlugin
public final class ApocryphaJeiPlugin implements IModPlugin {
	private static final String KEY_PREFIX = "jei.summy-reliquary-apocrypha.info.";

	private static final List<Supplier<? extends Item>> ANGEL_LATER = List.of(
			Apocrypha.SWIFT_FEATHER_COVENANT,
			Apocrypha.WING_FEATHER_SACRAMENT,
			Apocrypha.INVERSE_FEATHER_HOVER,
			Apocrypha.PAIRED_WINGS
	);

	private static final List<Supplier<? extends Item>> DEMON_LATER = List.of(
			Apocrypha.FALLEN_PLUME,
			Apocrypha.SCOURGED_HEART,
			Apocrypha.WITHERED_BRUSH,
			Apocrypha.FINAL_SONG
	);

	@Override
	public ResourceLocation getPluginUid() {
		return Apocrypha.id("jei");
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registerBase(registration, Apocrypha.NEW_TESTAMENT,
				text("base.angel"), text("base.new_testament"));
		registerBase(registration, Apocrypha.FALLEN_HEAVEN,
				text("base.demon"), text("base.fallen_heaven"));

		registerLater(registration, ANGEL_LATER,
				text("later.angel"), text("later.route.new_testament"));
		registerLater(registration, DEMON_LATER,
				text("later.demon"), text("later.route.apoptosis"));
	}

	private static void registerBase(IRecipeRegistration registration, Supplier<? extends Item> item,
			Component condition, Component guarantee) {
		registration.addItemStackInfo(new ItemStack(item.get()), condition, guarantee);
	}

	private static void registerLater(IRecipeRegistration registration, List<Supplier<? extends Item>> items,
			Component condition, Component routeName) {
		int chancePercent = ApocryphaConfig.acquisitionChancePercent();
		int pityChests = ApocryphaConfig.acquisitionPityChests();
		List<ItemStack> stacks = items.stream()
				.map(item -> new ItemStack(item.get()))
				.toList();
		if (pityChests > 0) {
			registration.addItemStackInfo(stacks, condition,
					text("later.chance_with_pity", chancePercent + "%", routeName, pityChests));
		} else {
			registration.addItemStackInfo(stacks, condition,
					text("later.chance", chancePercent + "%", routeName),
					text("later.no_pity"));
		}
	}

	private static Component text(String suffixKey, Object... arguments) {
		return Component.translatable(KEY_PREFIX + suffixKey, arguments);
	}
}
