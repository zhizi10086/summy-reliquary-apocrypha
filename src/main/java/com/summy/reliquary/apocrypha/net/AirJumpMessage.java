package com.summy.reliquary.apocrypha.net;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.effect.HoverFlight;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S 空包：客户端在空中按下跳跃键时发送一次。
 */
public final class AirJumpMessage implements CustomPacketPayload {
	public static final Type<AirJumpMessage> TYPE =
			new Type<>(Apocrypha.id("air_jump"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AirJumpMessage> CODEC =
			StreamCodec.of((buffer, message) -> message.encode(buffer), AirJumpMessage::new);

	public AirJumpMessage() {
	}

	private AirJumpMessage(RegistryFriendlyByteBuf buffer) {
	}

	private void encode(RegistryFriendlyByteBuf buffer) {
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player) {
				HoverFlight.tryAirJump(player);
			}
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
