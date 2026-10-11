package com.summy.reliquary.apocrypha.client;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.net.AirJumpMessage;
import com.summy.reliquary.apocrypha.net.ApocryphaNetworking;
import com.summy.reliquary.util.CurioHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端输入上报：「逆羽·滞空」的额外跳跃。
 *
 * <p>原版的「跳跃键」输入状态存在 {@code LivingEntity#jumping} 这个 protected 字段里，服务端没有公开读取方式，
 * 所以这里在客户端做**按下沿检测**（未按 → 按住的那一 tick），只在佩戴饰品时上报一次；
 * 服务端收到后会再做完整校验（是否离地、本段是否还有剩余跳数）。
 *
 * <p>用 FORGE 总线订阅（客户端 tick 属于游戏事件总线），value = Dist.CLIENT 保证专用服务端不加载本类。
 */
@Mod.EventBusSubscriber(modid = Apocrypha.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ApocryphaClientInput {
	/** 上一 tick 跳跃键是否按住（用于检测按下沿） */
	private static boolean jumpWasDown;

	private ApocryphaClientInput() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null) {
			jumpWasDown = false;
			return;
		}
		boolean down = minecraft.options.keyJump.isDown();
		if (down && !jumpWasDown && CurioHelper.wears(minecraft.player, Apocrypha.INVERSE_FEATHER_HOVER.get())) {
			ApocryphaNetworking.CHANNEL.sendToServer(new AirJumpMessage());
		}
		jumpWasDown = down;
	}
}
