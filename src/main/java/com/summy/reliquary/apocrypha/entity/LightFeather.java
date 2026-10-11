package com.summy.reliquary.apocrypha.entity;

import com.summy.reliquary.apocrypha.Apocrypha;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 一对「光羽」：悬浮在玩家背后、随移动飘动。**每对一个实体**，每名玩家最多同时存在 3 个（第 4 对只计数不显示）。
 *
 * <p>服务端负责跟随与朝向（客户端只按实体位置/朝向渲染，因此不需要任何同步字段）；
 * 渲染器按 {@link #tier()} 决定平面内旋转角（上对 0°、中对顺时针 45°、下对累计 90°）。
 */
public class LightFeather extends Entity {
	/**
	 * 层号（0 上 / 1 中 / 2 下）：**必须同步给客户端** —— 渲染器要据此决定平面内旋转角
	 * （0° / 顺时针 45° / 累计顺时针 90°）。之前只放在服务端字段里，客户端恒为 0，
	 * 于是三对都按 0° 画、看起来"没有旋转"。
	 */
	private static final EntityDataAccessor<Integer> DATA_TIER =
			SynchedEntityData.defineId(LightFeather.class, EntityDataSerializers.INT);

	/** 背后的水平偏移（沿玩家朝向反方向，格） */
	private static final double BACK_OFFSET = 0.35D;
	/** 三层的竖直锚点高度（相对玩家脚底，格）：0 上 / 1 中 / 2 下 */
	private static final double[] TIER_HEIGHT = {1.45D, 1.05D, 0.65D};
	/** 跟随强度：每 tick 朝锚点插值的比例（1.0 = 完全刚性贴合；"飘"由渲染端的上下浮动负责） */
	private static final double FOLLOW_LERP = 1.0D;

	// 服务端字段（不属于同步内容：只有服务端需要用它找主人）
	private UUID ownerId;

	public LightFeather(EntityType<? extends LightFeather> type, Level level) {
		super(type, level);
		this.noCulling = true;
		this.setNoGravity(true);
	}

	public LightFeather(Level level, ServerPlayer owner, int tier) {
		this(Apocrypha.LIGHT_FEATHER.get(), level);
		this.ownerId = owner.getUUID();
		setTier(tier);
		Vec3 anchor = anchorOf(owner, tier());
		this.setPos(anchor.x, anchor.y, anchor.z);
		this.setYRot(owner.yBodyRot);
	}

	/** 第几层：0 上 / 1 中 / 2 下（同步值，客户端渲染器同样读它） */
	public int tier() {
		return entityData.get(DATA_TIER);
	}

	public void setTier(int value) {
		entityData.set(DATA_TIER, clampTier(value));
	}

	public UUID ownerId() {
		return ownerId;
	}

	private static int clampTier(int value) {
		return Math.max(0, Math.min(TIER_HEIGHT.length - 1, value));
	}

	/** 该层在玩家背后的锚点（玩家朝向的反方向偏移 + 该层层高） */
	private static Vec3 anchorOf(ServerPlayer owner, int tier) {
		Vec3 look = Vec3.directionFromRotation(0.0F, owner.yBodyRot);
		return new Vec3(
				owner.getX() - look.x * BACK_OFFSET,
				owner.getY() + TIER_HEIGHT[clampTier(tier)],
				owner.getZ() - look.z * BACK_OFFSET);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			return; // 位置/朝向由服务端同步
		}
		if (!(level() instanceof ServerLevel serverLevel)) {
			discard();
			return;
		}
		Entity ownerEntity = ownerId == null ? null : serverLevel.getEntity(ownerId);
		if (!(ownerEntity instanceof ServerPlayer owner) || !owner.isAlive()) {
			discard(); // 主人不在了（死亡 / 退出 / 换维度）→ 自行清理
			return;
		}
		Vec3 target = anchorOf(owner, tier());
		Vec3 current = position();
		Vec3 next = current.add(target.subtract(current).scale(FOLLOW_LERP));
		setPos(next.x, next.y, next.z);
		setYRot(owner.yBodyRot);
		setXRot(0.0F);
		// 每 tick 让服务端把位置/朝向同步出去，客户端才能看到平滑跟随
		hasImpulse = true;
	}

	// ==================== 存档（实体本身 noSave，这里只做兜底） ====================

	@Override
	protected void defineSynchedData() {
		entityData.define(DATA_TIER, 0);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.hasUUID("Owner")) {
			ownerId = tag.getUUID("Owner");
		}
		setTier(tag.getInt("Tier"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		if (ownerId != null) {
			tag.putUUID("Owner", ownerId);
		}
		tag.putInt("Tier", tier());
	}

	// ==================== 不参与任何交互 ====================

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}
}
