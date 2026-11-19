package com.summy.reliquary.apocrypha.net;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.client.ApocryphaClientInput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 同步本次连接的服务端空跳设置，供客户端输入判断与预测。 */
public record AirJumpSettingsMessage(boolean enabled, int jumps, double velocity) implements CustomPacketPayload {
	public static final Type<AirJumpSettingsMessage> TYPE = new Type<>(Apocrypha.id("air_jump_settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AirJumpSettingsMessage> CODEC =
			StreamCodec.ofMember(AirJumpSettingsMessage::encode, AirJumpSettingsMessage::decode);

	private void encode(RegistryFriendlyByteBuf buffer) {
		buffer.writeBoolean(enabled);
		buffer.writeVarInt(jumps);
		buffer.writeDouble(velocity);
	}

	private static AirJumpSettingsMessage decode(RegistryFriendlyByteBuf buffer) {
		return new AirJumpSettingsMessage(buffer.readBoolean(), buffer.readVarInt(), buffer.readDouble());
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> ApocryphaClientInput.acceptSettings(this));
	}

	@Override
	public Type<AirJumpSettingsMessage> type() {
		return TYPE;
	}
}
