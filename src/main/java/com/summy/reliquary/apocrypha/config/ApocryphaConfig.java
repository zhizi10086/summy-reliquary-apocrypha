package com.summy.reliquary.apocrypha.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 配置：config/summy_reliquary_apocrypha-common.toml（文件名由 Forge 的 modId 决定）。
 *
 * <p>按 SR 的习惯只提供配置文件、不做图形界面；改完文件后 Forge 会热加载，效果立即生效。
 */
public final class ApocryphaConfig {
	public static final ForgeConfigSpec SPEC;

	private static final ForgeConfigSpec.BooleanValue ENABLE;

	// ===== 新约 =====
	private static final ForgeConfigSpec.DoubleValue HEAL_AMOUNT;
	private static final ForgeConfigSpec.IntValue SCATTER_CHANCE_PERCENT;
	private static final ForgeConfigSpec.IntValue SCATTER_ARROW_MIN;
	private static final ForgeConfigSpec.IntValue SCATTER_ARROW_MAX;
	private static final ForgeConfigSpec.DoubleValue SCATTER_SPAWN_HEIGHT;
	private static final ForgeConfigSpec.DoubleValue SCATTER_ANGLE_MAX_DEGREES;
	private static final ForgeConfigSpec.DoubleValue SCATTER_INITIAL_SPEED;
	private static final ForgeConfigSpec.DoubleValue ARROW_DAMAGE;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		builder.comment("Summy Reliquary: Apocrypha 总开关").push("general");
		ENABLE = builder.comment("是否启用本模组的玩法效果（关掉后饰品仍可佩戴，但不再触发效果）")
				.define("enable", true);
		builder.pop();

		builder.comment("新约（饰品，装入「新约-光环」栏位；需要天使标记）").push("new_testament");
		HEAL_AMOUNT = builder.comment("弓箭命中目标后自身回复的生命值")
				.defineInRange("heal_amount", 0.5D, 0.0D, 100.0D);
		SCATTER_CHANCE_PERCENT = builder.comment("弓箭命中后触发「落箭散射」的概率（百分比）")
				.defineInRange("scatter_chance_percent", 35, 0, 100);
		SCATTER_ARROW_MIN = builder.comment("散射箭矢的最小数量")
				.defineInRange("scatter_arrow_min", 3, 1, 64);
		SCATTER_ARROW_MAX = builder.comment("散射箭矢的最大数量（小于最小值时按最小值处理）")
				.defineInRange("scatter_arrow_max", 5, 1, 64);
		SCATTER_SPAWN_HEIGHT = builder.comment("生成高度：目标碰撞箱顶部往上多少格")
				.defineInRange("scatter_spawn_height", 3.0D, 0.0D, 32.0D);
		SCATTER_ANGLE_MAX_DEGREES = builder
				.comment("散射角度上限（度）：竖直向下为 0°，每支箭在 0~该值之间随机偏转")
				.defineInRange("scatter_angle_max_degrees", 25.0D, 0.0D, 90.0D);
		SCATTER_INITIAL_SPEED = builder.comment("落箭的初速度（格/tick，之后按原版箭矢重力下落）")
				.defineInRange("scatter_initial_speed", 0.3D, 0.0D, 4.0D);
		ARROW_DAMAGE = builder
				.comment("落箭的固定伤害（真实伤害：无视护甲 / 保护附魔 / 抗性 / 无敌帧）")
				.defineInRange("arrow_damage", 5.0D, 0.0D, 1000.0D);
		builder.pop();

		SPEC = builder.build();
	}

	private ApocryphaConfig() {
	}

	/** 总开关 */
	public static boolean enable() {
		return ENABLE.get();
	}

	public static double healAmount() {
		return HEAL_AMOUNT.get();
	}

	public static int scatterChancePercent() {
		return SCATTER_CHANCE_PERCENT.get();
	}

	public static int scatterArrowMin() {
		return SCATTER_ARROW_MIN.get();
	}

	public static int scatterArrowMax() {
		return SCATTER_ARROW_MAX.get();
	}

	public static double scatterSpawnHeight() {
		return SCATTER_SPAWN_HEIGHT.get();
	}

	public static double scatterAngleMaxDegrees() {
		return SCATTER_ANGLE_MAX_DEGREES.get();
	}

	public static double scatterInitialSpeed() {
		return SCATTER_INITIAL_SPEED.get();
	}

	public static double arrowDamage() {
		return ARROW_DAMAGE.get();
	}
}
