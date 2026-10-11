package com.summy.reliquary.apocrypha.client;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.effect.HoverFlight;
import com.summy.reliquary.apocrypha.net.AirJumpMessage;
import com.summy.reliquary.apocrypha.net.ApocryphaNetworking;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端输入：「逆羽·滞空」的额外跳跃（输入状态机参考 Artifacts 的「云朵瓶」）。
 *
 * <p>原版的跳跃键状态存在 {@code LivingEntity#jumping} 这个 protected 字段里，服务端没有公开读取方式，
 * 所以这里在客户端判定、再上报服务端做权威结算。
 *
 * <p>两个状态位是手感的关键：
 * <ul>
 *     <li>{@code canAirJump}：落地 / 攀爬时补满，用掉一次后置假 —— 一段离地只给一次；</li>
 *     <li>{@code hasReleasedJumpKey}：落地 / 攀爬时置假，**空中松开过跳跃键**才置真 ——
 *     用来排除「地面起跳的那一次按键」，否则那一次就会把这段离地的额外跳跃提前吃掉。</li>
 * </ul>
 *
 * <p>触发时先本地施加一次（{@link HoverFlight#jump} 客户端预测，立刻起效、无延迟），
 * 同时发包让服务端做同样的权威施加；服务端收到后仍会完整校验一次。
 *
 * <p>用 FORGE 总线订阅（客户端 tick 属于游戏事件总线），value = Dist.CLIENT 保证专用服务端不加载本类。
 */
@Mod.EventBusSubscriber(modid = Apocrypha.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ApocryphaClientInput {
	/** 本段离地是否还有可用的额外跳跃 */
	private static boolean canAirJump;
	/** 离开地面之后是否松开过跳跃键（用来排除地面起跳的那次按键） */
	private static boolean hasReleasedJumpKey;

	private ApocryphaClientInput() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			canAirJump = false;
			hasReleasedJumpKey = false;
			return;
		}
		// 落地或攀爬：视为一段新的离地
		if (player.onGround() || player.onClimbable()) {
			canAirJump = true;
			hasReleasedJumpKey = false;
			return;
		}
		// 空中松开跳跃键 → 之后的那次按下才算一次"额外的跳跃"
		if (!player.input.jumping) {
			hasReleasedJumpKey = true;
			return;
		}
		if (!canAirJump || !hasReleasedJumpKey || player.getAbilities().flying) {
			return;
		}
		// 客户端先自查一次（服务端还会再校验），避免对没戴饰品/不可用的玩家发无用的包
		if (!CurioHelper.wears(player, Apocrypha.INVERSE_FEATHER_HOVER.get())) {
			return;
		}
		canAirJump = false;
		ApocryphaNetworking.CHANNEL.sendToServer(new AirJumpMessage());
		// 本地预测：立刻起效，不用等服务端回包
		HoverFlight.jump(player);
	}
}
