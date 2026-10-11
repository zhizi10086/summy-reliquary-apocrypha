package com.summy.reliquary.apocrypha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.entity.LightFeather;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 光羽的渲染器：一对 = 两片镜像羽毛，构成向外张开的 V。
 *
 * <p>贴图是「左下（羽根）→ 右上（尖端）」的 45° 斜向，所以：
 * <ul>
 *     <li><b>上对（tier 0）</b>：贴图原样 → 斜向上外 45°；</li>
 *     <li><b>中对（tier 1）</b>：平面内顺时针 45° → 水平外张；</li>
 *     <li><b>下对（tier 2）</b>：平面内累计顺时针 90° → 向下外张；</li>
 *     <li><b>左片</b> = 右片左右镜像（X 取负）后再用同一角度绘制。</li>
 * </ul>
 *
 * <p>整对固定在"玩家背后的竖直平面"上（随玩家身体朝向旋转），用 {@code entityCutoutNoCull} 双面可见，
 * 因此从背后看是完整的 V、从侧面看是薄的。
 */
public class LightFeatherRenderer extends EntityRenderer<LightFeather> {
	private static final ResourceLocation TEXTURE =
			Apocrypha.id("textures/entity/light_feather.png");
	/** 每片羽毛的边长（格） */
	private static final float FEATHER_SIZE = 0.55F;
	/** 上下浮动的幅度（格）与频率 */
	private static final float BOB_AMPLITUDE = 0.05F;
	private static final float BOB_SPEED = 0.1F;

	public LightFeatherRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(LightFeather entity) {
		return TEXTURE;
	}

	@Override
	public void render(LightFeather entity, float entityYaw, float partialTick, PoseStack pose,
			MultiBufferSource buffer, int packedLight) {
		pose.pushPose();
		// 转到"背后平面"：实体 yaw 已同步成玩家身体朝向，这里让平面法线朝向玩家正前方
		pose.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
		// 轻微的上下浮动（"随移动飘动"的观感）
		float bob = Mth.sin((entity.tickCount + partialTick) * BOB_SPEED) * BOB_AMPLITUDE;
		pose.translate(0.0D, bob, 0.0D);
		// 逐层顺时针 45°：上对 0°、中对 45°、下对 90°
		float tilt = entity.tier() * 45.0F;
		drawFeather(pose, buffer, packedLight, tilt, false); // 右片
		drawFeather(pose, buffer, packedLight, tilt, true);  // 左片（左右镜像）
		pose.popPose();
	}

	/** 画一片羽毛：锚点（羽根）在原点，四边形向右上铺开，贴图的对角线正好形成张开角 */
	private static void drawFeather(PoseStack pose, MultiBufferSource buffer, int packedLight, float tilt,
			boolean mirror) {
		pose.pushPose();
		// MC 的 Z 轴正角是逆时针，所以要"顺时针"取负
		pose.mulPose(Axis.ZP.rotationDegrees(-tilt));
		if (mirror) {
			pose.scale(-1.0F, 1.0F, 1.0F);
		}
		Matrix4f matrix = pose.last().pose();
		Matrix3f normal = pose.last().normal();
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		// 羽根在左下角（uv 0,1）、尖端在右上角（uv 1,0）
		vertex(consumer, matrix, normal, 0.0F, -FEATHER_SIZE, packedLight, 0.0F, 1.0F);
		vertex(consumer, matrix, normal, 0.0F, 0.0F, packedLight, 0.0F, 0.0F);
		vertex(consumer, matrix, normal, FEATHER_SIZE, 0.0F, packedLight, 1.0F, 0.0F);
		vertex(consumer, matrix, normal, FEATHER_SIZE, -FEATHER_SIZE, packedLight, 1.0F, 1.0F);
		pose.popPose();
	}

	private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y,
			int packedLight, float u, float v) {
		consumer.vertex(matrix, x, y, 0.0F)
				.color(255, 255, 255, 255)
				.uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(packedLight)
				.normal(normal, 0.0F, 0.0F, 1.0F)
				.endVertex();
	}
}
