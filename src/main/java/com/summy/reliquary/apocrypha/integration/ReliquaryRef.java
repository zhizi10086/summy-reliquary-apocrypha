package com.summy.reliquary.apocrypha.integration;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.fml.ModList;

import java.util.Set;

/**
 * 对前置模组 Summy Reliquary（SR）的唯一访问出口。
 *
 * <p>所有跨模组调用都收在这里，理由：SR 一旦改动 API 或版本线，只需要改这一个文件。
 * 本模组与 SR 是「硬前置 + 编译期直接依赖」的关系（见 build.gradle 的 compileOnly），
 * 所以这里可以放心直接写 SR 的类型与静态方法。
 */
public final class ReliquaryRef {
	/** 前置模组的 modId */
	public static final String MOD_ID = "summy_reliquary";

	/** SR 的伤害类型在 message_id 上统一用这个前缀（见 SR 的 data/summy-reliquary/damage_type/*.json） */
	private static final String MSG_ID_PREFIX = "summy-reliquary.";

	/**
	 * SR 自带「无视无敌帧」的真伤类型（按 message_id 匹配）。
	 *
	 * <p>来源：SR 的 data/summy-reliquary/damage_type 目录 —— 共 11 个类型，其中 10 个是真伤，
	 * 唯一的例外是 golden_razor（金刀片：物理伤害、本来就不无视无敌帧），因此不在名单里。
	 *
	 * <p>还要注意 godhead_aura（神性光环）与 holy_light（圣光）共用一个 message_id，
	 * 所以 10 个真伤文件去重后是下面 9 个 id。
	 *
	 * <p>SR 将来若新增真伤类型，本表需要同步；漏登记时会有兜底提醒（见 TrueDamageFrames 的漂移告警）。
	 */
	private static final Set<String> TRUE_DAMAGE_MSG_IDS = Set.of(
			"summy-reliquary.abyss_aura",
			"summy-reliquary.demon_flame",
			"summy-reliquary.holy_light",
			"summy-reliquary.hellfire",
			"summy-reliquary.holy_light_burst",
			"summy-reliquary.pact_shatter",
			"summy-reliquary.revelation_light",
			"summy-reliquary.sacrifice",
			"summy-reliquary.sacrifice_self");

	private ReliquaryRef() {
	}

	/** 前置模组的版本号；未安装时返回占位文本（正常不会发生：mods.toml 里是强制依赖） */
	public static String version() {
		return ModList.get().getModContainerById(MOD_ID)
				.map(container -> container.getModInfo().getVersion().toString())
				.orElse("（未安装）");
	}

	/** 前置模组的网络协议版本（用于排查两端版本不匹配） */
	public static String protocolVersion() {
		return com.summy.reliquary.net.ReliquaryNetworking.protocolVersion();
	}

	/**
	 * 是不是 SR 的「无视无敌帧」真伤。
	 *
	 * <p>DamageSource#getMsgId() 返回的是伤害类型的 message_id（死亡文本键），
	 * SR 的类型全部写成 summy-reliquary.&lt;path&gt;，所以按名单精确匹配即可。
	 */
	public static boolean isTrueDamage(DamageSource source) {
		return source != null && TRUE_DAMAGE_MSG_IDS.contains(source.getMsgId());
	}

	/** 是不是「SR 命名空间下的伤害类型」（用于漂移告警：SR 新增了真伤但我们没登记） */
	public static boolean looksLikeSrDamage(String msgId) {
		return msgId != null && msgId.startsWith(MSG_ID_PREFIX);
	}

	/**
	 * 当前是否跑在 Fabric 侧（Kilt / Connector）。
	 *
	 * <p>直接复用 SR 的探测结论：这些环境下 LivingHurtEvent 发生在 hurt 开头
	 * （原版还没把 invulnerableTime 写成 20），因此清帧必须推迟到 tick 末尾才不会被覆盖。
	 */
	public static boolean fabricSide() {
		return com.summy.reliquary.effect.DamagePools.fabricSide();
	}
}
