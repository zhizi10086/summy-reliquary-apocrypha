package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

/**
 * 4 号件「枯笔」的**恐惧**（服务端）：引爆者佩戴枯笔时，被爆条的目标获得一小段「无法移动 + 无法攻击」。
 *
 * <p>两种目标两种做法：
 * <ul>
 *     <li><b>生物</b>：每 tick 在**服务器 tick 末尾**（晚于实体自身的 AI）清空攻击目标与寻路、
 *     并把它这一 tick 的水平速度清零（保留垂直分量，不冻结重力/跳跃）；</li>
 *     <li><b>玩家</b>（需把 {@code fear_affects_players} 打开，默认关）：给移动速度与攻速各挂一条
 *     {@code ADD_MULTIPLIED_TOTAL −1} 的固定 UUID 修饰符（走不动、冷却不回复），再拦
 *     {@link AttackEntityEvent}（近战）与 {@link PlayerInteractEvent.RightClickItem} / RightClickBlock
 *     （弓 / 药水 / 食物 / 放置 / 交互）。修饰符是 transient，到期、死亡、登出与停机都会主动移除，
 *     所以不会出现"永久定身"。</li>
 * </ul>
 *
 * <p>已知边界（写进文档）：玩家档只压移动速度 —— 爬梯与游泳是原版硬编码位移，不受影响；
 * 被击退 / 爆炸推动也不受限制。
 */
public final class WitheredBrush {
	/** 移动速度归零的修饰符 UUID（固定，便于幂等增删） */
	private static final UUID SPEED_MODIFIER = UUID.fromString("6f1c1a10-9f21-4c2f-9a7e-2b0f1d5c7a01");
	/** 攻速归零的修饰符 UUID */
	private static final UUID ATTACK_SPEED_MODIFIER = UUID.fromString("6f1c1a10-9f21-4c2f-9a7e-2b0f1d5c7a02");
	/** 临时 noAI 的恢复值；区块卸载或停服后靠实体重新载入时恢复。 */
	private static final String NO_AI_RESTORE = "ApocryphaFearRestoreNoAi";

	/** 被恐惧的目标 → （所在关卡 + 剩余 tick） */
	private static final Map<UUID, Fear> FEARED = new HashMap<>();

	private WitheredBrush() {
	}

	private static final class Fear {
		private final ServerLevel level;
		private int ticks;

		private Fear(ServerLevel level, int ticks) {
			this.level = level;
			this.ticks = ticks;
		}
	}

