package com.summy.reliquary.apocrypha.net;

import com.summy.reliquary.apocrypha.effect.HoverFlight;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S 空包：客户端「空中按下跳跃键」时发一次（只在按下沿发送，频率极低）。
 *
 * <p>服务端收到后再做一次完整校验（是否佩戴「逆羽·滞空」、是否离地、本段是否还有剩余跳数），
 * 所以客户端即使被改也顶多骗到一次自己本来就能做的跳跃。
 */
public final class AirJumpMessage {
	public static void encode(AirJumpMessage message, FriendlyByteBuf buffer) {
	}

	public static AirJumpMessage decode(FriendlyByteBuf buffer) {
		return new AirJumpMessage();
	}

	public static void handle(AirJumpMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		ServerPlayer player = context.getSender();
		if (player != null) {
			context.enqueueWork(() -> HoverFlight.tryAirJump(player));
		}
		context.setPacketHandled(true);
	}
}
