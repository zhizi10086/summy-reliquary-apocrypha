package com.summy.reliquary.apocrypha.integration;

import com.summy.reliquary.apocrypha.Apocrypha;
import net.minecraft.world.entity.projectile.AbstractArrow;

import java.lang.reflect.Method;

/**
 * Summy Enchantment 的可选兼容入口。
 *
 * <p>SE 不是本模组的必需前置，因此这里只反射调用其公开 API；
 * 未安装 SE、旧版本缺少方法或调用失败时统一按 1.0 倍处理。
 */
public final class SummyEnchantmentRef {
	private static final String API_CLASS = "com.summy.enchantment.api.SummyEnchantmentApi";
	private static Method deathEyeMultiplier;
	private static boolean lookedUp;
	private static boolean warned;

	private SummyEnchantmentRef() {
	}

	public static float deathEyeMultiplier(AbstractArrow arrow) {
		if (arrow == null) {
			return 1.0F;
		}
		Method method = method();
		if (method == null) {
			return 1.0F;
		}
		try {
			Object value = method.invoke(null, arrow);
			if (value instanceof Number number) {
				float multiplier = number.floatValue();
				return Float.isFinite(multiplier) && multiplier >= 1.0F ? multiplier : 1.0F;
			}
		} catch (ReflectiveOperationException | LinkageError exception) {
			warnOnce(exception);
		}
		return 1.0F;
	}

	private static synchronized Method method() {
		if (lookedUp) {
			return deathEyeMultiplier;
		}
		lookedUp = true;
		Class<?> apiClass;
		try {
			apiClass = Class.forName(API_CLASS);
		} catch (ClassNotFoundException ignored) {
			// SE 未安装是正常情况，不向普通玩家输出兼容警告。
			return null;
		} catch (LinkageError exception) {
			warnOnce(exception);
			return null;
		}
		try {
			deathEyeMultiplier = apiClass.getMethod("arrowDeathEyeMultiplier", AbstractArrow.class);
		} catch (NoSuchMethodException | SecurityException exception) {
			warnOnce(exception);
		}
		return deathEyeMultiplier;
	}

	private static void warnOnce(Throwable throwable) {
		if (!warned) {
			warned = true;
			Apocrypha.LOGGER.warn("[SE兼容] 无法读取死亡之眼倍率，新约按 1.0 倍处理：{}",
					throwable.toString());
		}
	}
}
