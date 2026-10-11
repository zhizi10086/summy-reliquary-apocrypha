package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.entity.NewTestamentArrow;
import com.summy.reliquary.entity.ThrownRazor;
import com.summy.reliquary.entity.ThrownSpear;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 「圣餐之矢」：放出箭矢时若光羽已攒到第 4 对，就消耗 1 对把这一发变成圣餐之矢。
//
// 三步：
//   ① ArrowLooseEvent（弓与弩都会触发）→ 检查门槛、消耗 1 对、登记"本玩家刚放箭"；
//   ② EntityJoinLevelEvent → 同一时间窗内该玩家新生成的箭打上标记（Forge 持久数据，服务端）；
//   ③ HIGHEST 的 LivingHurtEvent → 把该箭的伤害放大 30%（排在 SR 的 NORMAL 乘区之前，
//      所以后续增伤基于放大后的值，即"箭矢伤害本身 +30%"）。
public final class SacramentArrows {
	/** 箭上的标记键（Forge 持久数据，只在服务端读写） */
	private static final String MARK = "ApocryphaSacrament";
	/** 放箭后多久内新生成的箭算作"这一发"（tick）：弩要蓄力/放弦，留出余量 */
	private static final long MATCH_WINDOW_TICKS = 40L;

	private static final Map<UUID, Long> PENDING = new HashMap<>();

	private SacramentArrows() {
	}

	/** ① 放箭（弓与弩都会触发）：达到门槛就消耗 1 对并登记时间窗 */
	@SubscribeEvent
	public static void onArrowLoose(ArrowLooseEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		if (!ApocryphaConfig.enable() || !event.hasAmmo()) {
			return;
		}
		if (!CurioHelper.wears(player, Apocrypha.WING_FEATHER_SACRAMENT.get())) {
			return;
		}
		if (!FeatherCharge.canConsume(player)) {
			return;
		}
		FeatherCharge.consumePair(player);
		PENDING.put(player.getUUID(), player.serverLevel().getGameTime());
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info("[新约] 圣餐之矢：{} 消耗 1 对光羽（剩 {} 片）",
					player.getName().getString(), FeatherCharge.charge(player));
		}
	}

	/** ② 给时间窗内该玩家射出的箭打标记（只标记第一支，多重射击不会整梭变圣餐） */
	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()) {
			return;
		}
		if (!(event.getEntity() instanceof AbstractArrow arrow)) {
			return;
		}
		if (!(arrow.getOwner() instanceof ServerPlayer player)) {
			return;
		}
		Long looseTick = PENDING.get(player.getUUID());
		if (looseTick == null) {
			return;
		}
		long now = player.serverLevel().getGameTime();
		if (now - looseTick > MATCH_WINDOW_TICKS) {
			PENDING.remove(player.getUUID());
			return;
		}
		// SR 的幻影矛 / 金刀片与本模组落箭不算"弓箭射出的箭矢"
		if (arrow instanceof ThrownSpear || arrow instanceof ThrownRazor || arrow instanceof NewTestamentArrow) {
			return;
		}
		arrow.getPersistentData().putBoolean(MARK, true);
		PENDING.remove(player.getUUID());
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info("[新约] 圣餐之矢已就绪：{}（伤害 +{}%）",
					player.getName().getString(), ApocryphaConfig.sacramentDamageBonusPercent());
		}
	}

	/** 这一击是否来自圣餐之矢 */
	public static boolean isSacrament(DamageSource source) {
		return source != null
				&& source.getDirectEntity() instanceof AbstractArrow arrow
				&& arrow.getPersistentData().getBoolean(MARK);
	}

	/** ③ 伤害 +30%：HIGHEST —— 排在 SR 的增伤乘区（NORMAL）之前 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide() || event.isCanceled()) {
			return;
		}
		if (!isSacrament(event.getSource())) {
			return;
		}
		int bonus = ApocryphaConfig.sacramentDamageBonusPercent();
		if (bonus <= 0) {
			return;
		}
		event.setAmount(event.getAmount() * (1.0F + bonus / 100.0F));
	}

	/** 服务器停止 / 换存档：清掉待匹配登记 */
	public static void clear() {
		PENDING.clear();
	}
}
