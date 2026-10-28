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

import java.util.ArrayList;
import java.util.List;

/**
 * 坠羽：装入「殁亡之释经」栏位的饰品（**需要恶魔标记**），恶魔线「凋亡」套系的第二件。
 *
 * <p>它是「堕天」的强化档：凋亡损伤只需 **4 刀**就爆条，且爆条伤害不再是固定值，而是
 * 「本轮推条那 4 刀伤害之和 × 60%」；损伤状态期间附加的元素伤害提升到 **20%**；
 * 爆条瞬间还会在佩戴者背后展开一对翅膀（5 秒，再次爆条刷新）。
 *
 * <p>两件同戴时按本件（坠羽）的口径结算，见 {@code effect/Apoptosis}。
 */
public class FallenPlumeItem extends Item implements ApocryphaKeptCurioItem {
	/** 暗红（恶魔线经文用色） */
	private static final int DARK_RED = 0xD86A6A;

	public FallenPlumeItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		// 恶魔门槛：第 4 个参数（天使标记）为 false，第 5 个参数换成恶魔标记谓词
		return CurioItemSupport.canEquipInto(slotContext, stack, ApocryphaSlots.PERDITION_EXEGESIS, false,
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
				Component.translatable("item.summy-reliquary-apocrypha.fallen_plume.desc.1")
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(DARK_RED)).withItalic(true)));
		ReliquaryTooltips.add(tooltip,
				Component.translatable("item.summy-reliquary-apocrypha.fallen_plume.desc.2")
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
		lines.add(ApocryphaText.line("item.summy-reliquary-apocrypha.fallen_plume.shift.1",
				ApocryphaConfig.apoptosisHitsWithSecond()));
		lines.add(ApocryphaText.line("item.summy-reliquary-apocrypha.fallen_plume.shift.2",
				ReliquaryTooltips.number(ApocryphaConfig.apoptosisBurstRatioWithSecondPercent())));
		lines.add(ApocryphaText.line("item.summy-reliquary-apocrypha.fallen_plume.shift.3",
				ReliquaryTooltips.number(ApocryphaConfig.apoptosisExtraDamagePercentWithSecond())));
		lines.add(ApocryphaText.line("item.summy-reliquary-apocrypha.fallen_plume.shift.4",
				ReliquaryTooltips.number(ApocryphaConfig.apoptosisWingSeconds())));
		return lines;
	}

	/** 物品名按派系上色（恶魔线 = 紫到暗红渐变，与 SR 的恶魔线饰品一致） */
	@Override
	public Component getName(ItemStack stack) {
		return SinTexts.factionName("item.summy-reliquary-apocrypha.fallen_plume", ReliquaryFaction.DEMON);
	}
}
