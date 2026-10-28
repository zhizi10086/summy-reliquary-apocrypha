package com.summy.reliquary.apocrypha.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.world.level.Level;

/**
 * 「凋亡损伤条」在客户端的缓存（**纯数据**：只用到基本类型与 Map，服务端加载也无副作用）。
 *
 * <p>数据由 {@code ApoptosisBarMessage} 写入；之后客户端每 tick 本地衰减，因此服务端不必逐 tick 发包
 * （只在阶段切换与节流间隔上补发一次）。
 */
public final class ApoptosisBarState {
	/** 一条条状态：损伤值（0~barMax）、损伤状态剩余 tick、抵抗剩余 tick */
	public static final class Entry {
		public double value;
		public int injuredTicks;
		public int resistTicks;

		private Entry(double value, int injuredTicks, int resistTicks) {
			this.value = value;
			this.injuredTicks = injuredTicks;
			this.resistTicks = resistTicks;
		}
	}

	private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

	private ApoptosisBarState() {
	}

	/** 收到服务端同步：整条覆盖 */
	public static void accept(int entityId, float value, int injuredTicks, int resistTicks) {
		ENTRIES.put(entityId, new Entry(value, Math.max(0, injuredTicks), Math.max(0, resistTicks)));
	}

	/** 当前缓存（没有就返回 null） */
	public static Entry get(int entityId) {
		return ENTRIES.get(entityId);
	}

	/** 客户端每 tick：本地衰减（损伤状态期间条按同一速率回退），全部归零后移除 */
	public static void tick(Level level, double barMax, int injuredTotalTicks) {
		if (ENTRIES.isEmpty()) {
			return;
		}
		double drain = injuredTotalTicks > 0 ? barMax / injuredTotalTicks : 0.0D;
		Iterator<Map.Entry<Integer, Entry>> iterator = ENTRIES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Entry> mapEntry = iterator.next();
			// 目标卸载、死亡或换维度后不再存在于当前客户端世界，立即丢弃旧缓存。
			if (level == null || level.getEntity(mapEntry.getKey()) == null) {
				iterator.remove();
				continue;
			}
			Entry entry = mapEntry.getValue();
			if (entry.injuredTicks > 0) {
				entry.injuredTicks--;
				entry.value = Math.max(0.0D, entry.value - drain);
			} else if (entry.resistTicks > 0) {
				entry.resistTicks--;
			}
			if (entry.injuredTicks <= 0 && entry.resistTicks <= 0 && entry.value <= 0.0D) {
				iterator.remove();
			}
		}
	}

	/** 手动移除（例如目标消失） */
	public static void remove(int entityId) {
		ENTRIES.remove(entityId);
	}

	/** 退出世界 / 断开连接时清空 */
	public static void clear() {
		ENTRIES.clear();
	}
}
