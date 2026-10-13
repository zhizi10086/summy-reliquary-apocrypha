package com.summy.reliquary.apocrypha.item;

import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.slot.ApocryphaSlots;
import com.summy.reliquary.apocrypha.text.ApocryphaText;
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
 * 枯笔：装入「殁亡之悲悯」栏位的饰品（**需要恶魔标记**），恶魔线「凋亡」套系的第四件。
 *
 * <p>两个效果：① **近战直击「损伤抵抗中」的目标 → 它的抵抗时间每次减少若干秒**（减到 0 抵抗立刻结束）；
 * ② 佩戴者引爆时，**目标获得一小段恐惧**（无法移动 + 无法攻击；玩家档默认关闭，
 * 可用指令 {@code /summyapocrypha fear players <on|off>} 打开）。
 */
public class WitheredBrushItem extends Item implements ICurioItem {
	/** 暗红（恶魔线经文用色） */
	private static final int DARK_RED = 0xD86A6A;

	public WitheredBrushItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		// 恶魔门槛：第 4 个参数（天使标记）为 false，第 5 个参数换成恶魔标记谓词
		return CurioItemSupport.canEquipInto(slotContext, stack, ApocryphaSlots.PERDITION_MERCY, false,
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
				Component.translatable("item.summy-reliquary-apocrypha.withered_brush.desc.1")
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(DARK_RED)).withItalic(true)));
		ReliquaryTooltips.add(tooltip,
				Component.translatable("item.summy-reliquary-apocrypha.withered_brush.desc.2")
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
		lines.add(ApocryphaText.line("item.summy-reliquary-apocrypha.withered_brush.shift.1",
				ReliquaryTooltips.number(ApocryphaConfig.apoptosisResistanceCutSecondsWithFourth())));
		lines.add(ApocryphaText.line("item.summy-reliquary-apocrypha.withered_brush.shift.2",
				ReliquaryTooltips.number(ApocryphaConfig.apoptosisFearSecondsWithFourth())));
		lines.add(ApocryphaText.line("item.summy-reliquary-apocrypha.withered_brush.shift.3"));
		return lines;
	}

	/** 物品名按派系上色（恶魔线 = 紫到暗红渐变，与 SR 的恶魔线饰品一致） */
	@Override
	public Component getName(ItemStack stack) {
		return SinTexts.factionName("item.summy-reliquary-apocrypha.withered_brush", ReliquaryFaction.DEMON);
	}
}
