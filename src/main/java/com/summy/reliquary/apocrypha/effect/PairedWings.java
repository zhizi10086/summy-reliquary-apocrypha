package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.entity.HolyArrow;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 「比翼·连携」的穿透圣光矢：发射、以及与「新约」落箭的协同补发。
 *
 * <p>圣光矢的伤害由触发那一刻算出（触发之箭「各类倍率后」的金额 + 配置加成），随箭携带并**钉住**，
 * 命中时不再叠加 SR 的乘区 —— 所以这里只负责"把箭按准心射出去"。
 *
 * <p>协同补发用两个小队列（圣光矢 / 落箭批次），延迟 {@value #COUPLED_DELAY_TICKS} tick（0.1 秒）执行：
 * 射手离线或换维度就丢弃该条；服务器停止时清空。目标在延迟期间死亡时，落箭沿用它登记时的中心点照常落下。
 */
public final class PairedWings {
	/** 协同补发的延迟（tick）：2 tick = 0.1 秒 */
	public static final int COUPLED_DELAY_TICKS = 2;
	/** 共同触发（两次掷点都成功）时额外多出的圣光矢道数 */
	public static final int COUPLED_BONUS_ARROWS = 1;
	/** 第二道圣光矢的水平偏角上限（度）：能看出是两道 */
	private static final double EXTRA_SPREAD_DEGREES = 3.0D;
	/** 圣光矢的初速（格/tick）：与原版满蓄力箭矢一致 */
	private static final double LAUNCH_VELOCITY = 3.0D;
	/** 发射点沿视线前移的距离（格）：避免一出生就贴在自己身上 */
	private static final double EYE_FORWARD_OFFSET = 0.3D;
	/** 待发队列上限（纯保险） */
	private static final int MAX_PENDING = 256;

	private static final List<HolyShot> HOLY_SHOTS = new ArrayList<>();
	private static final List<VolleyShot> VOLLEY_SHOTS = new ArrayList<>();

	private PairedWings() {
	}

	/** 待补发的一次圣光矢 */
	private static final class HolyShot {
		private final ServerLevel level;
		private final UUID shooter;
		private final int count;
		private final float damage;
		private final long fireTick;

		private HolyShot(ServerLevel level, UUID shooter, int count, float damage, long fireTick) {
			this.level = level;
			this.shooter = shooter;
			this.count = count;
			this.damage = damage;
			this.fireTick = fireTick;
		}
	}

	/** 待补发的一批落箭 */
	private static final class VolleyShot {
		private final ServerLevel level;
		private final UUID shooter;
		/** 目标 UUID；为 null 表示登记时目标就已经不在了 */
		private final UUID target;
		/** 登记瞬间的中心点（目标消失时沿用它） */
		private final Vec3 center;
		private final int count;
		private final double speedMultiplier;
		private final float pinnedDamage;
		/** 逆羽·滞空的下落中口径：伤害改为瞬时逐笔结算 */
		private final boolean instantSettle;
		private final long fireTick;

		private VolleyShot(ServerLevel level, UUID shooter, @Nullable UUID target, Vec3 center, int count,
				double speedMultiplier, float pinnedDamage, boolean instantSettle, long fireTick) {
			this.level = level;
			this.shooter = shooter;
			this.target = target;
			this.center = center;
			this.count = count;
			this.speedMultiplier = speedMultiplier;
			this.pinnedDamage = pinnedDamage;
			this.instantSettle = instantSettle;
			this.fireTick = fireTick;
		}
	}

	/** 是否佩戴「比翼·连携」 */
	public static boolean wears(LivingEntity entity) {
		return entity != null && CurioHelper.wears(entity, Apocrypha.PAIRED_WINGS.get());
	}

	/**
	 * 立刻放出 count 道圣光矢（1 或 2 道）。
	 *
	 * <p>从玩家眼位沿准心方向射出，初速 3.0 格/tick（等同满蓄力箭矢）、无重力；
	 * 第 2 道带 ±{@value #EXTRA_SPREAD_DEGREES}° 的水平偏角，看得出是两道。
	 */
	public static void fire(ServerLevel level, ServerPlayer shooter, int count, float damage) {
		if (count <= 0 || damage <= 0.0F) {
			return;
		}
		for (int index = 0; index < count; index++) {
			float pitch = shooter.getXRot();
			float yaw = shooter.getYRot();
			if (index > 0) {
				yaw += (float) ((level.random.nextDouble() * 2.0D - 1.0D) * EXTRA_SPREAD_DEGREES);
			}
			Vec3 origin = shooter.getEyePosition()
					.add(Vec3.directionFromRotation(pitch, yaw).scale(EYE_FORWARD_OFFSET));
			HolyArrow arrow = new HolyArrow(level, shooter);
			arrow.setPos(origin.x, origin.y, origin.z);
			arrow.setPinnedDamage(damage);
			arrow.shootFromRotation(shooter, pitch, yaw, 0.0F, (float) LAUNCH_VELOCITY, 0.0F);
			arrow.setNoGravity(true);
			level.addFreshEntity(arrow);
		}
	}

	/** 登记：count 道圣光矢延迟 {@link #COUPLED_DELAY_TICKS} tick 补发 */
	public static void enqueueHoly(ServerLevel level, ServerPlayer shooter, int count, float damage) {
		if (count <= 0 || damage <= 0.0F || HOLY_SHOTS.size() >= MAX_PENDING) {
			return;
		}
		HOLY_SHOTS.add(new HolyShot(level, shooter.getUUID(), count, damage,
				level.getGameTime() + COUPLED_DELAY_TICKS));
	}

	/** 登记：一批落箭延迟 {@link #COUPLED_DELAY_TICKS} tick 补发 */
	public static void enqueueVolley(ServerLevel level, ServerPlayer shooter, @Nullable LivingEntity target,
			Vec3 center, int count, double speedMultiplier, float pinnedDamage, boolean instantSettle) {
		if (count <= 0 || VOLLEY_SHOTS.size() >= MAX_PENDING) {
			return;
		}
		VOLLEY_SHOTS.add(new VolleyShot(level, shooter.getUUID(), target == null ? null : target.getUUID(), center,
				count, speedMultiplier, pinnedDamage, instantSettle,
				level.getGameTime() + COUPLED_DELAY_TICKS));
	}

	/** 服务端 tick 末尾：把到期的补发动作放出去 */
	public static void tick(MinecraftServer server) {
		if (HOLY_SHOTS.isEmpty() && VOLLEY_SHOTS.isEmpty()) {
			return;
		}
		Iterator<HolyShot> holyIterator = HOLY_SHOTS.iterator();
		while (holyIterator.hasNext()) {
			HolyShot shot = holyIterator.next();
			if (shot.level.getGameTime() < shot.fireTick) {
				continue;
			}
			holyIterator.remove();
			ServerPlayer shooter = server.getPlayerList().getPlayer(shot.shooter);
			if (shooter == null || shooter.serverLevel() != shot.level) {
				continue;
			}
			fire(shot.level, shooter, shot.count, shot.damage);
		}
		Iterator<VolleyShot> volleyIterator = VOLLEY_SHOTS.iterator();
		while (volleyIterator.hasNext()) {
			VolleyShot shot = volleyIterator.next();
			if (shot.level.getGameTime() < shot.fireTick) {
				continue;
			}
			volleyIterator.remove();
			ServerPlayer shooter = server.getPlayerList().getPlayer(shot.shooter);
			if (shooter == null || shooter.serverLevel() != shot.level) {
				continue;
			}
			LivingEntity target = shot.target != null
					&& shot.level.getEntity(shot.target) instanceof LivingEntity living ? living : null;
			// 听觉反馈与"当场触发"时保持一致（只发给触发者本人）
			shooter.playNotifySound(SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
			if (shot.instantSettle && target != null) {
				// 逆羽·滞空的下落中口径：伤害在补发这一刻逐笔瞬时结算
				NewTestamentEffect.settleInstantly(shot.level, shooter, target, shot.count, shot.speedMultiplier,
						shot.pinnedDamage);
			} else {
				ScatterVolley.enqueueAt(shot.level, shooter, target, shot.center, shot.count, shot.speedMultiplier,
						shot.pinnedDamage, false, 0);
			}
		}
	}

	/** 服务器停止：清空待发队列，避免残留到下一个存档 */
	public static void clear() {
		HOLY_SHOTS.clear();
		VOLLEY_SHOTS.clear();
	}

	/** 射手死亡 / 退服：只丢弃该玩家尚未发出的协同补发。 */
	public static void clearShooter(UUID shooter) {
		HOLY_SHOTS.removeIf(shot -> shot.shooter.equals(shooter));
		VOLLEY_SHOTS.removeIf(shot -> shot.shooter.equals(shooter));
	}
}
