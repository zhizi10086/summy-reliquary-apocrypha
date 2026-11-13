package com.summy.reliquary.apocrypha.net;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * SRA 1.21.1 强类型 Payload 网络通道。
 */
public final class ApocryphaNetworking {
	public static final String PROTOCOL = "2";

	private ApocryphaNetworking() {
	}

	public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(PROTOCOL);
		registrar.playToServer(AirJumpMessage.TYPE, AirJumpMessage.CODEC,
				(payload, context) -> payload.handle(context));
		registrar.playToClient(ApoptosisBarMessage.TYPE, ApoptosisBarMessage.CODEC,
				(payload, context) -> payload.handle(context));
	}

	public static void sendAirJump() {
		PacketDistributor.sendToServer(new AirJumpMessage());
	}

	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}

	public static void sendToTracking(Entity entity, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
	}

	public static String protocolVersion() {
		return PROTOCOL;
	}
}
