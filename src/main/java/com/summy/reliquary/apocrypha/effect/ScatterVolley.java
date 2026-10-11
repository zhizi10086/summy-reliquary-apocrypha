package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.entity.NewTestamentArrow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

// 落箭散射的「待落批次」：触发时只登记一条，之后每 N tick 放出一支，形成连珠落箭。
// 生成高度会考虑头顶方块（洞穴/室内不会整批失效）。
public final class ScatterVolley {
	// 待落批次上限（纯保险：正常一次触发只登记一条）
	private static final int MAX_PENDING = 256;

	// 以下三项是内部实现细节，从配置回调为硬编码（没人会去调，留在配置里只是噪音）
	/** 错峰生成间隔（tick）：每支箭之间隔多少 tick 放出；触发日志里的「间隔 N tick」引用它 */
	public static final int SPAWN_INTERVAL_TICKS = 2;
	/** 头顶有方块时的夹紧余量（格）：实际高度 = 命中点 − 该值 */
	private static final double CEILING_MARGIN = 0.5D;
	/** 夹紧后的最小生成高度（格）：头顶空间不足时也不会低于「目标顶部 + 该值」 */
	private static final double MIN_HEIGHT = 0.5D;

	private static final List<Pending> PENDING = new ArrayList<>();

	private ScatterVolley() {
	}

	// 一条待落批次
	private static final class Pending {
		private final ServerLevel level;
		private final UUID shooter;
		private final UUID target;
		// 触发瞬间的中心点（x/z 为目标中心、y 为包围盒顶部）；目标消失时沿用它
		private final Vec3 fallbackCenter;
		private final int total;
		// 本批的统一参数：初速倍率（迅羽档 > 1）与钉住的单支伤害（<= 0 表示不钉住、用配置的基础伤害）
		private final double speedMultiplier;
		private final float pinnedDamage;
		/** 纯视觉批次（瞬时结算用）：箭矢只做表现，不结算伤害 */
		private final boolean visualOnly;
		private int spawned;
		// 下一支箭的生成时刻（game time）
		private long nextTick;

		private Pending(ServerLevel level, UUID shooter, UUID target, Vec3 fallbackCenter, int total,
				double speedMultiplier, float pinnedDamage, boolean visualOnly, long nextTick) {
			this.level = level;
			this.shooter = shooter;
			this.target = target;
			this.fallbackCenter = fallbackCenter;
			this.total = total;
			this.speedMultiplier = speedMultiplier;
			this.pinnedDamage = pinnedDamage;
			this.visualOnly = visualOnly;
			this.nextTick = nextTick;
		}
	}

	// 登记一批落箭；间隔配成 0 时退化为同一 tick 全部生成
	public static void enqueue(ServerLevel level, ServerPlayer shooter, LivingEntity target, int count,
			double speedMultiplier, float pinnedDamage, boolean visualOnly) {
		if (count <= 0) {
			return;
		}
		Vec3 center = centerOf(target);
		int interval = SPAWN_INTERVAL_TICKS;
		if (interval <= 0) {
			for (int index = 0; index < count; index++) {
				spawn(level, shooter, target, center, false, speedMultiplier, pinnedDamage, visualOnly);
			}
			return;
		}
		if (PENDING.size() >= MAX_PENDING) {
			return;
		}
		PENDING.add(new Pending(level, shooter.getUUID(), target.getUUID(), center, count, speedMultiplier,
				pinnedDamage, visualOnly, level.getGameTime()));
	}

	/**
	 * 立刻生成一支落箭并把它交给调用方（瞬时结算模式用它当"伤害来源"，
	 * 这样既保住"直接来源是箭矢 → SR 按远程口径处理"，也让钉住与日志逻辑照旧生效）。
	 */
	public static NewTestamentArrow spawnOne(ServerLevel level, ServerPlayer shooter, LivingEntity target,
			double speedMultiplier, float pinnedDamage, boolean visualOnly, boolean logStart) {
		return spawn(level, shooter, target, centerOf(target), logStart, speedMultiplier, pinnedDamage,
				visualOnly);
	}

