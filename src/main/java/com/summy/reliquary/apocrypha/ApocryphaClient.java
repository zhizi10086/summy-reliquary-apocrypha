package com.summy.reliquary.apocrypha;

import com.summy.reliquary.apocrypha.client.NewTestamentArrowRenderer;
import com.summy.reliquary.apocrypha.client.HolyArrowRenderer;
import com.summy.reliquary.apocrypha.client.WingFeatherSacramentRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

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
		// 穿透圣光矢（光灵箭外观）
		event.registerEntityRenderer(Apocrypha.HOLY_ARROW.get(), HolyArrowRenderer::new);
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		// 光翼：照 SR「邦邦咔邦光环」的做法，用 Curios 的渲染回调画在玩家模型的躯干骨骼上（零延迟跟随）
		CuriosRendererRegistry.register(Apocrypha.WING_FEATHER_SACRAMENT.get(),
				WingFeatherSacramentRenderer::new);
		// 恶魔翅膀（坠羽）：同一套渲染层，参数与标志都按物品切换（见渲染器里的 demon 分支）
		CuriosRendererRegistry.register(Apocrypha.FALLEN_PLUME.get(),
				WingFeatherSacramentRenderer::new);
	}
}
