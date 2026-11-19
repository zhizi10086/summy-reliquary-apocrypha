package com.summy.reliquary.apocrypha.net;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;

/**
 * 本模组的网络通道（与 SR 的 {@code summy-reliquary:main} 分开，互不干扰）。
 *
 * <p>协议版本 {@code "3"}：
 * <ul>
 *     <li>序号 0：C2S 空包「空中按下跳跃键」（{@link AirJumpMessage}）—— 用于「逆羽·滞空」的额外跳跃。
 *     之所以需要它：原版的"跳跃键"输入状态是 {@code LivingEntity#jumping} 这个 protected 字段，
 *     服务端没有公开读取方式，所以由客户端在**按下沿**上报一次；</li>
 *     <li>序号 1：S2C 包「凋亡损伤条」（{@link ApoptosisBarMessage}）—— 把目标的损伤条状态发给能看到它的玩家，
 *     供头顶条渲染。</li>
 *     <li>序号 2：S2C 同步服务端空跳设置。</li>
 * </ul>
 * 后续新增包时按顺序追加注册序号并同步升协议版本。
 */
public final class ApocryphaNetworking {
	private static final String PROTOCOL = "3";

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
		CHANNEL.registerMessage(1, ApoptosisBarMessage.class,
				ApoptosisBarMessage::encode, ApoptosisBarMessage::decode, ApoptosisBarMessage::handle);
		CHANNEL.registerMessage(2, AirJumpSettingsMessage.class, AirJumpSettingsMessage::encode,
				AirJumpSettingsMessage::decode, AirJumpSettingsMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
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
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), settings);
	}

	/** 当前协议版本（自检 / 文档用） */
	public static String protocolVersion() {
		return PROTOCOL;
	}
}
