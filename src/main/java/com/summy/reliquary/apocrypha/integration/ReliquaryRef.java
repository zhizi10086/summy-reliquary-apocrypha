package com.summy.reliquary.apocrypha.integration;

import net.neoforged.fml.ModList;

/** 读取硬前置 Summy Reliquary 的版本与网络协议，用于启动诊断。 */
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
