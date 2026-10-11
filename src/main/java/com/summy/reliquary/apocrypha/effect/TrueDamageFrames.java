package com.summy.reliquary.apocrypha.effect;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import com.summy.reliquary.apocrypha.integration.ReliquaryRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// 给 SR 自带「无视无敌帧」的真伤补上「命中后清帧」——纯本模组侧实现，不改 SR 一行。
//
// SR 的这些真伤只在命中前清一次 invulnerableTime，命中后原版仍会把字段写成「窗口 + 10」
// （SR 的 CombatTuning 只是把它改写成 5 tick 级窗口）。于是它们会给受害者留下几帧保护，
// 既会挡住同批的后续段落，也会挡住随后落下来的近战/箭矢。这里在落地后再清一次，做到「真伤不留帧」。
//
// 两条路径（与 SR 自己的双路径分工一致）：
//   Forge：原版在进入 actuallyHurt 之前就写好了字段，事件里清零后同一次调用里没人再改它 -> 就地清零；
//   Kilt/Connector：LivingHurtEvent 发生在 hurt 开头（字段还没写），就地写会被随后覆盖 -> 登记后 tick 末尾清零。
public final class TrueDamageFrames {
	// 推迟到 tick 末尾清帧的受害者（Kilt 侧用）
	private static final List<PendingClear> DEFERRED = new ArrayList<>();
	// 已经提醒过的「未登记 SR 伤害类型」，避免每次命中都刷日志
	private static final Set<String> WARNED = new HashSet<>();

	private TrueDamageFrames() {
	}

	// 一条推迟到 tick 末尾的清帧登记
	private record PendingClear(ServerLevel level, UUID victim) {
	}

	// 在 LivingHurtEvent（LOWEST）里调用；只处理真正落地的 SR 真伤
	public static void handle(LivingHurtEvent event, ServerLevel level) {
		String msgId = event.getSource().getMsgId();
		if (!ReliquaryRef.isTrueDamage(event.getSource())) {
			// 名单漂移兜底：SR 出现了新的伤害类型而我们没登记 -> 只提醒一次（且只在调试日志打开时）
			if (ApocryphaConfig.logTrigger() && ReliquaryRef.looksLikeSrDamage(msgId) && WARNED.add(msgId)) {
				Apocrypha.LOGGER.info("[新约] 清帧名单可能已漂移：SR 的伤害类型 {} 未登记，本次按不处理放行", msgId);
			}
			return;
		}
		LivingEntity victim = event.getEntity();
		boolean fabric = ReliquaryRef.fabricSide();
		if (ApocryphaConfig.logTrigger()) {
			Apocrypha.LOGGER.info("[新约] 清帧：来源 {}、受害者 {}、通道 {}",
					msgId, victim.getName().getString(), fabric ? "Kilt" : "Forge");
		}
		if (fabric) {
			DEFERRED.add(new PendingClear(level, victim.getUUID()));
		} else {
			victim.invulnerableTime = 0;
		}
	}

	// 服务端 tick 末尾：把 Kilt 侧登记的受害者统一清零（此刻原版已经写完字段，不会再被覆盖）
	public static void tickServer() {
		if (DEFERRED.isEmpty()) {
			return;
		}
		for (PendingClear pending : DEFERRED) {
			if (pending.level().getEntity(pending.victim()) instanceof LivingEntity living) {
				living.invulnerableTime = 0;
			}
		}
		DEFERRED.clear();
	}

	// 服务器停止：清空登记与去重表
	public static void clear() {
		DEFERRED.clear();
		WARNED.clear();
	}
}
