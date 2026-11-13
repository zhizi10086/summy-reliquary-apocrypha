package com.summy.reliquary.apocrypha.net;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.client.ApoptosisBarState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C：同步某个目标的凋亡损伤条状态。
 */
public final class ApoptosisBarMessage implements CustomPacketPayload {
	public static final Type<ApoptosisBarMessage> TYPE =
			new Type<>(Apocrypha.id("apoptosis_bar"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ApoptosisBarMessage> CODEC =
			StreamCodec.of((buffer, message) -> message.encode(buffer), ApoptosisBarMessage::new);

	private final int entityId;
	private final float value;
	private final int injuredTicks;
	private final int resistTicks;

	public ApoptosisBarMessage(int entityId, float value, int injuredTicks, int resistTicks) {
		this.entityId = entityId;
		this.value = value;
		this.injuredTicks = injuredTicks;
		this.resistTicks = resistTicks;
	}

	private ApoptosisBarMessage(RegistryFriendlyByteBuf buffer) {
		this.entityId = buffer.readVarInt();
		this.value = buffer.readFloat();
		this.injuredTicks = buffer.readVarInt();
		this.resistTicks = buffer.readVarInt();
	}

	private void encode(RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(entityId);
		buffer.writeFloat(value);
		buffer.writeVarInt(injuredTicks);
		buffer.writeVarInt(resistTicks);
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> ApoptosisBarState.accept(entityId, value, injuredTicks, resistTicks));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
