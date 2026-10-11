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

	// ===== 翼羽·圣餐（新约系列 #3）=====
	private static final ForgeConfigSpec.IntValue SACRAMENT_DECAY_DELAY_SECONDS;
	private static final ForgeConfigSpec.IntValue SACRAMENT_DECAY_INTERVAL_SECONDS;
	private static final ForgeConfigSpec.IntValue SACRAMENT_DAMAGE_BONUS_PERCENT;
	private static final ForgeConfigSpec.IntValue SACRAMENT_ARROW_COUNT;

	// ===== 逆羽·滞空（新约系列 #4）=====
	private static final ForgeConfigSpec.IntValue HOVER_FALLING_DAMAGE_PERCENT;
	private static final ForgeConfigSpec.IntValue HOVER_FALLING_CHANCE_PERCENT;
	private static final ForgeConfigSpec.IntValue HOVER_GLIDE_REDUCTION_PERCENT;
	private static final ForgeConfigSpec.IntValue HOVER_AIR_JUMPS;
	private static final ForgeConfigSpec.DoubleValue HOVER_AIR_JUMP_VELOCITY;

	// ===== 比翼·连携（新约系列 #5）=====
	private static final ForgeConfigSpec.IntValue PAIRED_HOLY_CHANCE_PERCENT;
	private static final ForgeConfigSpec.IntValue PAIRED_HOLY_DAMAGE_RATIO_PERCENT;

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

		builder.comment("翼羽·圣餐（装入「新约-恩典」栏位；与「新约」同戴时光羽凝聚、可化为圣餐之矢）")
				.push("wing_feather_sacrament");
		SACRAMENT_DECAY_DELAY_SECONDS = builder
				.comment("脱战多久（秒，自最后一次治疗触发起算）开始逐对熄灭光羽")
				.defineInRange("decay_delay_seconds", 10, 0, 600);
		SACRAMENT_DECAY_INTERVAL_SECONDS = builder
				.comment("开始熄灭后每隔多少秒熄灭 1 对")
				.defineInRange("decay_interval_seconds", 2, 1, 600);
		SACRAMENT_DAMAGE_BONUS_PERCENT = builder
				.comment("圣餐之矢的伤害加成（百分比；在 SR 乘区之前放大，因此后续增伤基于放大后的值）")
				.defineInRange("damage_bonus_percent", 30, 0, 1000);
		SACRAMENT_ARROW_COUNT = builder
				.comment("圣餐之矢的落箭数量（固定值，取代 3~5 的随机区间）")
				.defineInRange("arrow_count", 5, 1, 64);
		builder.pop();

		builder.comment("逆羽·滞空（装入「新约-祝福」栏位；下落状态强化新约，并被动提供缓降与额外跳跃）")
				.push("inverse_feather_hover");
		HOVER_FALLING_DAMAGE_PERCENT = builder
				.comment("处于下落状态时的箭矢伤害加成（百分比；与圣餐之矢的加成同轴相加）")
				.defineInRange("falling_damage_bonus_percent", 15, 0, 1000);
		HOVER_FALLING_CHANCE_PERCENT = builder
				.comment("处于下落状态时的新约触发概率倍率（百分比；150 = 生效基础值 ×1.5）")
				.defineInRange("falling_chance_percent", 150, 0, 1000);
		HOVER_GLIDE_REDUCTION_PERCENT = builder
				.comment("缓降：空中长按潜行键时下落速度与坠落高度的折减（百分比；75 = 只按 25% 计）")
				.defineInRange("glide_reduction_percent", 75, 0, 100);
		HOVER_AIR_JUMPS = builder
				.comment("每段离地可用的额外跳跃次数（空中按跳跃键触发，落地重置）")
				.defineInRange("air_jumps", 1, 0, 8);
		HOVER_AIR_JUMP_VELOCITY = builder
				.comment("空中额外跳跃的向上初速（格/tick；原版起跳是 0.42，0.5 约可再抬升 1.5 格）")
				.defineInRange("air_jump_velocity", 0.5D, 0.0D, 4.0D);
		builder.pop();

		builder.comment("比翼·连携（装入「新约-启示」栏位；追加穿透圣光矢，并与「新约」的落箭协同触发）")
				.push("paired_wings");
		PAIRED_HOLY_CHANCE_PERCENT = builder
				.comment("弓箭命中时追加一支穿透圣光矢的概率（百分比）")
				.defineInRange("holy_arrow_chance_percent", 20, 0, 100);
		PAIRED_HOLY_DAMAGE_RATIO_PERCENT = builder
				.comment("圣光矢的伤害比例（百分比）：伤害 = 触发之箭「各类倍率后」的金额 × 该比例",
						"该值会被钉住 —— 圣光矢命中时不再叠加 Summy Reliquary 的增伤乘区")
				.defineInRange("holy_arrow_damage_ratio_percent", 100, 0, 1000);
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

	// ===== 翼羽·圣餐 =====

	/** 脱战多久开始熄灭光羽（秒） */
	public static int sacramentDecayDelaySeconds() {
		return SACRAMENT_DECAY_DELAY_SECONDS.get();
	}

	/** 熄灭节奏：每隔多少秒熄 1 对 */
	public static int sacramentDecayIntervalSeconds() {
		return SACRAMENT_DECAY_INTERVAL_SECONDS.get();
	}

	/** 圣餐之矢的伤害加成（百分比） */
	public static int sacramentDamageBonusPercent() {
		return SACRAMENT_DAMAGE_BONUS_PERCENT.get();
	}

	/** 圣餐之矢的落箭数量（固定值） */
	public static int sacramentArrowCount() {
		return SACRAMENT_ARROW_COUNT.get();
	}

	// ===== 逆羽·滞空 =====

	/** 下落状态下的箭矢伤害加成（百分比） */
	public static int hoverFallingDamageBonusPercent() {
		return HOVER_FALLING_DAMAGE_PERCENT.get();
	}

	/** 下落状态下的触发概率倍率（百分比，150 = ×1.5） */
	public static int hoverFallingChancePercent() {
		return HOVER_FALLING_CHANCE_PERCENT.get();
	}

	/** 缓降的折减比例（百分比：75 = 下落速度与坠落高度只按 25% 计） */
	public static int hoverGlideReductionPercent() {
		return HOVER_GLIDE_REDUCTION_PERCENT.get();
	}

	/** 每段离地可用的额外跳跃次数 */
	public static int hoverAirJumps() {
		return HOVER_AIR_JUMPS.get();
	}

	/** 空中额外跳跃的向上初速（格/tick） */
	public static double hoverAirJumpVelocity() {
		return HOVER_AIR_JUMP_VELOCITY.get();
	}

	// ===== 比翼·连携 =====

	/** 追加穿透圣光矢的概率（百分比） */
	public static int holyArrowChancePercent() {
		return PAIRED_HOLY_CHANCE_PERCENT.get();
	}

	/** 圣光矢的伤害比例（百分比，相对触发之箭「各类倍率后」的金额） */
	public static int holyArrowDamageRatioPercent() {
		return PAIRED_HOLY_DAMAGE_RATIO_PERCENT.get();
	}
}
