package com.summy.reliquary.apocrypha;

import com.summy.reliquary.apocrypha.client.NewTestamentArrowRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端入口：注册落箭实体的渲染器。
 *
 * <p>用 value = Dist.CLIENT 的注解订阅，FML 在专用服务端不会加载这个类
 * （与 SR 的 ReliquaryAttributes 同一套写法）。
 */
@Mod.EventBusSubscriber(modid = Apocrypha.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ApocryphaClient {
	private ApocryphaClient() {
	}

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		// 外观与原版箭矢一致（直接复用原版 ArrowRenderer，只指定贴图位置）
		event.registerEntityRenderer(Apocrypha.NEW_TESTAMENT_ARROW.get(), NewTestamentArrowRenderer::new);
	}
}
