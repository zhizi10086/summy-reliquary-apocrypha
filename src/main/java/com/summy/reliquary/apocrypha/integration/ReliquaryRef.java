package com.summy.reliquary.apocrypha.integration;

import net.minecraftforge.fml.ModList;

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
}
