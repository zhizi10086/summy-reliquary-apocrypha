package com.summy.reliquary.apocrypha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.apocrypha.effect.FeatherCharge;
import com.summy.reliquary.apocrypha.effect.FallenPlumeWings;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * 光翼（翼羽·圣餐）的渲染器：**照 SR「邦邦咔邦光环」的做法**，用 Curios 的 {@link ICurioRenderer} 回调
 * 直接画在玩家模型的躯干骨骼上 —— 渲染随手模型当帧位姿，所以**零延迟**，还顺带跟随潜行前倾、骑乘偏移等体型动作。
 *
 * <p>与旧版（独立实体 + 服务端跟随）的区别：不再需要实体与位置同步；显隐由物品 NBT 上的
 * {@link FeatherCharge#WING_FLAG} 标志决定（服务端在攒满 3 对时写入，Curios 每 tick 会把饰品变化同步给客户端）。
 *
 * <p>两件饰品的贴图结构不同，因此两套画法：
 * <ul>
 *     <li><b>光翼（翼羽·圣餐）</b>：贴图是**完整的单边翅膀**（32×32），铺满一个正方形四边形，**不做任何旋转**、
 *     只用**左右镜像**构成一对；</li>
 *     <li><b>恶魔翅膀（坠羽）</b>：作者给的是**整对**（79×32：左翼 + 中间核心 + 右翼），所以画**一个矩形四边形**、
 *     uv 铺满、**不镜像也不内移**，按贴图原比例（79:32）居中于脊柱。</li>
 * </ul>
 * 坐标系里 **−Y 向上、+Z 朝玩家身后**（与 SR 光环渲染器同一套约定）。
 *
 * <p>★ 实机观感调节区：只改下面这几个常量即可挪动 / 收放翅膀。
 */
public class WingFeatherSacramentRenderer implements ICurioRenderer {
	/** 光翼（翼羽·圣餐）的贴图 */
	private static final ResourceLocation ANGEL_TEXTURE =
			Apocrypha.id("textures/entity/light_feather.png");
  /**
   * 恶魔翅膀（坠羽）的贴图：作者提供的**整对**（左翼 + 中间核心 + 右翼），79×32。
   *
   * <p>作者已经把左缘多出的 1px 裁掉，所以直接铺满 uv 0..1 就是居中的。
   */
	private static final ResourceLocation DEMON_TEXTURE =
			Apocrypha.id("textures/entity/demon_feather.png");

	/** 四边形边长（格）：正方形 + 整张画布铺满，才保得住贴图 20×30 内容的比例（1.25 格 ≈ 半身多一点） */
	private static final float WING_SIZE = 1.25F;
	/**
	 * 两片之间的**可视缝隙**（格）—— 想调宽窄就只改这一个数。
	 *
	 * <p>贴图内容只占画布右侧（左缘 10/32 处起），所以左右镜像后中间会自然留出空隙；
	 * 下面用 {@link #INNER_SHIFT} 把它收窄到本值。
	 */
	private static final float WING_GAP = 0.20F;
	/** 贴图内容左缘在四边形内的占比（画布 32 像素里左边 10 像素是留白） */
	private static final float TEXTURE_INNER_MARGIN = 10.0F / 32.0F;
	/**
	 * 每片沿脊柱方向内移的距离（格）：由目标缝隙反推。
	 *
	 * <p>尺寸是参数（两件饰品各一套），所以这个值必须跟着尺寸一起算。
	 */
	private static float innerShift(float size) {
		return TEXTURE_INNER_MARGIN * size - WING_GAP / 2.0F;
	}
	/** 整对向玩家身后偏移的距离（格） */
	private static final float BACK_OFFSET = 0.35F;
  /** 恶魔翅膀离背的距离（格）：比光翼略远，两件同戴时不会共面闪烁 */
	private static final float DEMON_BACK_OFFSET = 0.42F;
  /** 恶魔翅膀整对的宽度（格）—— 实机偏大 / 偏小只改这一个数 */
  private static final float DEMON_PAIR_WIDTH = 2.7F;
  /** 贴图原始宽高比（79 : 32），高度由宽度反推，保证不被拉伸 */
  private static final float DEMON_PAIR_ASPECT = 79.0F / 32.0F;
  /** 恶魔翅膀整对的高度（格） */
  private static final float DEMON_PAIR_HEIGHT = DEMON_PAIR_WIDTH / DEMON_PAIR_ASPECT;
	/**
	 * 翼根相对 <b>body 骨骼枢轴</b>"向下"偏移的距离（格，<b>正值向下</b>）—— 最常调的一个数。
	 *
	 * <p>依据：1.20.1 的 {@code HumanoidModel} 里 body 是 {@code PartPose.offset(0,0,0)}，即枢轴在
	 * <b>肩线（世界 y ≈ 1.5 格）</b>而不是腰部；该骨骼空间里 {@code world_y = 1.501 − y_local}，
	 * 所以 <b>+y 向下、−y 向上</b>（与 SR 光环「从头部枢轴 −0.50 正好到头顶」这条已验证的参照一致）。
	 * 取 0.85 → 翼根基准落在 1.5 − 0.85 = <b>0.65 格</b>（背中段）；叠上 ±0.05 的上下浮动后实际区间约 0.60 ~ 0.70 格
	 * （再往上一轮，最低时偏低，故整体抬高 0.03 格）。
	 */
	private static final float WING_ROOT_DROP = 0.85F;
	/** 上下浮动的幅度（格）与频率：逐帧平滑，靠回调里的 ageInTicks 驱动 */
	private static final float BOB_AMPLITUDE = 0.05F;
	private static final float BOB_SPEED = 0.1F;

	@Override
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
			PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffer, int light,
			float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
			float headPitch) {
		// 两件饰品共用这一套渲染：光翼看「攒满 3 对」的标志，恶魔翅膀（坠羽）看爆条时写的标志
		boolean demon = stack.is(Apocrypha.FALLEN_PLUME.get());
		String flag = demon ? FallenPlumeWings.FLAG : FeatherCharge.WING_FLAG;
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.getBoolean(flag)) {
			return;
		}
		// 只有人形模型（玩家、僵尸这类）才有躯干骨骼
		if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoid)) {
			return;
		}
		float backOffset = demon ? DEMON_BACK_OFFSET : BACK_OFFSET;
		ResourceLocation texture = demon ? DEMON_TEXTURE : ANGEL_TEXTURE;

		poseStack.pushPose();
		// 挂到躯干骨骼：跟随身体朝向、潜行前倾、骑乘偏移等（该空间里 −Y 向上、+Z 朝身后）
		humanoid.body.translateAndRotate(poseStack);
		// 先从肩线向下落到背中段（该空间 +y 向下），再退到背后
		poseStack.translate(0.0D, WING_ROOT_DROP, backOffset);
		// 轻微上下浮动：相对背部上下，逐帧平滑
		float bob = Mth.sin(ageInTicks * BOB_SPEED) * BOB_AMPLITUDE;
		poseStack.translate(0.0D, -bob, 0.0D);
    if (demon) {
      // 整对贴图：一个矩形四边形居中于脊柱，uv 铺满、不镜像
      drawPair(poseStack, buffer, light, texture);
    } else {
      // 单边贴图：正方形四边形 + 左右镜像成一对
      float shift = innerShift(WING_SIZE);
      drawWing(poseStack, buffer, light, false, texture, WING_SIZE, shift); // 右片（贴图原样）
      drawWing(poseStack, buffer, light, true, texture, WING_SIZE, shift);  // 左片（左右镜像）
    }
		poseStack.popPose();
	}

  /**
   * 画**整对**翅膀（恶魔线）：一个矩形四边形，底边过原点、向 −Y 展开，水平居中于脊柱。
   *
   * <p>uv 与 {@link #drawWing} 同一套约定：底边 v=1、顶边 v=0（v=0 是贴图顶部，所以画出来是正的）。
   */
  private static void drawPair(PoseStack poseStack, MultiBufferSource buffer, int light,
      ResourceLocation texture) {
    float halfWidth = DEMON_PAIR_WIDTH / 2.0F;
    Matrix4f matrix = poseStack.last().pose();
    Matrix3f normal = poseStack.last().normal();
    VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
    vertex(consumer, matrix, normal, -halfWidth, 0.0F, light, 0.0F, 1.0F);
    vertex(consumer, matrix, normal, -halfWidth, -DEMON_PAIR_HEIGHT, light, 0.0F, 0.0F);
    vertex(consumer, matrix, normal, halfWidth, -DEMON_PAIR_HEIGHT, light, 1.0F, 0.0F);
    vertex(consumer, matrix, normal, halfWidth, 0.0F, light, 1.0F, 1.0F);
  }

	/**
	 * 画一片翅膀：枢轴（翼根）在原点，四边形朝"上"（该空间里的 −Y）展开。
	 *
	 * <p>uv 对应：枢轴一角取 (0,1)、沿 −Y 到顶边取 (0,0)、顶边外角 (1,0)、外下角 (1,1)
	 * —— v=0 是贴图顶部，所以画出来是正的。
	 */
	private static void drawWing(PoseStack poseStack, MultiBufferSource buffer, int light, boolean mirror,
			ResourceLocation texture, float size, float shift) {
		poseStack.pushPose();
		if (mirror) {
			poseStack.scale(-1.0F, 1.0F, 1.0F);
		}
		// 镜像之后施加同一个内移，两片会自动对称地朝脊柱收拢
		poseStack.translate(-shift, 0.0D, 0.0D);
		Matrix4f matrix = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
		vertex(consumer, matrix, normal, 0.0F, 0.0F, light, 0.0F, 1.0F);
		vertex(consumer, matrix, normal, 0.0F, -size, light, 0.0F, 0.0F);
		vertex(consumer, matrix, normal, size, -size, light, 1.0F, 0.0F);
		vertex(consumer, matrix, normal, size, 0.0F, light, 1.0F, 1.0F);
		poseStack.popPose();
	}

	private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y,
			int light, float u, float v) {
		consumer.vertex(matrix, x, y, 0.0F)
				.color(255, 255, 255, 255)
				.uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(light)
				// 本地 +Z 指向玩家身后，法线朝后朝向观察者
				.normal(normal, 0.0F, 0.0F, 1.0F)
				.endVertex();
	}
}
