package com.summy.reliquary.apocrypha.client;

import com.summy.reliquary.apocrypha.entity.NewTestamentArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 落箭的渲染器：直接复用原版 ArrowRenderer（外观与原版箭矢完全一致），
 * 只把贴图位置固定成原版箭矢贴图。
 */
public class NewTestamentArrowRenderer extends ArrowRenderer<NewTestamentArrow> {
	private static final ResourceLocation ARROW_TEXTURE =
			ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

	public NewTestamentArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(NewTestamentArrow entity) {
		return ARROW_TEXTURE;
	}
}
