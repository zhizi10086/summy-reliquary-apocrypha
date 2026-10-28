package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
 * <p>额外跳跃：手感与输入状态机参考 Artifacts（MIT）的「云朵瓶」实现 ——
 * <ul>
 *     <li>{@link #jump(Player)} 是**双端共用**的：客户端调用它是本地预测（立刻起效、无延迟），
 *     服务端调用它是权威结算；速度赋值幂等，重复执行没有副作用；</li>
 *     <li>音效只在客户端本地播一次、服务端广播给**其他人**（排除本人），因此本人只会听到一次；
 *     粒子与食物消耗、跳跃统计只在服务端做，避免双份。</li>
 * </ul>
 *
 * <p>计数只存在内存（不写存档），退服 / 死亡时清理；{@code groundedTicks} 用来消除
 * 「地面起跳那一次按键」与「刚离地 1 tick」的时序竞争（详见 {@link #tryAirJump}）。
 */
public final class HoverFlight {
	/** 每个玩家的空中状态（只存内存） */
	private static final Map<UUID, AirState> STATES = new HashMap<>();

	private HoverFlight() {
	}

	/** 每个玩家的空中状态：连续站在地面的 tick 数 + 本段离地已用掉的额外跳跃次数 */
	private static final class AirState {
		/** 服务端连续看到「站在地面上」的 tick 数：> 0 表示上一 tick 末尾玩家还在地上 */
		private int groundedTicks;
		/** 本段离地已经用掉的额外跳跃次数 */
		private int jumpsUsed;
	}

	/** 「下落状态」：不在地面且垂直速度为负（三项"下落中"增益与缓降共用这一判据） */
	public static boolean isFalling(LivingEntity entity) {
		return entity != null && !entity.onGround() && entity.getDeltaMovement().y < 0.0D;
	}

	/** 是否佩戴「逆羽·滞空」（服务端判定） */
	public static boolean wears(LivingEntity entity) {
		return entity != null && CurioHelper.wears(entity, Apocrypha.INVERSE_FEATHER_HOVER.get());
	}

	/**
	 * 施加一次额外跳跃（**双端共用**：客户端预测 + 服务端权威）。
	 *
	 * <p>只改垂直速度，水平速度原样保留；置 {@code hasImpulse} 让服务端把这次速度同步出去
	 * （{@code hurtMarked} 是「受伤击退」的语义，不适用于这里）。
	 * 同时清空 {@code fallDistance} —— 云朵瓶口径：跳完不再吃跳之前那段坠落伤害。
	 */
	public static void jump(Player player) {
		if (!ApocryphaConfig.enable()) {
			return;
		}
		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(velocity.x, ApocryphaConfig.hoverAirJumpVelocity(), velocity.z);
		player.hasImpulse = true;
		player.fallDistance = 0.0F;

		float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;
		if (player.level().isClientSide()) {
			// 客户端本地播一次：预测手感，不必等服务端回包
			player.playSound(SoundEvents.WOOL_FALL, 1.0F, pitch);
			return;
		}
		if (player.level() instanceof ServerLevel serverLevel) {
			// 只广播给其他人（第一个参数就是"要被排除的玩家"），本人已经在客户端预测时听到
			serverLevel.playSound(player, player.getX(), player.getY(), player.getZ(),
					SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 1.0F, pitch);
			// 粒子只由服务端发出（全员可见，客户端不重复发）
			serverLevel.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.2D, player.getZ(),
					20, 0.3D, 0.05D, 0.3D, 0.02D);
			// 与一次原版跳跃同口径的消耗与统计，只在服务端结算
			player.awardStat(Stats.JUMP);
			player.causeFoodExhaustion(0.05F);
		}
	}

	/**
	 * 服务端收到「空中按下跳跃键」：完整校验后再施加额外跳跃。
	 *
	 * <p>离地判据用 {@code groundedTicks == 0} 而不是 {@code player.onGround()}：
	 * 客户端在地面起跳的那次按键会与「服务端刚把玩家抬离地面」发生在同一 tick，
	 * 直接看 {@code onGround()} 会时真时假，从而把这一段的额外跳跃提前吃掉。
	 * {@code groundedTicks} 由 {@link #tickServer} 在每 tick 末尾更新，语义是
	 * 「上一 tick 末尾服务端还看到玩家站在地面」，恰好把那次按键排除掉。
	 */
	public static void tryAirJump(ServerPlayer player) {
		if (!ApocryphaConfig.enable() || !wears(player)) {
			return;
		}
		if (player.isPassenger() || player.getAbilities().flying) {
			return;
		}
		AirState state = STATES.computeIfAbsent(player.getUUID(), key -> new AirState());
		if (state.groundedTicks > 0) {
			// 这是"地面起跳"的那次按键，不是额外跳跃
			return;
		}
		int max = ApocryphaConfig.hoverAirJumps();
		if (state.jumpsUsed >= max) {
			return;
		}
		state.jumpsUsed++;
		jump(player);
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info("[新约] 逆羽·滞空：{} 空中跳跃 {}/{}", player.getName().getString(),
					state.jumpsUsed, max);
		}
	}

	/** 服务端每 tick：维护离地状态与空跳计数，并处理缓降 */
	public static void tickServer(MinecraftServer server) {
		if (!ApocryphaConfig.enable()) {
			STATES.clear();
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			AirState state = STATES.computeIfAbsent(player.getUUID(), key -> new AirState());
			if (player.onGround() || player.onClimbable()) {
				// 站在地面（或攀爬中）＝一段新的离地开始：补满额外跳跃
				state.groundedTicks++;
				state.jumpsUsed = 0;
			} else {
				state.groundedTicks = 0;
			}
			glide(player);
		}
	}

	/** 缓降：空中长按潜行键时压低垂直速度，并把当 tick 的下降量从坠落高度里扣掉 */
	private static void glide(ServerPlayer player) {
		if (!ApocryphaConfig.enable() || player.onGround() || !wears(player) || !player.isShiftKeyDown()) {
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

	/** 退服 / 死亡：清掉该玩家的空中状态 */
	public static void forget(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	/** 服务器停止：清空全部登记 */
	public static void clear() {
		STATES.clear();
	}
}
