package com.summy.reliquary.apocrypha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.config.ApocryphaConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 「凋亡损伤条」画在目标头顶（纯代码、不需要贴图）。
 *
 * <p>朝向：**完全正对相机**（先按相机水平朝向绕 Y，再按俯仰绕 X；等价于名称牌的 billboard，但轴向明确可控：
 * 该局部坐标系里 **+x = 屏幕右、+y = 屏幕上**）。遮挡：不关深度测试，与名称牌一致 —— 被方块挡住就看不见。
 *
 * <p>三段共用一条：
 * <ul>
 *     <li>累积中：从左向右生长，暗血红；</li>
 *     <li>损伤状态：从满线性回退，亮血红并轻微脉动；</li>
 *     <li>损伤抵抗：显示抵抗剩余时间，青灰。</li>
 * </ul>
 * 叠层顺序（各自带一点 z 偏移避免共面闪烁）：底槽 → 填充 → 刻度 → 描边。
 *
 * <p>数据来自 {@link ApoptosisBarState}（由服务端的凋亡同步包写入），客户端每 tick 本地衰减。
 */
@Mod.EventBusSubscriber(modid = Apocrypha.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ApoptosisBarRenderer {
	/** 条宽 / 条高（格） */
	private static final float BAR_WIDTH = 0.9F;
	private static final float BAR_HEIGHT = 0.10F;
	/** 条底边距目标碰撞箱顶部的高度（格） */
	private static final float ANCHOR_ABOVE = 0.55F;
	/** 刻度线宽度（格）：共 5 条，切出 6 段 */
	private static final float TICK_WIDTH = 0.008F;
	/** 描边厚度（格） */
	private static final float BORDER = 0.012F;
	/** 每层之间的 z 偏移，避免共面闪烁 */
	private static final float LAYER_STEP = 0.002F;
	/** 损伤状态脉动：周期（tick）与幅度（alpha 只在 1−幅度 ~ 1 之间波动） */
	private static final float PULSE_TICKS = 10.0F;
	private static final float PULSE_AMPLITUDE = 0.15F;

	// 配色（与 README 的色表一致）
	private static final int COL_BG = 0x140A0C;
	private static final int COL_BORDER = 0x000000;
	private static final int COL_TICK = 0x000000;
	private static final int COL_ACCUM = 0x7A1220;
	private static final int COL_INJURED = 0xE0203A;
	private static final int COL_RESIST = 0x5A6B7A;

	private ApoptosisBarRenderer() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (!ApocryphaConfig.enable()) {
			ApoptosisBarState.clear();
			return;
		}
		ApoptosisBarState.tick(Minecraft.getInstance().level, ApocryphaConfig.apoptosisBarMax(),
				ApocryphaConfig.apoptosisInjuredSeconds() * 20);
	}

	/** 退出世界时清空缓存，避免残留到下一个存档 */
	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		ApoptosisBarState.clear();
	}

	@SuppressWarnings("rawtypes")
	@SubscribeEvent
	public static void onRenderLiving(RenderLivingEvent.Post event) {
		// 原始类型下 getEntity() 已经是 LivingEntity
		LivingEntity living = event.getEntity();
		if (living == null) {
			return;
		}
		ApoptosisBarState.Entry entry = ApoptosisBarState.get(living.getId());
		if (entry == null) {
			return;
		}
		double barMax = Math.max(1.0D, ApocryphaConfig.apoptosisBarMax());
		boolean injured = entry.injuredTicks > 0;
		boolean resisting = !injured && entry.resistTicks > 0;
		float progress;
		int color;
		int alpha = 255;
		if (injured) {
			progress = (float) Mth.clamp(entry.value / barMax, 0.0D, 1.0D);
			color = COL_INJURED;
			// 轻微脉动：只在 1−幅度 ~ 1 之间波动
			float phase = Mth.sin((living.tickCount + event.getPartialTick())
					* (float) (Math.PI * 2.0D / PULSE_TICKS));
			alpha = (int) (255.0F * (1.0F - PULSE_AMPLITUDE * 0.5F * (1.0F - phase)));
		} else if (resisting) {
			int total = Math.max(1, ApocryphaConfig.apoptosisResistanceSeconds() * 20);
			progress = Mth.clamp(entry.resistTicks / (float) total, 0.0F, 1.0F);
			color = COL_RESIST;
		} else {
			progress = (float) Mth.clamp(entry.value / barMax, 0.0D, 1.0D);
			color = COL_ACCUM;
		}
		if (progress <= 0.0F && !injured && !resisting) {
			return;
		}

		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		PoseStack pose = event.getPoseStack();
		pose.pushPose();
		pose.translate(0.0D, living.getBbHeight() + ANCHOR_ABOVE, 0.0D);
		// billboard：先对相机水平朝向，再按俯仰倾斜（该局部空间 +x = 屏幕右、+y = 屏幕上）
		pose.mulPose(Axis.YP.rotationDegrees(180.0F - camera.getYRot()));
		pose.mulPose(Axis.XP.rotationDegrees(-camera.getXRot()));
		VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.debugQuads());
		Matrix4f matrix = pose.last().pose();

		float left = -BAR_WIDTH / 2.0F;
		float right = BAR_WIDTH / 2.0F;
		float bottom = -BAR_HEIGHT / 2.0F;
		float top = BAR_HEIGHT / 2.0F;

		// ① 底槽（半透明近黑）
		quad(consumer, matrix, left, bottom, right, top, 0.0F, COL_BG, 170);
		// ② 填充（按阶段配色，内缩 1px 让描边压得住）
		float filled = left + BAR_WIDTH * progress;
		if (filled > left) {
			quad(consumer, matrix, left + BORDER, bottom + BORDER, filled, top - BORDER, LAYER_STEP, color, alpha);
		}
		// ③ 刻度：5 条竖线切出 6 段
		float tickZ = LAYER_STEP * 2.0F;
		for (int index = 1; index <= 5; index++) {
			float x = left + BAR_WIDTH * index / 6.0F;
			quad(consumer, matrix, x - TICK_WIDTH / 2.0F, bottom, x + TICK_WIDTH / 2.0F, top, tickZ,
					COL_TICK, 130);
		}
		// ④ 描边（四条边，不透明纯黑）
		float borderZ = LAYER_STEP * 3.0F;
		quad(consumer, matrix, left, bottom, right, bottom + BORDER, borderZ, COL_BORDER, 255);
		quad(consumer, matrix, left, top - BORDER, right, top, borderZ, COL_BORDER, 255);
		quad(consumer, matrix, left, bottom, left + BORDER, top, borderZ, COL_BORDER, 255);
		quad(consumer, matrix, right - BORDER, bottom, right, top, borderZ, COL_BORDER, 255);
		pose.popPose();
	}

	/** 画一个四边形（POSITION_COLOR，z 相同即可；颜色为 0xRRGGBB + 独立 alpha） */
	private static void quad(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float x1, float y1,
			float z, int rgb, int alpha) {
		int red = (rgb >> 16) & 0xFF;
		int green = (rgb >> 8) & 0xFF;
		int blue = rgb & 0xFF;
		consumer.vertex(matrix, x0, y0, z).color(red, green, blue, alpha).endVertex();
		consumer.vertex(matrix, x0, y1, z).color(red, green, blue, alpha).endVertex();
		consumer.vertex(matrix, x1, y1, z).color(red, green, blue, alpha).endVertex();
		consumer.vertex(matrix, x1, y0, z).color(red, green, blue, alpha).endVertex();
	}
}
