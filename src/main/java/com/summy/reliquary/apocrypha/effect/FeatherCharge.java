package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.entity.LightFeather;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

// 「光羽」的计数、脱战衰减与实体维护（服务端）。
//
// 规则（与设计一致）：
//   每次「新约治疗触发」+1 对（2 片）；存储上限 8 片（4 对）；
//   只有攒满 6 片（3 对）时背后才出现那一对光翼（1、2 对时完全不显示），跌破门槛立刻消失；
//   只有片数 > 6（攒到第 4 对）才允许射击时消耗 1 对；脱战（自最后一次治疗触发起）满 N 秒后每 M 秒熄 1 对。
// 计数写在玩家 Forge 持久数据（随存档保存，键根 summy_reliquary_apocrypha），与 SR 的根键互不干扰。
public final class FeatherCharge {
	/** 本模组在玩家持久数据里的根键 */
	private static final String ROOT = "summy_reliquary_apocrypha";
	private static final String CHARGE = "feather_charge";
	/** 脱战计时（秒）：>0 时每秒递减，归零时熄一对并按间隔续上 */
	private static final String DECAY_TIMER = "feather_decay_timer";

	/** 1 对 = 2 片 */
	public static final int PER_PAIR = 2;
	/** 显示上限（片）：6 片 = 3 对 */
	public static final int DISPLAY_CAP = 6;
	/** 存储上限（片）：8 片 = 4 对 */
	public static final int STORE_CAP = 8;
	/** 消耗门槛：片数必须 > 6（即攒到第 4 对）才允许消耗 */
	public static final int CONSUME_THRESHOLD = 6;
	/** 光翼的显现门槛（片）：攒满 3 对（6 片）时背后才出现那一对翅膀 */
	public static final int WING_GATE_CHARGE = DISPLAY_CAP;
	/** 唯一那对翅膀使用的层号（中层锚点：背后 0.35 格、高度 1.05 格） */
	private static final int WING_TIER = 1;

	private static final int TICK_INTERVAL = 20;

	private FeatherCharge() {
	}

	private static CompoundTag root(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		if (!data.contains(ROOT, CompoundTag.TAG_COMPOUND)) {
			data.put(ROOT, new CompoundTag());
		}
		return data.getCompound(ROOT);
	}

	/** 当前光羽片数（0~8） */
	public static int charge(ServerPlayer player) {
		return root(player).getInt(CHARGE);
	}

	private static void setCharge(ServerPlayer player, int value) {
		root(player).putInt(CHARGE, Math.max(0, Math.min(STORE_CAP, value)));
	}

	/** 是否已达到「可消耗」门槛（片数 > 6，即攒到第 4 对） */
	public static boolean canConsume(ServerPlayer player) {
		return charge(player) > CONSUME_THRESHOLD;
	}

	/** 新约治疗触发：+1 对，并把脱战计时重置 */
	public static void addPair(ServerPlayer player) {
		setCharge(player, charge(player) + PER_PAIR);
		root(player).putInt(DECAY_TIMER, ApocryphaConfig.sacramentDecayDelaySeconds());
		// 立刻把新的一对实体补上：否则要等每秒兜底扫描，表现为"过一会儿才冒出来"
		syncNow(player);
	}

	/** 消耗 1 对（射击 / 熄灭共用） */
	public static void consumePair(ServerPlayer player) {
		setCharge(player, charge(player) - PER_PAIR);
		// 立刻收掉多出来的那一对（射击消耗与脱战熄灭都要即时反馈）
		syncNow(player);
	}

	/**
	 * 此刻应当显示的光翼对数：**0 或 1**。
	 *
	 * <p>未佩戴饰品时恒为 0（计数保留、只是不显示）；即使佩戴，也要攒满 {@link #WING_GATE_CHARGE}
	 * （3 对 / 6 片）才显现 —— 1、2 对时背后什么都看不到。
	 */
	public static int visibleWings(ServerPlayer player) {
		if (!CurioHelper.wears(player, Apocrypha.WING_FEATHER_SACRAMENT.get())) {
			return 0;
		}
		return charge(player) >= WING_GATE_CHARGE ? 1 : 0;
	}

	/** 服务端每秒：推进衰减 + 校正实体 */
	public static void tickServer(MinecraftServer server) {
		if (server.getTickCount() % TICK_INTERVAL != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickDecay(player);
			syncEntities(player);
		}
	}

	// 脱战衰减：计时归零时熄一对；若还有剩余，按配置间隔续上
	private static void tickDecay(ServerPlayer player) {
		if (charge(player) <= 0) {
			root(player).putInt(DECAY_TIMER, 0);
			return;
		}
		int timer = root(player).getInt(DECAY_TIMER);
		if (timer <= 0) {
			return;
		}
		timer--;
		if (timer > 0) {
			root(player).putInt(DECAY_TIMER, timer);
			return;
		}
		consumePair(player);
		root(player).putInt(DECAY_TIMER,
				charge(player) > 0 ? ApocryphaConfig.sacramentDecayIntervalSeconds() : 0);
	}

	// 应有光翼 = 0 或 1：没到门槛就把已有的收掉，到门槛就补上（只有一对，层号固定为中层）
	private static void syncEntities(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		int want = visibleWings(player);
		List<LightFeather> existing = level.getEntitiesOfClass(LightFeather.class,
				player.getBoundingBox().inflate(8.0D),
				feather -> player.getUUID().equals(feather.ownerId()));
		if (want <= 0) {
			for (LightFeather feather : existing) {
				feather.discard();
			}
			return;
		}
		if (existing.stream().noneMatch(LightFeather::isAlive)) {
			level.addFreshEntity(new LightFeather(level, player, WING_TIER));
		}
	}

	/** 立即校正（装备 / 卸下 / 指令等需要立刻反馈的场合） */
	public static void syncNow(ServerPlayer player) {
		syncEntities(player);
	}
}
