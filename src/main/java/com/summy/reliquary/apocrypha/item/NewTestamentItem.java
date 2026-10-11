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
 * 新约：装入「新约-光环」栏位的饰品（需要天使标记）。
 *
 * <p>门槛与提示格式完全复用 SR 的现成实现：
 * <ul>
 *     <li>CurioItemSupport#canEquipInto —— 与 SR 的圣光 / 圣心同口径：缺标记不让装，
 *     并且自带「读档阶段 / 登录重算放行」「同种物品不可重复佩戴」这些 SR 已经踩过坑的处理；</li>
 *     <li>ReliquaryTooltips —— 与 SR 饰品一致的提示排版（Shift 展开、天使门槛提示行）。</li>
 * </ul>
 *
 * <p>玩法逻辑在 effect/NewTestamentEffect（命中判定 + 回血 + 散射 + 音效），
 * 落箭实体见 entity/NewTestamentArrow。
 */
public class NewTestamentItem extends Item implements ICurioItem {
	/** 淡金（天使线经文用色） */
	private static final int PALE_GOLD = 0xFFE4B5;

	public NewTestamentItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		// 第 4 个参数 = 需要天使标记（读档阶段一律放行，见 SR 的 CurioItemSupport）
		return CurioItemSupport.canEquipInto(slotContext, stack, ApocryphaSlots.NEW_TESTAMENT_HALO, true);
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
		ReliquaryTooltips.add(tooltip, Component.translatable("item.summy-reliquary-apocrypha.new_testament.desc.1")
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(PALE_GOLD)).withItalic(true)));
		ReliquaryTooltips.add(tooltip, Component.translatable("item.summy-reliquary-apocrypha.new_testament.desc.2")
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
		int min = Math.min(ApocryphaConfig.scatterArrowMin(), ApocryphaConfig.scatterArrowMax());
		int max = Math.max(ApocryphaConfig.scatterArrowMin(), ApocryphaConfig.scatterArrowMax());

		List<Component> lines = new ArrayList<>();
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.ANGEL,
				"item.summy-reliquary-apocrypha.new_testament.shift.1",
				ReliquaryTooltips.number(ApocryphaConfig.healAmount())));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.new_testament.shift.2",
						ReliquaryTooltips.number(ApocryphaConfig.scatterChancePercent()),
						ReliquaryTooltips.number(ApocryphaConfig.scatterSpawnHeight()),
						min, max,
						ReliquaryTooltips.number(ApocryphaConfig.scatterAngleMaxDegrees()))
				.withStyle(ChatFormatting.GRAY));
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.ANGEL,
				"item.summy-reliquary-apocrypha.new_testament.shift.3",
				ReliquaryTooltips.number(ApocryphaConfig.arrowDamage())));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.new_testament.shift.4")
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.new_testament.shift.5")
				.withStyle(ChatFormatting.GRAY));
		return lines;
	}

	/** 物品名按派系上色（天使线 = 白到金渐变，与 SR 的天使线饰品一致） */
	@Override
	public Component getName(ItemStack stack) {
		return SinTexts.factionName("item.summy-reliquary-apocrypha.new_testament", ReliquaryFaction.ANGEL);
	}
}
