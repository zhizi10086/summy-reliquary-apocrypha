package com.summy.reliquary.apocrypha.client;

import com.summy.reliquary.apocrypha.entity.HolyArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 圣光矢的渲染器：复用原版 {@link ArrowRenderer}，贴图换成**光灵箭**（spectral_arrow）。
 *
 * <p>原版光灵箭的外观就是「同一套箭矢模型 + spectral_arrow 贴图」，
 * 所以这里不需要自定义模型，也不会额外加拖尾粒子（ER 那边另用数据包标签退出了箭矢粒子接管）。
 */
public class HolyArrowRenderer extends ArrowRenderer<HolyArrow> {
	private static final ResourceLocation SPECTRAL_ARROW_TEXTURE =
			ResourceLocation.withDefaultNamespace("textures/entity/projectiles/spectral_arrow.png");

	public HolyArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(HolyArrow entity) {
		return SPECTRAL_ARROW_TEXTURE;
	}
}
