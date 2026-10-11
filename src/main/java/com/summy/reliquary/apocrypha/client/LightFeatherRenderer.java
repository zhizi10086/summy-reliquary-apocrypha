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
 * 光羽（光翼）的渲染器：一对 = 同一张贴图的「原样 + 左右镜像」。
 *
 * <p>贴图是**完整的单边翅膀**（左下为翼根、右上为翼尖），所以：
 * <ul>
 *     <li><b>不做任何旋转</b> —— 旧版那种「上 / 中 / 下三态平面内 45° 展开」与配套的沿轴外移已全部退役；</li>
 *     <li><b>左片 = 右片左右镜像</b>（X 取负），于是两片从脊柱两侧对称地向外上方展开；</li>
 *     <li>四边形是**正方形**（边长 {@link #WING_SIZE} 格）并铺满整张画布 —— 只有正方形才能保住贴图的自然比例
 *     （翅膀内容在 32×32 画布里占 20×30 像素，画布左侧约 31% 的留白会成为两片之间的间隙）；</li>
 *     <li>枢轴取四边形的**底边内侧角**（贴图里翼根那一端），所以翼根贴着身体两侧、翼尖朝斜上外。</li>
 * </ul>
 *
 * <p>整对固定在"玩家背后的竖直平面"上（随玩家身体朝向旋转），用 {@code entityCutoutNoCull} 双面可见，
 * 因此从背后看是一对完整的翅膀、从侧面看是薄的。
 */
public class LightFeatherRenderer extends EntityRenderer<LightFeather> {
	private static final ResourceLocation TEXTURE =
			Apocrypha.id("textures/entity/light_feather.png");
	/** 四边形的边长（格）：正方形，整张画布铺满它（约 1.1 格 ≈ 半身大小） */
	private static final float WING_SIZE = 1.1F;
	/** 上下浮动的幅度（格）与频率：跟随是刚性贴合，"飘"的观感只由这里负责 */
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
		drawWing(pose, buffer, packedLight, false); // 右片（贴图原样）
		drawWing(pose, buffer, packedLight, true);  // 左片（左右镜像）
		pose.popPose();
	}

	/**
	 * 画一片翅膀：枢轴（翼根）在原点，四边形铺满整张画布、朝"右上"展开。
	 *
	 * <p>uv 对应关系（MC 的 v=0 是贴图顶部）：画布左下 = (0,1)、左上 = (0,0)、右上 = (1,0)、右下 = (1,1)。
	 */
	private static void drawWing(PoseStack pose, MultiBufferSource buffer, int packedLight, boolean mirror) {
		pose.pushPose();
		if (mirror) {
			pose.scale(-1.0F, 1.0F, 1.0F);
		}
		Matrix4f matrix = pose.last().pose();
		Matrix3f normal = pose.last().normal();
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		vertex(consumer, matrix, normal, 0.0F, 0.0F, packedLight, 0.0F, 1.0F);
		vertex(consumer, matrix, normal, 0.0F, WING_SIZE, packedLight, 0.0F, 0.0F);
		vertex(consumer, matrix, normal, WING_SIZE, WING_SIZE, packedLight, 1.0F, 0.0F);
		vertex(consumer, matrix, normal, WING_SIZE, 0.0F, packedLight, 1.0F, 1.0F);
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
