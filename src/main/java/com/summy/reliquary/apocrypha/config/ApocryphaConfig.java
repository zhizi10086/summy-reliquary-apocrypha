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
	/** 是否给 Summy Reliquary 的真伤补「命中后清帧」 */
	private static final ForgeConfigSpec.BooleanValue CLEAR_SR_IFRAMES;

	// ===== 新约 =====
	private static final ForgeConfigSpec.DoubleValue HEAL_AMOUNT;
	private static final ForgeConfigSpec.IntValue SCATTER_CHANCE_PERCENT;
	private static final ForgeConfigSpec.IntValue SCATTER_ARROW_MIN;
	private static final ForgeConfigSpec.IntValue SCATTER_ARROW_MAX;
	private static final ForgeConfigSpec.DoubleValue SCATTER_SPAWN_HEIGHT;
	private static final ForgeConfigSpec.DoubleValue SCATTER_ANGLE_MAX_DEGREES;
	private static final ForgeConfigSpec.DoubleValue SCATTER_INITIAL_SPEED;
	private static final ForgeConfigSpec.DoubleValue ARROW_DAMAGE;
	private static final ForgeConfigSpec.BooleanValue LOG_TRIGGER;

	// ===== 迅羽·协约（新约系列 #2；只改写「新约」的落箭参数）=====
	private static final ForgeConfigSpec.IntValue SWIFT_CHANCE_PERCENT;
	private static final ForgeConfigSpec.IntValue SWIFT_SPEED_BONUS_PERCENT;
	private static final ForgeConfigSpec.IntValue SWIFT_DAMAGE_RATIO_PERCENT;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		builder.comment("Summy Reliquary: Apocrypha 总开关").push("general");
		ENABLE = builder.comment("是否启用本模组的玩法效果（关掉后饰品仍可佩戴，但不再触发效果）")
				.define("enable", true);
		CLEAR_SR_IFRAMES = builder
				.comment("是否给 Summy Reliquary 的「无视无敌帧」真伤补上「命中后清帧」",
						"开启后：这些真伤打完之后不会再给受害者留下无敌帧（同批多段真伤、以及随后的近战/箭矢都能全额落地）",
						"本项独立于上面的 enable：即使玩法效果全关，补强依然生效；关掉即恢复 SR 原本的行为")
				.define("clear_sr_iframes_after_hit", true);
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
		SCATTER_SPAWN_HEIGHT = builder
				.comment("生成高度：目标碰撞箱顶部往上多少格（越高落点铺得越开，但需要头顶有足够空间）")
				.defineInRange("scatter_spawn_height", 8.0D, 0.0D, 32.0D);
		SCATTER_ANGLE_MAX_DEGREES = builder
				.comment("散射角度上限（度）：竖直向下为 0°，每支箭在 0~该值之间随机偏转")
				.defineInRange("scatter_angle_max_degrees", 25.0D, 0.0D, 90.0D);
		SCATTER_INITIAL_SPEED = builder
				.comment("落箭的初速度（格/tick，之后按原版箭矢重力下落；越大横向铺开越快，但下落时间越短）")
				.defineInRange("scatter_initial_speed", 1.2D, 0.0D, 4.0D);
		ARROW_DAMAGE = builder
				.comment("落箭的固定伤害（真实伤害：无视护甲 / 保护附魔 / 抗性 / 无敌帧）")
				.defineInRange("arrow_damage", 5.0D, 0.0D, 1000.0D);
		LOG_TRIGGER = builder
				.comment("调试日志：开启后每次命中掷点、每批起点、每支落箭命中与每次清帧各记一行 INFO")
				.define("log_trigger", false);
		builder.pop();

		builder.comment("迅羽·协约（装入「新约-才能」栏位；仅在与「新约」同戴时改写其落箭参数）")
				.push("swift_feather_covenant");
		SWIFT_CHANCE_PERCENT = builder
				.comment("落箭触发概率（替换 [new_testament] scatter_chance_percent 的基础值）")
				.defineInRange("chance_percent", 50, 0, 100);
		SWIFT_SPEED_BONUS_PERCENT = builder
				.comment("落箭初速加成（百分比，叠加在 scatter_initial_speed 之上）")
				.defineInRange("speed_bonus_percent", 50, 0, 400);
		SWIFT_DAMAGE_RATIO_PERCENT = builder
				.comment("落箭伤害：触发之箭「各类倍率结算后」金额的百分比",
						"该值会被钉住 —— 落箭不再叠加 SR 的七罪/七德/最终倍率乘区")
				.defineInRange("damage_ratio_percent", 40, 0, 1000);
		builder.pop();

		SPEC = builder.build();
	}

	private ApocryphaConfig() {
	}

	/** 总开关 */
	public static boolean enable() {
		return ENABLE.get();
	}

	/** 是否给 SR 真伤补「命中后清帧」 */
	public static boolean clearSrIframesAfterHit() {
		return CLEAR_SR_IFRAMES.get();
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

	/** 调试日志开关 */
	public static boolean logTrigger() {
		return LOG_TRIGGER.get();
	}

	// ===== 迅羽·协约 =====

	/** 迅羽档的落箭触发概率（替换基础值） */
	public static int swiftChancePercent() {
		return SWIFT_CHANCE_PERCENT.get();
	}

	/** 迅羽档的落箭初速加成（百分比） */
	public static int swiftSpeedBonusPercent() {
		return SWIFT_SPEED_BONUS_PERCENT.get();
	}

	/** 迅羽档的落箭伤害系数（触发之箭金额的百分比，钉住不吃乘区） */
	public static int swiftDamageRatioPercent() {
		return SWIFT_DAMAGE_RATIO_PERCENT.get();
	}
}