	/** 引爆时调用：给目标施加（或刷新）恐惧 */
	public static void applyFear(ServerLevel level, ServerPlayer source, LivingEntity target) {
		if (!ApocryphaConfig.enable()) {
			return;
		}
		int ticks = Math.max(0, ApocryphaConfig.apoptosisFearSecondsWithFourth() * 20);
		if (ticks <= 0) {
			return;
		}
		if (target instanceof ServerPlayer player) {
			// 玩家档默认关闭：要真正封禁玩家必须动属性与事件，属于可选行为
			if (!ApocryphaConfig.apoptosisFearAffectsPlayers()) {
				return;
			}
			// 骑乘会绕开移动速度归零；恐惧生效时先下马，恐惧期间实体交互也会被拦截。
			if (player.isPassenger()) {
				player.stopRiding();
			}
			setPlayerFrozen(player, true);
		} else if (!(target instanceof Mob mob)) {
			// 只对生物与玩家生效（盔甲架、展示实体之类没有"移动/攻击"的概念）
			return;
		} else {
			// 1.20.1 没有公开的 MoveControl#setWait；临时关闭 AI 后恢复原值是最可靠的硬停方案。
			CompoundTag persistent = mob.getPersistentData();
			if (!persistent.contains(NO_AI_RESTORE)) {
				persistent.putBoolean(NO_AI_RESTORE, mob.isNoAi());
			}
			mob.setNoAi(true);
			FEARED.put(target.getUUID(), new Fear(level, ticks));
		}
		if (target instanceof ServerPlayer) {
			FEARED.put(target.getUUID(), new Fear(level, ticks));
		}
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info(String.format("[凋亡] 恐惧：%s ← %s（%.1f 秒，无法移动与攻击）",
					target.getName().getString(), source.getName().getString(), ticks / 20.0D));
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (!ApocryphaConfig.enable()) {
			if (!FEARED.isEmpty()) {
				clearAll(event.getServer());
			}
			return;
		}
		if (FEARED.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Fear>> iterator = FEARED.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Fear> entry = iterator.next();
			Fear fear = entry.getValue();
			Entity entity = fear.level.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				ServerPlayer movedPlayer = event.getServer().getPlayerList().getPlayer(entry.getKey());
				if (movedPlayer != null) {
					clear(movedPlayer);
					iterator.remove();
					continue;
				}
				release(entity);
				iterator.remove();
				continue;
			}
			fear.ticks--;
			if (fear.ticks <= 0) {
				release(living);
				iterator.remove();
				continue;
			}
			drive(living);
		}
	}

	/** 生物档的每 tick 施加：无法攻击（清目标与寻路）+ 无法移动（水平速度清零） */
	private static void drive(LivingEntity living) {
		if (!(living instanceof Mob mob)) {
			return; // 玩家档靠属性，不需要逐 tick 驱动
		}
		mob.setTarget(null);
		if (mob instanceof NeutralMob neutral) {
			neutral.setPersistentAngerTarget(null);
		}
		mob.getNavigation().stop();
		mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0D);
		mob.setSpeed(0.0F);
		Vec3 velocity = mob.getDeltaMovement();
		mob.setDeltaMovement(0.0D, velocity.y, 0.0D);
		// 让客户端立刻收到这次速度变化（否则本地预测会继续滑一小段）
		mob.hurtMarked = true;
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && FEARED.containsKey(player.getUUID())) {
			event.setCanceled(true);
		}
	}

	/** 某些模组攻击不经过 AttackEntityEvent，统一在最终攻击事件再拦一道。 */
	@SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
	public static void onLivingAttack(LivingAttackEvent event) {
		if (event.getEntity().level().isClientSide()) {
			return;
		}
		Entity attacker = event.getSource().getEntity();
		if (attacker instanceof LivingEntity living && attacker != event.getEntity()
				&& FEARED.containsKey(living.getUUID())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		if (event.getEntity() instanceof ServerPlayer player && FEARED.containsKey(player.getUUID())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getEntity() instanceof ServerPlayer player && FEARED.containsKey(player.getUUID())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (event.getEntity() instanceof ServerPlayer player && FEARED.containsKey(player.getUUID())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		if (event.getEntity() instanceof ServerPlayer player && FEARED.containsKey(player.getUUID())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		clear(event.getEntity());
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)
				|| FEARED.containsKey(mob.getUUID())) {
			return;
		}
		CompoundTag persistent = mob.getPersistentData();
		if (persistent.contains(NO_AI_RESTORE)) {
			mob.setNoAi(persistent.getBoolean(NO_AI_RESTORE));
			persistent.remove(NO_AI_RESTORE);
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		clear(event.getEntity());
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		// 死亡即解除：避免复活后还带着"定身"
		if (!event.isCanceled()) {
			clear(event.getEntity());
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		clearAll(event.getServer());
	}

	/** 立刻解除某个目标的恐惧（死亡 / 登出 / 手动） */
	public static void clear(Entity entity) {
		if (entity == null) {
			return;
		}
		FEARED.remove(entity.getUUID());
		release(entity);
	}

	/** 自检用：判断目标是否仍登记为恐惧状态。 */
	public static boolean isFearedForTest(Entity entity) {
		return Boolean.getBoolean("summy_apocrypha.devcheck") && entity != null
				&& FEARED.containsKey(entity.getUUID());
	}

	private static void clearAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			clear(player);
		}
		// 生物可能不在玩家列表里，按登记关卡逐个恢复原始 noAI 值。
		for (Map.Entry<UUID, Fear> entry : new ArrayList<>(FEARED.entrySet())) {
			Entity entity = entry.getValue().level.getEntity(entry.getKey());
			if (entity != null) {
				clear(entity);
			}
		}
		FEARED.clear();
	}

	/** 表格移除时的收尾（实体可能已经读不到） */
	private static void release(Entity entity) {
		if (entity instanceof ServerPlayer player) {
			setPlayerFrozen(player, false);
		} else if (entity instanceof Mob mob) {
			CompoundTag persistent = mob.getPersistentData();
			if (persistent.contains(NO_AI_RESTORE)) {
				mob.setNoAi(persistent.getBoolean(NO_AI_RESTORE));
				persistent.remove(NO_AI_RESTORE);
			}
		}
	}

	/** 玩家档：把移动速度与攻速一起归零 / 复原 */
	private static void setPlayerFrozen(ServerPlayer player, boolean frozen) {
		applyZeroModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, "apocrypha_fear_speed", frozen);
		applyZeroModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER, "apocrypha_fear_attack_speed",
				frozen);
	}

	/** 挂 / 摘一条固定 UUID 的「归零」修饰符（写法照 SR 的 AttributeManager：先删后加，幂等） */
	private static void applyZeroModifier(ServerPlayer player, Attribute attribute, UUID uuid, String name,
			boolean frozen) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (instance.getModifier(uuid) != null) {
			instance.removeModifier(uuid);
		}
		if (frozen) {
			// 1.20.1 的 MULTIPLY_TOTAL：最终值 ×(1 + amount)，取 −1 即把结果钳到 0（与基础值无关）
			instance.addTransientModifier(new AttributeModifier(uuid, name, -1.0D,
					AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}
}
