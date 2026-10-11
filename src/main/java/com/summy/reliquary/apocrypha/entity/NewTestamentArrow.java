package com.summy.reliquary.apocrypha.entity;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.effect.NewTestamentEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 「新约」落下的箭矢（服务端权威）。
 *
 * <p>结构与「命中后贴地停留再删除」的做法照搬 SR 的幻影矛 / 金刀片：
 * <ul>
 *     <li>拾取权限恒为 DISALLOWED、getPickupItem() 返回空栈，不可能被捡起来复制；</li>
 *     <li>命中实体：先清零目标无敌帧，再打我们自己命名的伤害类型（真实伤害），打完立即消失；</li>
 *     <li>命中方块：原地贴地停留 GROUND_STICK_TICKS tick 后删除；</li>
 *     <li>自管理寿命，超时强制清除，避免孤儿实体堆积。</li>
 * </ul>
 *
 * <p>伤害口径：固定 [new_testament] arrow_damage（默认 5），
 * 伤害类型是 summy-reliquary-apocrypha:new_testament_arrow，它的四个 bypasses_* 标签全开
 * → 无视护甲 / 保护附魔 / 抗性。伤害源的直接来源是本实体、造成者是玩家，因此：
 * <ul>
 *     <li>SR 会把这一击当作「玩家造成的远程伤害」，自动叠加七罪 / 七德与「全伤害最终倍率」乘区；</li>
 *     <li>本类型不在 SR 的 isDivine / isExactDamage 名单里，不会被 SR 当成自家特效伤害排除。</li>
 * </ul>
 *
 * <p>本实体不播放任何音效：听感反馈由 NewTestamentEffect 在散射触发时一次性给出。
 */
public class NewTestamentArrow extends AbstractArrow {
	// 以下两项是内部实现细节，从配置回调为硬编码（没人会去调，留在配置里只是噪音）
	/** 命中方块后贴地停留多久（tick）再删除 */
	private static final int GROUND_STICK_TICKS = 10;
	/** 最长寿命（tick）：超时强制清除，避免孤儿实体堆积 */
	private static final int MAX_LIFE_TICKS = 200;

	/** 是否已经落地（命中方块） */
	private boolean landed;
	/** 贴地停留计数 */
	private int stuckTicks;
	/** 飞行计数 */
	private int lifeTicks;

	public NewTestamentArrow(EntityType<? extends NewTestamentArrow> type, Level level) {
		super(type, level);
		this.pickup = Pickup.DISALLOWED;
	}

	/** 由散射生成：owner = 投出本体箭矢的玩家（出伤归属 + 圣心追踪的前提） */
	public NewTestamentArrow(Level level, LivingEntity shooter) {
		super(Apocrypha.NEW_TESTAMENT_ARROW.get(), shooter, level);
		this.pickup = Pickup.DISALLOWED;
		// 伤害完全由 onHitEntity 自己结算，不走原版箭矢的 baseDamage
		this.setBaseDamage(0.0D);
	}

	@Override
	protected ItemStack getPickupItem() {
		return ItemStack.EMPTY;
	}

	// ==================== 命中 ====================

	@Override
	protected void onHitEntity(EntityHitResult result) {
		if (!(level() instanceof ServerLevel serverLevel)
				|| !(getOwner() instanceof ServerPlayer shooter)
				|| !(result.getEntity() instanceof LivingEntity target)
				|| target == shooter) {
			discard();
			return;
		}
		// 无击退：原版 hurt 一旦落地就会按「造成者方向」施加击退，并附带水平速度减半与一点上浮。
		// 落箭是「散射落下」的表现，把目标推走会自我拆台，所以照 SR 神性光环的同一手法在结算前后保存/恢复速度。
		Vec3 velocityBefore = target.getDeltaMovement();
		float healthBefore = target.getHealth();
		// 无视无敌帧：先清零，保证这一箭不会被目标身上残留的无敌帧吞掉
		// （伤害减免交给伤害类型的 bypasses_* 标签）
		target.invulnerableTime = 0;
		boolean landedHit = target.hurt(damageSource(serverLevel, shooter), (float) ApocryphaConfig.arrowDamage());
		// 真伤不留帧：命中后再清一次，避免把无敌帧留给同批的其它落箭与随后落下的近战/箭矢。
		// 只在真的打中时清 —— 被免疫/没落地时不该剥掉目标自己的保护帧。
		if (landedHit) {
			// 先取一次「原版施加击退后」的速度作对照（日志里它与命中前速度的差就是被抵消掉的击退量）
			Vec3 velocityKnocked = target.getDeltaMovement();
			target.setDeltaMovement(velocityBefore);
			// 让客户端同步到恢复后的速度（否则本地仍会看到被推走）
			target.hurtMarked = true;
			target.invulnerableTime = 0;
			// 命中行放在这里打：只有此刻才拿得到「还原后」的真值
			NewTestamentEffect.logArrowHit(target, healthBefore, velocityBefore, velocityKnocked,
					target.getDeltaMovement());
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		// 不调用 super：原版会在这里播放落地音效并写 inGround，我们只要「钉住」
		landed = true;
		setDeltaMovement(Vec3.ZERO);
		setNoGravity(true);
	}

	@Override
	public void tick() {
		if (landed) {
			setDeltaMovement(Vec3.ZERO);
			if (++stuckTicks >= GROUND_STICK_TICKS) {
				discard();
			}
			return;
		}
		super.tick();
		if (++lifeTicks > MAX_LIFE_TICKS) {
			discard();
		}
	}

	/**
	 * 本模组命名的伤害源；数据包缺失（手动删掉了 damage_type 文件）时退回原版箭矢伤害源，
	 * 避免抛异常把整个世界 tick 打断。
	 */
	private DamageSource damageSource(ServerLevel level, ServerPlayer shooter) {
		var registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		var holder = registry.getHolder(
						ResourceKey.create(Registries.DAMAGE_TYPE, Apocrypha.id("new_testament_arrow")))
				.orElse(null);
		if (holder == null) {
			return level.damageSources().arrow(this, shooter);
		}
		// 直接来源 = 本实体（SR 据此按「远程」口径处理），造成者 = 玩家
		return new DamageSource(holder, this, shooter);
	}
}
