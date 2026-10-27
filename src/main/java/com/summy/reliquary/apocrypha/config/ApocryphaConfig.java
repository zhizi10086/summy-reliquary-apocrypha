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
	private static final ForgeConfigSpec.IntValue ACQUISITION_CHANCE_PERCENT;
	private static final ForgeConfigSpec.IntValue ACQUISITION_PITY_CHESTS;

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

	// ===== 凋亡（恶魔线 #1：核心机制 + 堕天）=====
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_BAR_MAX;
	private static final ForgeConfigSpec.IntValue APOPTOSIS_HITS_BASE;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_BURST_DAMAGE;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_EXTRA_DAMAGE_PERCENT;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_HEAL_REDUCTION_PERCENT;
	private static final ForgeConfigSpec.IntValue APOPTOSIS_INJURED_SECONDS;
	private static final ForgeConfigSpec.IntValue APOPTOSIS_RESISTANCE_SECONDS;
	private static final ForgeConfigSpec.IntValue APOPTOSIS_HITS_WITH_SECOND;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_BURST_RATIO_WITH_SECOND_PERCENT;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_EXTRA_DAMAGE_PERCENT_WITH_SECOND;
	private static final ForgeConfigSpec.IntValue APOPTOSIS_WING_SECONDS;
	private static final ForgeConfigSpec.IntValue APOPTOSIS_IDLE_CLEAR_SECONDS;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_EXECUTE_THRESHOLD_PERCENT;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_EXECUTE_DAMAGE_PERCENT_OF_MAX;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_BURST_MULTIPLIER_WITH_THIRD;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_EXTRA_MULTIPLIER_WITH_THIRD;
	private static final ForgeConfigSpec.IntValue APOPTOSIS_RESISTANCE_CUT_SECONDS_WITH_FOURTH;
	private static final ForgeConfigSpec.IntValue APOPTOSIS_FEAR_SECONDS_WITH_FOURTH;
	private static final ForgeConfigSpec.BooleanValue APOPTOSIS_FEAR_AFFECTS_PLAYERS;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_BURST_PERCENT_PER_SECOND_WITH_FIFTH;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_HEAL_REDUCTION_PERCENT_WITH_FIFTH;
	private static final ForgeConfigSpec.DoubleValue APOPTOSIS_DAMAGE_TAKEN_BONUS_PERCENT_WITH_FIFTH;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		builder.comment("Summy Reliquary: Apocrypha 总开关").push("general");
		ENABLE = builder.comment("是否启用本模组的玩法效果（关掉后饰品仍可佩戴，但不再触发效果）")
				.define("enable", true);
		builder.pop();

		builder.comment("两套饰品的野箱获取规则").push("acquisition");
		ACQUISITION_CHANCE_PERCENT = builder
				.comment("佩戴基础件后，每个合格野箱获得后四件之一的概率（百分比）")
				.defineInRange("subsequent_chance_percent", 15, 0, 100);
		ACQUISITION_PITY_CHESTS = builder
				.comment("连续多少个合格野箱未出货后，下一个箱子必得；0 = 关闭保底")
				.defineInRange("pity_chests", 10, 0, 1000);
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

		builder.comment("凋亡（恶魔线套系；装入「殁亡之提喻」栏位的「堕天」开启核心机制）").push("apoptosis");
		APOPTOSIS_BAR_MAX = builder
				.comment("凋亡损伤条的总点数（满值即触发爆条）")
				.defineInRange("bar_max", 100.0D, 1.0D, 10000.0D);
		APOPTOSIS_HITS_BASE = builder
				.comment("未佩戴 2 号件（坠羽）时，打满损伤条需要的近战次数：每次攻击推 bar_max / 该值",
						"（2 号件那轮会再加一个键，把该值改成 4）")
				.defineInRange("hits_base", 6, 1, 100);
		APOPTOSIS_BURST_DAMAGE = builder
				.comment("损伤爆发的元素伤害（点；元素伤害无视护甲/保护/抗性，且不吃增伤乘区）")
				.defineInRange("burst_damage", 30.0D, 0.0D, 10000.0D);
		APOPTOSIS_EXTRA_DAMAGE_PERCENT = builder
				.comment("损伤状态期间，目标每次受伤额外附加的元素伤害（百分比，按该次伤害计算）")
				.defineInRange("extra_damage_percent", 10.0D, 0.0D, 1000.0D);
		APOPTOSIS_HEAL_REDUCTION_PERCENT = builder
				.comment("损伤状态期间目标的治疗削减（百分比，静默缩放 LivingHealEvent）")
				.defineInRange("heal_reduction_percent", 50.0D, 0.0D, 100.0D);
		APOPTOSIS_INJURED_SECONDS = builder
				.comment("损伤状态持续秒数（条在这个时间内从满线性回退到 0）")
				.defineInRange("injured_seconds", 5, 1, 600);
		APOPTOSIS_RESISTANCE_SECONDS = builder
				.comment("损伤抵抗持续秒数（期间不再累积凋亡损伤）")
				.defineInRange("resistance_seconds", 5, 0, 600);
		APOPTOSIS_HITS_WITH_SECOND = builder
				.comment("佩戴 2 号件「坠羽」时，打满损伤条需要的近战次数（定稿为「6 − 2 = 4」）")
				.defineInRange("hits_with_second", 4, 1, 100);
		APOPTOSIS_BURST_RATIO_WITH_SECOND_PERCENT = builder
				.comment("佩戴「坠羽」时，爆条伤害 = 本轮「推条那几刀」伤害之和 × 该比例（%）",
						"（取代固定的 burst_damage；刀数为 hits_with_second）")
				.defineInRange("burst_ratio_with_second_percent", 60.0D, 0.0D, 1000.0D);
		APOPTOSIS_EXTRA_DAMAGE_PERCENT_WITH_SECOND = builder
				.comment("佩戴「坠羽」时，损伤状态期间额外附加的元素伤害比例（%）",
						"（取代 extra_damage_percent）")
				.defineInRange("extra_damage_percent_with_second", 20.0D, 0.0D, 1000.0D);
		APOPTOSIS_WING_SECONDS = builder
				.comment("「坠羽」爆条时背后展开翅膀的持续秒数（再次爆条会刷新）")
				.defineInRange("wing_seconds", 5, 0, 600);
		APOPTOSIS_IDLE_CLEAR_SECONDS = builder
				.comment("连续多少秒没有被推过条就清空凋亡损伤进度（值与已累计的推条伤害一起清零；0 = 关闭）")
				.defineInRange("idle_clear_seconds", 30, 0, 3600);
		APOPTOSIS_EXECUTE_THRESHOLD_PERCENT = builder
				.comment("「笞心」斩杀的血量阈值（%）：目标受伤后生命值 ≤ 最大生命 × 该值时触发斩杀")
				.defineInRange("execute_threshold_percent", 15.0D, 0.0D, 100.0D);
		APOPTOSIS_EXECUTE_DAMAGE_PERCENT_OF_MAX = builder
				.comment("斩杀伤害 = 目标最大生命 × 该比例（%），作为一笔元素伤害结算")
				.defineInRange("execute_damage_percent_of_max", 100.0D, 0.0D, 1000.0D);
		APOPTOSIS_BURST_MULTIPLIER_WITH_THIRD = builder
				.comment("佩戴「笞心」时，最终爆条伤害的乘数（1.5 = 固定 30 → 45、坠羽档 60% → 90%）")
				.defineInRange("burst_multiplier_with_third", 1.5D, 0.0D, 10.0D);
		APOPTOSIS_EXTRA_MULTIPLIER_WITH_THIRD = builder
				.comment("佩戴「笞心」时，损伤状态附加伤害比例的乘数（1.5 = 10% → 15%、坠羽档 20% → 30%）")
				.defineInRange("extra_multiplier_with_third", 1.5D, 0.0D, 10.0D);
		APOPTOSIS_RESISTANCE_CUT_SECONDS_WITH_FOURTH = builder
				.comment("佩戴 4 号件「枯笔」时，近战直击「损伤抵抗中」的目标会让其抵抗时间减少的秒数",
						"（减到 0 即抵抗立刻结束）")
				.defineInRange("resistance_cut_seconds_with_fourth", 1, 0, 100);
		APOPTOSIS_FEAR_SECONDS_WITH_FOURTH = builder
				.comment("佩戴「枯笔」引爆时，目标获得的恐惧持续时间（秒）：期间无法移动与攻击")
				.defineInRange("fear_seconds_with_fourth", 1, 0, 600);
		APOPTOSIS_FEAR_AFFECTS_PLAYERS = builder
				.comment("恐惧是否也对玩家生效（默认 false）。",
						"玩家档 = 移动速度与攻速归零 + 近战与右击全部封禁；可用指令 /summyapocrypha fear players <on|off> 切换")
				.define("fear_affects_players", false);
		APOPTOSIS_BURST_PERCENT_PER_SECOND_WITH_FIFTH = builder
				.comment("佩戴 5 号件「终歌」时，损伤状态期间每秒结算的元素伤害 = 本次爆伤 × 该比例（%）")
				.defineInRange("burst_percent_per_second_with_fifth", 25.0D, 0.0D, 100.0D);
		APOPTOSIS_HEAL_REDUCTION_PERCENT_WITH_FIFTH = builder
				.comment("佩戴「终歌」时，损伤状态的减疗比例提升到的值（%，取代 heal_reduction_percent）")
				.defineInRange("heal_reduction_percent_with_fifth", 95.0D, 0.0D, 100.0D);
		APOPTOSIS_DAMAGE_TAKEN_BONUS_PERCENT_WITH_FIFTH = builder
				.comment("佩戴「终歌」时，损伤状态的目标受到的所有伤害提升的比例（%）",
						"（只作用于非本模组元素伤害；元素伤害仍按钉住值结算）")
				.defineInRange("damage_taken_bonus_percent_with_fifth", 30.0D, 0.0D, 1000.0D);
		builder.pop();

		SPEC = builder.build();
	}

	private ApocryphaConfig() {
	}

	/** 总开关 */
	public static boolean enable() {
		return ENABLE.get();
	}

	/** 佩戴基础件后，每个合格野箱出货的概率（%） */
	public static int acquisitionChancePercent() {
		return ACQUISITION_CHANCE_PERCENT.get();
	}

	/** 连续失败多少箱后保底；0 = 关闭 */
	public static int acquisitionPityChests() {
		return ACQUISITION_PITY_CHESTS.get();
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

	// ===== 凋亡（恶魔线 #1） =====

	/** 凋亡损伤条的总点数 */
	public static double apoptosisBarMax() {
		return APOPTOSIS_BAR_MAX.get();
	}

	/** 打满损伤条需要的近战次数（每次推 bar_max / 该值） */
	public static int apoptosisHitsBase() {
		return APOPTOSIS_HITS_BASE.get();
	}

	/** 损伤爆发的元素伤害 */
	public static double apoptosisBurstDamage() {
		return APOPTOSIS_BURST_DAMAGE.get();
	}

	/** 损伤状态期间附加的元素伤害（百分比） */
	public static double apoptosisExtraDamagePercent() {
		return APOPTOSIS_EXTRA_DAMAGE_PERCENT.get();
	}

	/** 损伤状态期间的治疗削减（百分比） */
	public static double apoptosisHealReductionPercent() {
		return APOPTOSIS_HEAL_REDUCTION_PERCENT.get();
	}

	/** 损伤状态持续秒数 */
	public static int apoptosisInjuredSeconds() {
		return APOPTOSIS_INJURED_SECONDS.get();
	}

	/** 损伤抵抗持续秒数 */
	public static int apoptosisResistanceSeconds() {
		return APOPTOSIS_RESISTANCE_SECONDS.get();
	}

	/** 佩戴「坠羽」时打满损伤条需要的近战次数 */
	public static int apoptosisHitsWithSecond() {
		return APOPTOSIS_HITS_WITH_SECOND.get();
	}

	/** 佩戴「坠羽」时爆条伤害的比例（%，乘以本轮推条伤害之和） */
	public static double apoptosisBurstRatioWithSecondPercent() {
		return APOPTOSIS_BURST_RATIO_WITH_SECOND_PERCENT.get();
	}

	/** 佩戴「坠羽」时损伤状态的附加伤害比例（%） */
	public static double apoptosisExtraDamagePercentWithSecond() {
		return APOPTOSIS_EXTRA_DAMAGE_PERCENT_WITH_SECOND.get();
	}

	/** 「坠羽」爆条时翅膀的持续秒数 */
	public static int apoptosisWingSeconds() {
		return APOPTOSIS_WING_SECONDS.get();
	}

	/** 连续多少秒没有被推过条就清空凋亡损伤进度（0 = 关闭） */
	public static int apoptosisIdleClearSeconds() {
		return APOPTOSIS_IDLE_CLEAR_SECONDS.get();
	}

	/** 「笞心」斩杀的血量阈值（%） */
	public static double apoptosisExecuteThresholdPercent() {
		return APOPTOSIS_EXECUTE_THRESHOLD_PERCENT.get();
	}

	/** 斩杀伤害占目标最大生命的比例（%） */
	public static double apoptosisExecuteDamagePercentOfMax() {
		return APOPTOSIS_EXECUTE_DAMAGE_PERCENT_OF_MAX.get();
	}

	/** 佩戴「笞心」时最终爆条伤害的乘数 */
	public static double apoptosisBurstMultiplierWithThird() {
		return APOPTOSIS_BURST_MULTIPLIER_WITH_THIRD.get();
	}

	/** 佩戴「笞心」时附加伤害比例的乘数 */
	public static double apoptosisExtraMultiplierWithThird() {
		return APOPTOSIS_EXTRA_MULTIPLIER_WITH_THIRD.get();
	}

	/** 佩戴「枯笔」时每次近战直击削减的抵抗秒数 */
	public static int apoptosisResistanceCutSecondsWithFourth() {
		return APOPTOSIS_RESISTANCE_CUT_SECONDS_WITH_FOURTH.get();
	}

	/** 佩戴「枯笔」引爆时目标的恐惧持续秒数 */
	public static int apoptosisFearSecondsWithFourth() {
		return APOPTOSIS_FEAR_SECONDS_WITH_FOURTH.get();
	}

	/** 恐惧是否也对玩家生效 */
	public static boolean apoptosisFearAffectsPlayers() {
		return APOPTOSIS_FEAR_AFFECTS_PLAYERS.get();
	}

	/** 运行时切换「恐惧是否对玩家生效」（指令用），并立刻写回配置文件 */
	public static void setApoptosisFearAffectsPlayers(boolean value) {
		APOPTOSIS_FEAR_AFFECTS_PLAYERS.set(value);
		SPEC.save();
	}

	/** 佩戴「终歌」时每秒结算的爆伤比例（%） */
	public static double apoptosisBurstPercentPerSecondWithFifth() {
		return APOPTOSIS_BURST_PERCENT_PER_SECOND_WITH_FIFTH.get();
	}

	/** 佩戴「终歌」时损伤状态的减疗比例（%） */
	public static double apoptosisHealReductionPercentWithFifth() {
		return APOPTOSIS_HEAL_REDUCTION_PERCENT_WITH_FIFTH.get();
	}

	/** 佩戴「终歌」时损伤状态目标的受伤加成（%） */
	public static double apoptosisDamageTakenBonusPercentWithFifth() {
		return APOPTOSIS_DAMAGE_TAKEN_BONUS_PERCENT_WITH_FIFTH.get();
	}
}
