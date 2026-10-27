package com.summy.reliquary.apocrypha.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * 两套饰品共用的战利品入口。
 *
 * <p>实际的箱子类型、玩家标记、保底与奖品选择都在 {@link ApocryphaAcquisition}；
 * 这里只负责让 Forge 在所有战利品生成路径上调用一次。
 */
public final class ApocryphaLootModifier extends LootModifier {
	public static final Codec<ApocryphaLootModifier> CODEC = RecordCodecBuilder.create(instance ->
			codecStart(instance).apply(instance, ApocryphaLootModifier::new));

	private ApocryphaLootModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Override
	protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		ApocryphaAcquisition.tryGrant(context);
		return generatedLoot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
}
