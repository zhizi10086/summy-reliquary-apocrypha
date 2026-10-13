package com.summy.reliquary.apocrypha.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.HashMap;
import java.util.Map;

/**
 * 提示行的「行内彩色术语」：语言文件里用 {@code {id|文本}} 标注特殊名词，这里把它拆成
 * 「灰色正文 + 带色术语」的兄弟组件。
 *
 * <p>为什么这么写：一句话里往往混着术语与数值，若按 SR 的 {@code 标签|值} 方式硬拆，会变成一堆
 * 只有译者能看懂的碎片键。用行内标记可以**每行只占一个语言键**（中英各一份），术语颜色集中在这张表里，
 * 以后要调色只改一个常量。
 *
 * <p>标记写法：{@code {state|损伤状态}}、{@code {value|6}}、{@code {element|元素伤害}}……
 * 未加标记的文字保持正文灰；标记在渲染前被解析掉，不会出现在游戏里。
 */
public final class ApocryphaText {
	/** 术语 id → 颜色（配色沿用头顶条的三段语义色，数值统一用 SR 的金色） */
	private static final Map<String, Integer> COLORS = new HashMap<>();

	static {
		COLORS.put("apoptosis", 0xD86A6A); // 凋亡损伤 / 凋亡：恶魔线暗红
		COLORS.put("state", 0xE0203A);     // 损伤状态：头顶条「损伤」亮红
		COLORS.put("burst", 0xE0203A);     // 损伤爆发：与损伤状态同色
		COLORS.put("resist", 0x8FA6B5);    // 损伤抵抗：青灰（提亮版，正文里可读）
		COLORS.put("element", 0xB44BE0);   // 元素伤害：紫
		COLORS.put("fear", 0x6FB3C8);      // 恐惧：青蓝
		COLORS.put("value", 0xFFD700);     // 数值 / 指令：金
	}

	private ApocryphaText() {
	}

	/** 正文颜色（未标记部分） */
	private static final ChatFormatting BASE = ChatFormatting.GRAY;

	/** 取一条语言文本、解析行内标记，返回可直接加进 tooltip 的组件 */
	public static MutableComponent line(String key, Object... args) {
		return parse(Component.translatable(key, args).getString());
	}

	/** 解析 {@code {id|文本}}：正文灰、术语按表着色；标记外的文字原样保留 */
	public static MutableComponent parse(String raw) {
		MutableComponent result = Component.empty();
		int index = 0;
		while (index < raw.length()) {
			int open = raw.indexOf('{', index);
			if (open < 0) {
				return result.append(plain(raw.substring(index)));
			}
			int close = raw.indexOf('}', open);
			int separator = raw.indexOf('|', open);
			if (close < 0 || separator < 0 || separator > close) {
				// 标记不完整：当作普通文字，避免把玩家的提示行吃掉
				return result.append(plain(raw.substring(index)));
			}
			if (open > index) {
				result.append(plain(raw.substring(index, open)));
			}
			String id = raw.substring(open + 1, separator).trim();
			String text = raw.substring(separator + 1, close);
			Integer color = COLORS.get(id);
			result.append(color == null
					? plain(text)
					: Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
			index = close + 1;
		}
		return result;
	}

	private static MutableComponent plain(String text) {
		return Component.literal(text).withStyle(BASE);
	}
}
