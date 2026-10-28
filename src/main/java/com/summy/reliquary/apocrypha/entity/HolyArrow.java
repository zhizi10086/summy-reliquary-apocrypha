package com.summy.reliquary.apocrypha.entity;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.effect.NewTestamentEffect;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 「比翼·连携」的穿透圣光矢（服务端权威）。
 *
 * <p><b>与 Summy Reliquary 的「圣光」（{@code summy-reliquary.holy_light} 饰品 / 命中光柱）无关</b>：
 * 这是本模组自建的投射物与伤害类型。
 *
 * <p>结构照 SR 的 {@code ThrownRazor} 与本模组的 {@link NewTestamentArrow}：
 * <ul>
 *     <li>拾取权限恒为 DISALLOWED、{@code getPickupItem()} 返回空栈，不可能被捡起来复制；</li>
 *     <li>走原版 {@link AbstractArrow} 的物理与碰撞，但**不调用** {@code super.onHitEntity} ——
 *     原版那条路径在"伤害为 0"时会把速度反弹回去（穿不透），所以这里自己写穿透记账；</li>
 *     <li>**无限穿透**：命中生物不消失、同一目标只吃一次（{@link #hitEntities} + {@link #canHitEntity}）；
 *     命中方块则贴地停留 {@value #GROUND_STICK_TICKS} tick 后消失；</li>
 *     <li>无重力（由发射方设置 {@code setNoGravity(true)}），另有寿命兜底 {@value #MAX_LIFE_TICKS} tick。</li>
 * </ul>
 *
 * <p>伤害口径：固定 {@code pinnedDamage}（= 触发之箭「各类倍率后」的金额 × 配置比例），
 * 伤害类型 {@code summy-reliquary-apocrypha:holy_arrow} 的四个 {@code bypasses_*} 标签全开
 * → 无视护甲 / 保护附魔 / 抗性；命中时先在 LOWEST 优先级把金额改回钉住值（不再吃第二次 SR 乘区），
 * 命中前后各清一次无敌帧，并还原目标速度实现**无击退**。
 */
public class HolyArrow extends AbstractArrow {
	/** 命中方块后贴地停留多久（tick）再删除 */
	private static final int GROUND_STICK_TICKS = 10;
	/** 最长寿命（tick）：超时强制清除，避免孤儿实体堆积（3 格/tick × 200 = 600 格） */
	private static final int MAX_LIFE_TICKS = 200;
	/** 本模组的伤害类型 id */
	private static final String DAMAGE_TYPE_ID = "holy_arrow";

	/** 钉住的单支伤害（&gt; 0 时生效） */
	private float pinnedDamage = -1.0F;
	/** 自己的穿透记账：已命中的目标不再参与碰撞检测 */
	private final IntOpenHashSet hitEntities = new IntOpenHashSet(8);
	/** 是否已经落地（命中方块） */
	private boolean landed;
	/** 贴地停留计数 */
	private int stuckTicks;
	/** 飞行计数 */
	private int lifeTicks;

	public HolyArrow(EntityType<? extends HolyArrow> type, Level level) {
		super(type, level);
		this.pickup = Pickup.DISALLOWED;
	}

	/** 由「比翼·连携」发射：owner = 射手玩家（出伤归属 + 远程口径） */
	public HolyArrow(Level level, LivingEntity shooter) {
		super(Apocrypha.HOLY_ARROW.get(), shooter, level);
		this.pickup = Pickup.DISALLOWED;
		// 伤害完全由 onHitEntity 自己结算，不走原版箭矢的 baseDamage
		this.setBaseDamage(0.0D);
	}

	@Override
	protected ItemStack getPickupItem() {
		return ItemStack.EMPTY;
	}

	/** 钉住的单支伤害 */
	public float pinnedDamage() {
		return pinnedDamage;
	}

	public void setPinnedDamage(float value) {
		this.pinnedDamage = value;
	}

	// ==================== 存档 ====================

	// 钉住值要随实体一起存取：区块卸载再加载时不能丢，否则回来就变成"没伤害"
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putFloat("ApocryphaPinnedDamage", pinnedDamage);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("ApocryphaPinnedDamage")) {
			pinnedDamage = tag.getFloat("ApocryphaPinnedDamage");
		}
		// 重新加载后同样不可拾取
		this.pickup = Pickup.DISALLOWED;
	}

	// ==================== 命中 ====================

	@Override
	protected void onHitEntity(EntityHitResult result) {
		if (!ApocryphaConfig.enable()) {
			discard();
			return;
		}
		// 穿透记账：同一个目标只吃一次；无限穿透，所以这里不设上限、也不消失
		Entity hit = result.getEntity();
		if (!hitEntities.add(hit.getId())) {
			return;
		}
		if (!(level() instanceof ServerLevel serverLevel)
				|| !(getOwner() instanceof ServerPlayer shooter)
				|| !(hit instanceof LivingEntity target)
				|| target == shooter) {
			return;
		}
		float damage = pinnedDamage > 0.0F ? pinnedDamage : 0.0F;
		if (damage <= 0.0F) {
			return;
		}
		// 无击退：与落箭同一手法，结算前后保存 / 还原目标速度
		Vec3 velocityBefore = target.getDeltaMovement();
		float healthBefore = target.getHealth();
		// 无视无敌帧：先清零，保证这一箭不会被目标身上残留的无敌帧吞掉
		target.invulnerableTime = 0;
		boolean landedHit = target.hurt(damageSource(serverLevel, shooter), damage);
		if (landedHit) {
			// 先取一次「原版施加击退后」的速度作对照（日志里它与命中前速度的差就是被抵消掉的击退量）
			Vec3 velocityKnocked = target.getDeltaMovement();
			target.setDeltaMovement(velocityBefore);
			target.hurtMarked = true;
			// 真伤不留帧：命中后再清一次
			target.invulnerableTime = 0;
			NewTestamentEffect.logArrowHit(target, healthBefore, velocityBefore, velocityKnocked,
					target.getDeltaMovement());
		}
		// 无限穿透：不 discard，继续飞
	}

	/** 同一个生物只吃一次：已命中的目标不再参与碰撞检测 */
	@Override
	protected boolean canHitEntity(Entity target) {
		return super.canHitEntity(target) && !hitEntities.contains(target.getId());
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
		if (!ApocryphaConfig.enable()) {
			discard();
			return;
		}
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
	 *
	 * <p>直接来源 = 本实体（SR 据此按「远程」口径处理），造成者 = 玩家。
	 */
	private DamageSource damageSource(ServerLevel level, ServerPlayer shooter) {
		var registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		var holder = registry.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, Apocrypha.id(DAMAGE_TYPE_ID)))
				.orElse(null);
		if (holder == null) {
			return level.damageSources().arrow(this, shooter);
		}
		return new DamageSource(holder, this, shooter);
	}

	// ==================== 自检接口 ====================

	/** 自检用：已经穿透命中了多少个目标 */
	public int piercedCount() {
		return hitEntities.size();
	}
}
