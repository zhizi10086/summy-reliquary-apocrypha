package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 「逆羽·滞空」的被动部分：缓降（空中长按潜行键）与额外跳跃（空中按跳跃键）。
 *
 * <p>缓降：每 tick 把垂直速度压到 {@code vy × (1 − 折减)}；同时把**当 tick 的下降量**按同一比例
 * 从 {@code fallDistance} 里扣掉，所以落地伤害按「坠落高度 −N%」计算（不是完全免伤）。
 *
 * <p>额外跳跃：服务端每 tick 在玩家落地时重置计数；收到 {@code AirJumpMessage} 时校验并施加一次向上速度。
 * 计数只存在内存（不写存档），退服 / 死亡时清理。
 */
public final class HoverFlight {
	/** 空中额外跳跃的初速（原版跳跃初速） */
	private static final double AIR_JUMP_VELOCITY = 0.42D;

	private static final Map<UUID, Integer> AIR_JUMPS_USED = new HashMap<>();

	private HoverFlight() {
	}

	/** 「下落状态」：不在地面且垂直速度为负（三项"下落中"增益与缓降共用这一判据） */
	public static boolean isFalling(LivingEntity entity) {
		return entity != null && !entity.onGround() && entity.getDeltaMovement().y < 0.0D;
	}

	/** 是否佩戴「逆羽·滞空」（服务端判定） */
	public static boolean wears(LivingEntity entity) {
		return entity != null && CurioHelper.wears(entity, Apocrypha.INVERSE_FEATHER_HOVER.get());
	}

	/** 服务端每 tick：缓降 + 落地重置额外跳跃计数 */
	public static void tickPlayer(ServerPlayer player) {
		if (player.onGround()) {
			AIR_JUMPS_USED.remove(player.getUUID());
			return;
		}
		if (!wears(player) || !player.isShiftKeyDown()) {
			return;
		}
		Vec3 velocity = player.getDeltaMovement();
		if (velocity.y >= 0.0D) {
			return;
		}
		double reduction = Math.max(0.0D, Math.min(1.0D, ApocryphaConfig.hoverGlideReductionPercent() / 100.0D));
		if (reduction <= 0.0D) {
			return;
		}
		double descent = -velocity.y;
		player.setDeltaMovement(velocity.x, velocity.y * (1.0D - reduction), velocity.z);
		// 让客户端同步到被压低的速度，否则本地预测会先掉一段
		player.hurtMarked = true;
		// 坠落高度也只按剩余比例计入 —— 于是落地伤害相当于「坠落高度 −N%」
		player.fallDistance = Math.max(0.0F, player.fallDistance - (float) (descent * reduction));
	}

	/** 服务端收到「空中按下跳跃键」：完整校验后再施加额外跳跃 */
	public static void tryAirJump(ServerPlayer player) {
		if (!ApocryphaConfig.enable() || !wears(player)) {
			return;
		}
		if (player.onGround() || player.isPassenger()) {
			return;
		}
		int max = ApocryphaConfig.hoverAirJumps();
		int used = AIR_JUMPS_USED.getOrDefault(player.getUUID(), 0);
		if (used >= max) {
			return;
		}
		AIR_JUMPS_USED.put(player.getUUID(), used + 1);
		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(velocity.x, AIR_JUMP_VELOCITY, velocity.z);
		player.hurtMarked = true;
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info("[新约] 逆羽·滞空：{} 空中跳跃 {}/{}", player.getName().getString(),
					used + 1, max);
		}
	}

	/** 服务端 tick 末尾：逐个玩家处理缓降与落地重置 */
	public static void tickServer(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickPlayer(player);
		}
	}

	/** 退服 / 死亡：清掉该玩家的空中跳计数 */
	public static void forget(ServerPlayer player) {
		AIR_JUMPS_USED.remove(player.getUUID());
	}

	/** 服务器停止：清空全部登记 */
	public static void clear() {
		AIR_JUMPS_USED.clear();
	}
}
