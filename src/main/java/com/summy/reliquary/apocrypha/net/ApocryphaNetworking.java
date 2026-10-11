package com.summy.reliquary.apocrypha.net;

import com.summy.reliquary.apocrypha.Apocrypha;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 本模组的网络通道（与 SR 的 {@code summy-reliquary:main} 分开，互不干扰）。
 *
 * <p>协议版本 {@code "1"}：目前只注册一条 C2S 空包 —— 「空中按下跳跃键」（{@link AirJumpMessage}），
 * 用于「逆羽·滞空」的额外跳跃。之所以需要它：原版的"跳跃键"输入状态是 {@code LivingEntity#jumping}
 * 这个 protected 字段，服务端没有公开读取方式，所以由客户端在**按下沿**上报一次。
 * 后续新增包时按顺序追加注册序号并同步升协议版本。
 */
public final class ApocryphaNetworking {
	private static final String PROTOCOL = "1";

	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			Apocrypha.id("main"),
			() -> PROTOCOL,
			PROTOCOL::equals,
			PROTOCOL::equals);

	private ApocryphaNetworking() {
	}

	/** 在模组构造阶段调用 */
	public static void register() {
		CHANNEL.registerMessage(0, AirJumpMessage.class,
				AirJumpMessage::encode, AirJumpMessage::decode, AirJumpMessage::handle);
	}

	/** 当前协议版本（自检 / 文档用） */
	public static String protocolVersion() {
		return PROTOCOL;
	}
}
