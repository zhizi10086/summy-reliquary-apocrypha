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
 * 翼羽·圣餐：装入「新约-恩典」栏位的饰品（需要天使标记）。
 *
 * <p>与「新约」同戴时：每次治疗触发在背后凝聚一对光羽（最多显示 3 对、实际可存 4 对）；
 * 攒到第 4 对后射出箭矢会消耗 1 对，使该箭成为**圣餐之矢**（伤害 +N%、必定触发新约、落箭固定若干支）；
 * 脱战若干秒后逐对熄灭。逻辑分别在 {@code effect/FeatherCharge} 与 {@code effect/SacramentArrows}。
 */
public class WingFeatherSacramentItem extends Item implements ICurioItem {
	/** 淡金（天使线经文用色） */
	private static final int PALE_GOLD = 0xFFE4B5;

	public WingFeatherSacramentItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		// 与新约同口径：需要天使标记（读档阶段一律放行，见 SR 的 CurioItemSupport）
		return CurioItemSupport.canEquipInto(slotContext, stack, ApocryphaSlots.NEW_TESTAMENT_GRACE, true);
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
				Component.translatable("item.summy-reliquary-apocrypha.wing_feather_sacrament.desc.1")
						.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(PALE_GOLD)).withItalic(true)));
		ReliquaryTooltips.add(tooltip,
				Component.translatable("item.summy-reliquary-apocrypha.wing_feather_sacrament.desc.2")
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

	/** 功能描述行（数值取配置；光羽上限等规则常量写在文案里） */
	public static List<Component> shiftLines() {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.wing_feather_sacrament.shift.1")
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.wing_feather_sacrament.shift.2")
				.withStyle(ChatFormatting.GRAY));
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.ANGEL,
				"item.summy-reliquary-apocrypha.wing_feather_sacrament.shift.3",
				ReliquaryTooltips.number(ApocryphaConfig.sacramentDamageBonusPercent())));
		lines.add(ReliquaryTooltips.statComponent(ReliquaryFaction.ANGEL,
				"item.summy-reliquary-apocrypha.wing_feather_sacrament.shift.4",
				ReliquaryTooltips.number(ApocryphaConfig.sacramentArrowCount())));
		lines.add(Component.translatable("item.summy-reliquary-apocrypha.wing_feather_sacrament.shift.5",
						ReliquaryTooltips.number(ApocryphaConfig.sacramentDecayDelaySeconds()),
						ReliquaryTooltips.number(ApocryphaConfig.sacramentDecayIntervalSeconds()))
				.withStyle(ChatFormatting.GRAY));
		return lines;
	}

	/** 物品名按派系上色（天使线 = 白到金渐变，与系列其它件一致） */
	@Override
	public Component getName(ItemStack stack) {
		return SinTexts.factionName("item.summy-reliquary-apocrypha.wing_feather_sacrament",
				ReliquaryFaction.ANGEL);
	}
}
