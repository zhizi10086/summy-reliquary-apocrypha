package com.summy.reliquary.apocrypha.net;

import com.summy.reliquary.apocrypha.client.ApoptosisBarState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C：把某个目标的「凋亡损伤条」状态发给能看到它的玩家（协议 {@code "2"} 新增）。
 *
 * <p>为什么需要它：条要画在目标头顶，而"当前损伤值 / 损伤状态剩余 / 抵抗剩余"只存在服务端，
 * 客户端必须知道才能渲染；原版没有可复用的同步通道，所以自己发一条。
 *
 * <p>客户端收到后写进 {@link ApoptosisBarState}（纯数据缓存），之后本地逐 tick 衰减，
 * 服务端只在阶段切换与节流间隔上补发，因此包量很小。
 */
public final class ApoptosisBarMessage {
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

	public static void encode(ApoptosisBarMessage message, FriendlyByteBuf buffer) {
		buffer.writeVarInt(message.entityId);
		buffer.writeFloat(message.value);
		buffer.writeVarInt(message.injuredTicks);
		buffer.writeVarInt(message.resistTicks);
	}

	public static ApoptosisBarMessage decode(FriendlyByteBuf buffer) {
		return new ApoptosisBarMessage(buffer.readVarInt(), buffer.readFloat(), buffer.readVarInt(),
				buffer.readVarInt());
	}

	public static void handle(ApoptosisBarMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> ApoptosisBarState.accept(message.entityId, message.value, message.injuredTicks,
				message.resistTicks));
		context.setPacketHandled(true);
	}
}