	// 服务端 tick 末尾：按间隔逐支放出
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		Iterator<Pending> iterator = PENDING.iterator();
		while (iterator.hasNext()) {
			Pending pending = iterator.next();
			if (pending.level.getGameTime() < pending.nextTick) {
				continue;
			}
			// 射手离线或换了维度：这一批剩下的直接作废（落箭的出伤归属依赖他）
			ServerPlayer shooter = server.getPlayerList().getPlayer(pending.shooter);
			if (shooter == null || shooter.serverLevel() != pending.level) {
				iterator.remove();
				continue;
			}
			LivingEntity target = pending.level.getEntity(pending.target) instanceof LivingEntity living
					? living
					: null;
			spawn(pending.level, shooter, target, pending.fallbackCenter, pending.spawned == 0,
					pending.speedMultiplier, pending.pinnedDamage, pending.visualOnly);
			pending.spawned++;
			if (pending.spawned >= pending.total) {
				iterator.remove();
			} else {
				pending.nextTick = pending.level.getGameTime() + SPAWN_INTERVAL_TICKS;
			}
		}
	}

	// 服务器停止 / 离开存档：清空待落队列，避免残留到下一个存档
	public static void clear() {
		PENDING.clear();
	}

	// 目标当前的中心点（x/z 取实体中心、y 取包围盒顶部）
	private static Vec3 centerOf(LivingEntity target) {
		return new Vec3(target.getX(), target.getBoundingBox().maxY, target.getZ());
	}

	// 生成一支落箭
	private static NewTestamentArrow spawn(ServerLevel level, ServerPlayer shooter, LivingEntity target,
			Vec3 fallbackCenter, boolean first, double speedMultiplier, float pinnedDamage, boolean visualOnly) {
		Vec3 center = target != null ? centerOf(target) : fallbackCenter;
		Vec3 origin = clampedOrigin(level, center);

		double maxAngle = Math.toRadians(ApocryphaConfig.scatterAngleMaxDegrees());
		// 迅羽档会提高初速（1.0 = 原样）
		double speed = ApocryphaConfig.scatterInitialSpeed() * speedMultiplier;
		double angle = maxAngle * level.random.nextDouble();
		double azimuth = level.random.nextDouble() * Math.PI * 2.0D;
		// 以竖直向下 (0, -1, 0) 为中心轴：按方位角偏出，再按偏角绕轴旋转
		Vec3 direction = new Vec3(
				Math.sin(angle) * Math.cos(azimuth),
				-Math.cos(angle),
				Math.sin(angle) * Math.sin(azimuth));

		NewTestamentArrow arrow = new NewTestamentArrow(level, shooter);
		arrow.setPos(origin.x, origin.y, origin.z);
		arrow.setDeltaMovement(direction.scale(speed));
		arrow.setPinnedDamage(pinnedDamage);
		arrow.setVisualOnly(visualOnly);
		level.addFreshEntity(arrow);

		// 每批只在第一支箭上记一行起点，用来核对「实际高度」是否被天花板夹紧
		if (first && ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info(String.format(
					"[新约] 落箭起点：实际高度 %.2f（配置 %.2f）、中心 %.1f/%.1f/%.1f",
					origin.y - center.y, ApocryphaConfig.scatterSpawnHeight(),
					origin.x, origin.y, origin.z));
		}
		return arrow;
	}

	// 生成点：按配置高度向上取点，撞到方块就压到「命中点 - 夹紧余量」，且不低于「目标顶部 + 最小高度」
	private static Vec3 clampedOrigin(ServerLevel level, Vec3 center) {
		double configured = ApocryphaConfig.scatterSpawnHeight();
		double minHeight = MIN_HEIGHT;
		Vec3 from = new Vec3(center.x, center.y, center.z);
		Vec3 to = new Vec3(center.x, center.y + configured, center.z);
		BlockHitResult hit = level.clip(new ClipContext(from, to,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
		double height = configured;
		if (hit.getType() == HitResult.Type.BLOCK) {
			height = Math.max(minHeight, hit.getLocation().y - center.y - CEILING_MARGIN);
		}
		return new Vec3(center.x, center.y + height, center.z);
	}
}
