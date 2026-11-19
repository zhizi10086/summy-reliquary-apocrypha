package com.summy.reliquary.apocrypha.net;

import com.summy.reliquary.apocrypha.client.ApocryphaClientInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 同步本次连接的服务端空跳设置，供客户端输入判断与预测。 */
public record AirJumpSettingsMessage(boolean enabled, int jumps, double velocity) {
	public static void encode(AirJumpSettingsMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.enabled());
		buffer.writeVarInt(message.jumps());
		buffer.writeDouble(message.velocity());
	}

	public static AirJumpSettingsMessage decode(FriendlyByteBuf buffer) {
		return new AirJumpSettingsMessage(buffer.readBoolean(), buffer.readVarInt(), buffer.readDouble());
	}

	public static void handle(AirJumpSettingsMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> ApocryphaClientInput.acceptSettings(message));
		context.setPacketHandled(true);
	}
}
