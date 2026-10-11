package com.summy.reliquary.apocrypha.item;

import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
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
 * 逆羽·滞空：装入「新约-祝福」栏位的饰品（需要天使标记）。
 *
 * <p>处于下落状态时强化新约（箭矢伤害 +N%、触发概率 ×N、落箭伤害改为瞬时结算），
 * 并被动提供缓降（空中长按潜行键）与额外跳跃（空中按跳跃键，经网络包上报）。
 * 逻辑分别在 {@code effect/HoverFlight}、{@code effect/ArrowDamageBonuses} 与 {@code effect/NewTestamentEffect}。
 */
public class InverseFeatherHoverItem extends Item implements ICurioItem {
	/** 淡金（天使线经文用色） */
	private static final int PALE_GOLD = 0xFFE4B5;

	public InverseFeatherHoverItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		// 与新约同口径：需要天使标记（读档阶段一律放行，见 SR 的 CurioItemSupport）
		return CurioItemSupport.canEquipInto(slotContext, stack, ApocryphaSlots.NEW_TESTAMENT_BLESSING, true);
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
				Component.translatable("item.summy-reliquary-apocrypha.inverse_feather_hover.desc.1")
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(PALE_GOLD)).withItalic(true)));
		ReliquaryTooltips.add(tooltip,
				Component.translatable("item.summy-reliquary-apocrypha.inverse_feather_hover.desc.2")
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
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.inverse_feather_hover.shift.1")
				.withStyle(ChatFormatting.GRAY));
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.ANGEL,
				"item.summy-reliquary-apocrypha.inverse_feather_hover.shift.2",
				ReliquaryTooltips.number(ApocryphaConfig.hoverFallingDamageBonusPercent())));
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.ANGEL,
				"item.summy-reliquary-apocrypha.inverse_feather_hover.shift.3",
				ReliquaryTooltips.number(ApocryphaConfig.hoverFallingChancePercent())));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.inverse_feather_hover.shift.4")
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.inverse_feather_hover.shift.5",
						ReliquaryTooltips.number(ApocryphaConfig.hoverGlideReductionPercent()),
						ReliquaryTooltips.number(ApocryphaConfig.hoverGlideReductionPercent()),
						ReliquaryTooltips.number(ApocryphaConfig.hoverAirJumps()))
				.withStyle(ChatFormatting.GRAY));
		return lines;
	}

	/** 物品名按派系上色（天使线 = 白到金渐变，与系列其它件一致） */
	@Override
	public Component getName(ItemStack stack) {
		return SinTexts.factionName("item.summy-reliquary-apocrypha.inverse_feather_hover",
				ReliquaryFaction.ANGEL);
	}
}
