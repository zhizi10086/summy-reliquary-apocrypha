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
 * 笞心：装入「殁亡之弥撒」栏位的饰品（**需要恶魔标记**），恶魔线「凋亡」套系的第三件。
 *
 * <p>它同时做两件事：① 放大整套的数值 —— 由佩戴者引爆时，**最终爆伤与损伤状态的附加比例各 ×1.5**；
 * ② 给本段损伤状态装上**斩杀** —— 目标受伤后（本次伤害结算完附加伤害再判）若生命值 ≤ 最大生命的 15%，
 * 就补上一击「最大生命 × 100%」的元素伤害，**每段损伤状态只落地一次**，且只有
 * **「本次伤害的造成者是佩戴笞心的玩家」**时才激活这个判断（见 {@code effect/Apoptosis#tryExecute}）。
 */
public class ScourgedHeartItem extends Item implements ICurioItem {
	/** 暗红（恶魔线经文用色） */
	private static final int DARK_RED = 0xD86A6A;

	public ScourgedHeartItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		// 恶魔门槛：第 4 个参数（天使标记）为 false，第 5 个参数换成恶魔标记谓词
		return CurioItemSupport.canEquipInto(slotContext, stack, ApocryphaSlots.PERDITION_MASS, false,
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
				Component.translatable("item.summy-reliquary-apocrypha.scourged_heart.desc.1")
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(DARK_RED)).withItalic(true)));
		ReliquaryTooltips.add(tooltip,
				Component.translatable("item.summy-reliquary-apocrypha.scourged_heart.desc.2")
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
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.DEMON,
				"item.summy-reliquary-apocrypha.scourged_heart.shift.1",
				ReliquaryTooltips.number(ApocryphaConfig.apoptosisBurstMultiplierWithThird() * 100.0D)));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.scourged_heart.shift.2",
						ReliquaryTooltips.number(ApocryphaConfig.apoptosisExtraMultiplierWithThird()))
				.withStyle(ChatFormatting.GRAY));
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.DEMON,
				"item.summy-reliquary-apocrypha.scourged_heart.shift.3",
				ReliquaryTooltips.number(ApocryphaConfig.apoptosisExecuteThresholdPercent())));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.scourged_heart.shift.4",
						ReliquaryTooltips.number(ApocryphaConfig.apoptosisExecuteDamagePercentOfMax()))
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.scourged_heart.shift.5")
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.scourged_heart.shift.6")
				.withStyle(ChatFormatting.GRAY));
		return lines;
	}

	/** 物品名按派系上色（恶魔线 = 紫到暗红渐变，与 SR 的恶魔线饰品一致） */
	@Override
	public Component getName(ItemStack stack) {
		return SinTexts.factionName("item.summy-reliquary-apocrypha.scourged_heart", ReliquaryFaction.DEMON);
	}
}
