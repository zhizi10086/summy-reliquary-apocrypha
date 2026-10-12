package com.summy.reliquary.apocrypha.item;

import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.slot.ApocryphaSlots;
import com.summy.reliquary.effect.PlayerFlags;
import com.summy.reliquary.item.CurioItemSupport;
import com.summy.reliquary.item.ReliquaryTooltips;
import com.summy.reliquary.text.ReliquaryFaction;
import com.summy.reliquary.text.SinTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 堕天：装入「殁亡之提喻」栏位的饰品（**需要恶魔标记**），恶魔线「凋亡」套系的第一件。
 *
 * <p>它开启整套核心机制（见 {@code effect/Apoptosis}）：近战直击为目标叠加**凋亡损伤**（每次 +1/6 条），
 * 满条**爆条**造成元素伤害，随后目标进入**损伤状态**（受伤附加元素伤害、治疗削减、条逐渐回退），
 * 结束后进入**损伤抵抗**。
 *
 * <p>门槛与提示排版都复用 SR 的现成实现（`CurioItemSupport` 的自定义门槛重载 + `ReliquaryTooltips`
 * 的恶魔门槛提示行），与天使线那五件是同款写法，只是门槛换成了恶魔标记。
 */
public class FallenHeavenItem extends Item implements ICurioItem {
	/** 暗红（恶魔线经文用色） */
	private static final int DARK_RED = 0xD86A6A;

	public FallenHeavenItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		// 恶魔门槛：第 4 个参数（天使标记）为 false，第 5 个参数换成恶魔标记谓词
		return CurioItemSupport.canEquipInto(slotContext, stack, ApocryphaSlots.PERDITION_SYNECDOCHE, false,
				PlayerFlags::isDemon, "需要恶魔标记");
	}

	@Override
	public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
		return CurioItemSupport.keepLines(tooltips);
	}

	@Override
	public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack) {
		return CurioItemSupport.keepLines(tooltips);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		ReliquaryTooltips.add(tooltip,
				Component.translatable("item.summy-reliquary-apocrypha.fallen_heaven.desc.1")
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(DARK_RED)).withItalic(true)));
		ReliquaryTooltips.add(tooltip,
				Component.translatable("item.summy-reliquary-apocrypha.fallen_heaven.desc.2")
						.withStyle(ChatFormatting.GRAY));

		// 没有恶魔标记：只显示名字 + 门槛提示行，不剧透功能
		if (ReliquaryTooltips.demonLocked()) {
			ReliquaryTooltips.appendDemonHint(tooltip);
			return;
		}

		if (ReliquaryTooltips.shiftDown()) {
			for (Component line : shiftLines()) {
				ReliquaryTooltips.add(tooltip, line);
			}
		} else {
			tooltip.add(ReliquaryTooltips.shiftHint());
		}
	}

	/** 功能描述行（数值全部取配置） */
	public static List<Component> shiftLines() {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.fallen_heaven.shift.1",
						ApocryphaConfig.apoptosisHitsBase(),
						ReliquaryTooltips.number(ApocryphaConfig.apoptosisBarMax()))
				.withStyle(ChatFormatting.GRAY));
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.DEMON,
				"item.summy-reliquary-apocrypha.fallen_heaven.shift.2",
				ReliquaryTooltips.number(ApocryphaConfig.apoptosisBurstDamage())));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.fallen_heaven.shift.3",
						ReliquaryTooltips.number(ApocryphaConfig.apoptosisExtraDamagePercent()),
						ReliquaryTooltips.number(ApocryphaConfig.apoptosisHealReductionPercent()),
						ReliquaryTooltips.number(ApocryphaConfig.apoptosisInjuredSeconds()))
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.fallen_heaven.shift.4",
						ReliquaryTooltips.number(ApocryphaConfig.apoptosisResistanceSeconds()))
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.fallen_heaven.shift.5")
				.withStyle(ChatFormatting.GRAY));
		return lines;
	}

	/** 物品名按派系上色（恶魔线 = 紫到暗红渐变，与 SR 的恶魔线饰品一致） */
	@Override
	public Component getName(ItemStack stack) {
		return SinTexts.factionName("item.summy-reliquary-apocrypha.fallen_heaven", ReliquaryFaction.DEMON);
	}
}
