package com.summy.reliquary.apocrypha.net;

import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * SRA 1.21.1 强类型 Payload 网络通道。
 */
public final class ApocryphaNetworking {
	public static final String PROTOCOL = "3";

	private ApocryphaNetworking() {
	}

	public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(PROTOCOL);
		registrar.playToServer(AirJumpMessage.TYPE, AirJumpMessage.CODEC,
				(payload, context) -> payload.handle(context));
		registrar.playToClient(ApoptosisBarMessage.TYPE, ApoptosisBarMessage.CODEC,
				(payload, context) -> payload.handle(context));
		registrar.playToClient(AirJumpSettingsMessage.TYPE, AirJumpSettingsMessage.CODEC, AirJumpSettingsMessage::handle);
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

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			sendAirJumpSettings(player, airJumpSettings());
		}
	}

	public static void onConfigReload(ModConfigEvent.Reloading event) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (event.getConfig().getSpec() != ApocryphaConfig.SPEC || server == null) {
			return;
		}
		// 配置监听可能运行在文件监视线程；发包和玩家列表读取都安排到服务端线程。
		server.execute(() -> {
			AirJumpSettingsMessage settings = airJumpSettings();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				sendAirJumpSettings(player, settings);
			}
		});
	}

	private static AirJumpSettingsMessage airJumpSettings() {
		return new AirJumpSettingsMessage(ApocryphaConfig.enable(), ApocryphaConfig.hoverAirJumps(),
				ApocryphaConfig.hoverAirJumpVelocity());
	}

	private static void sendAirJumpSettings(ServerPlayer player, AirJumpSettingsMessage settings) {
		sendToPlayer(player, settings);
	}

	public static String protocolVersion() {
		return PROTOCOL;
	}
}
