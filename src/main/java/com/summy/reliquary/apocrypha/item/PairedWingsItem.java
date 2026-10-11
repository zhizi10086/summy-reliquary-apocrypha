package com.summy.reliquary.apocrypha.item;

import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.effect.PairedWings;
import com.summy.reliquary.apocrypha.slot.ApocryphaSlots;
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
 * 比翼·连携：装入「新约-启示」栏位的饰品（需要天使标记）。
 *
 * <p>弓箭命中时按配置概率追加一支**穿透圣光矢**；与「新约」同戴时两者永远协同触发
 * （任一自然触发都带出另一个，两次判定都成功则各 +1）。逻辑分别在 {@code effect/PairedWings}
 * 与 {@code effect/NewTestamentEffect}，投射物见 {@code entity/HolyArrow}。
 *
 * <p><b>本模组的「圣光矢」与 Summy Reliquary 的「圣光」无关</b>，是本模组自建的投射物与伤害类型。
 */
public class PairedWingsItem extends Item implements ICurioItem {
	/** 淡金（天使线经文用色） */
	private static final int PALE_GOLD = 0xFFE4B5;

	public PairedWingsItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		// 与新约同口径：需要天使标记（读档阶段一律放行，见 SR 的 CurioItemSupport）
		return CurioItemSupport.canEquipInto(slotContext, stack, ApocryphaSlots.NEW_TESTAMENT_REVELATION, true);
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
				Component.translatable("item.summy-reliquary-apocrypha.paired_wings.desc.1")
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(PALE_GOLD)).withItalic(true)));
		ReliquaryTooltips.add(tooltip,
				Component.translatable("item.summy-reliquary-apocrypha.paired_wings.desc.2")
						.withStyle(ChatFormatting.GRAY));

		// 缺天使标记：只显示名字 + 门槛提示行，不剧透功能
		if (ReliquaryTooltips.angelLocked()) {
			ReliquaryTooltips.appendAngelHint(tooltip);
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
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.ANGEL,
				"item.summy-reliquary-apocrypha.paired_wings.shift.1",
				ReliquaryTooltips.number(ApocryphaConfig.holyArrowChancePercent())));
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.ANGEL,
				"item.summy-reliquary-apocrypha.paired_wings.shift.2",
				ReliquaryTooltips.number(ApocryphaConfig.holyArrowDamageBonus())));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.paired_wings.shift.3")
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.paired_wings.shift.4",
						ReliquaryTooltips.number(PairedWings.COUPLED_DELAY_TICKS / 20.0D))
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.paired_wings.shift.5",
						PairedWings.COUPLED_BONUS_ARROWS)
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.paired_wings.shift.6")
				.withStyle(ChatFormatting.GRAY));
		return lines;
	}

	/** 物品名按派系上色（天使线 = 白到金渐变，与系列其它件一致） */
	@Override
	public Component getName(ItemStack stack) {
		return SinTexts.factionName("item.summy-reliquary-apocrypha.paired_wings", ReliquaryFaction.ANGEL);
	}
}
