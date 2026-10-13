package com.summy.reliquary.apocrypha.item;

import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.slot.ApocryphaSlots;
import com.summy.reliquary.apocrypha.text.ApocryphaText;
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
 * 迅羽·协约：装入「新约-才能」栏位的饰品（需要天使标记）。
 *
 * <p>本身不提供任何属性，只在**与「新约」同戴**时改写后者落箭的三项参数：
 * 触发概率 → {@code [swift_feather_covenant] chance_percent}；
 * 落箭初速 ×(1 + {@code speed_bonus_percent}%)；
 * 落箭伤害 = 触发之箭（在 LivingHurtEvent 里）金额的 {@code damage_ratio_percent}%，
 * 且该值**钉住**、不再叠加 SR 的增伤乘区。改写逻辑在 {@code effect/NewTestamentEffect} 的命中分支里。
 */
public class SwiftFeatherCovenantItem extends Item implements ICurioItem {
	/** 淡金（天使线经文用色） */
	private static final int PALE_GOLD = 0xFFE4B5;

	public SwiftFeatherCovenantItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		// 与新约同口径：需要天使标记（读档阶段一律放行，见 SR 的 CurioItemSupport）
		return CurioItemSupport.canEquipInto(slotContext, stack, ApocryphaSlots.NEW_TESTAMENT_TALENT, true);
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
				Component.translatable("item.summy-reliquary-apocrypha.swift_feather_covenant.desc.1")
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(PALE_GOLD)).withItalic(true)));
		ReliquaryTooltips.add(tooltip,
				Component.translatable("item.summy-reliquary-apocrypha.swift_feather_covenant.desc.2")
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

	/** 功能描述行（数值全部取配置，改配置后提示同步变化） */
	public static List<Component> shiftLines() {
		List<Component> lines = new ArrayList<>();
		lines.add(ApocryphaText.line("item.summy-reliquary-apocrypha.swift_feather_covenant.shift.1",
				ReliquaryTooltips.number(ApocryphaConfig.swiftChancePercent())));
		lines.add(ApocryphaText.line("item.summy-reliquary-apocrypha.swift_feather_covenant.shift.2",
				ReliquaryTooltips.number(ApocryphaConfig.swiftSpeedBonusPercent())));
		lines.add(ApocryphaText.line("item.summy-reliquary-apocrypha.swift_feather_covenant.shift.3",
				ReliquaryTooltips.number(ApocryphaConfig.swiftDamageRatioPercent())));
		return lines;
	}

	/** 物品名按派系上色（天使线 = 白到金渐变，与新约一致） */
	@Override
	public Component getName(ItemStack stack) {
		return SinTexts.factionName("item.summy-reliquary-apocrypha.swift_feather_covenant",
				ReliquaryFaction.ANGEL);
	}
}
