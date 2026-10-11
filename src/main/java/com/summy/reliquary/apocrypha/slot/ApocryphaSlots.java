package com.summy.reliquary.apocrypha.slot;

import com.summy.reliquary.apocrypha.Apocrypha;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * 本模组的 Curios 栏位。
 *
 * <p>栏位本体是数据驱动的（见 data/summy-reliquary-apocrypha/curios/slots/*.json），
 * 这里只注册「哪个物品能进哪个栏位」的校验谓词，谓词 id 与 json 里的 validators 对应。
 */
public final class ApocryphaSlots {
	/** 新约-光环栏位（新约系列 #1） */
	public static final String NEW_TESTAMENT_HALO = "new_testament_halo";
	/** 新约-才能栏位（新约系列 #2） */
	public static final String NEW_TESTAMENT_TALENT = "new_testament_talent";
	/** 新约-恩典栏位（新约系列 #3） */
	public static final String NEW_TESTAMENT_GRACE = "new_testament_grace";

	/**
	 * 「新约系列」的栏位：**仅天使线开放**（有天使标记时 1 格、否则 0 格）。
	 *
	 * <p>后续系列饰品只需在这里追加一行，动态格数由 {@code ApocryphaSlotSizing} 统一维护。
	 */
	public static final List<String> ANGEL_SERIES_SLOTS =
			List.of(NEW_TESTAMENT_HALO, NEW_TESTAMENT_TALENT, NEW_TESTAMENT_GRACE);

	private ApocryphaSlots() {
	}

	/** 在模组构造阶段注册校验谓词 */
	public static void registerValidators() {
		// 新约-光环：只接受「新约」
		CuriosApi.registerCurioPredicate(Apocrypha.id("new_testament_halo_only"),
				result -> result.stack().is(Apocrypha.NEW_TESTAMENT.get()));
		// 新约-才能：只接受「迅羽·协约」
		CuriosApi.registerCurioPredicate(Apocrypha.id("new_testament_talent_only"),
				result -> result.stack().is(Apocrypha.SWIFT_FEATHER_COVENANT.get()));
		// 新约-恩典：只接受「翼羽·圣餐」
		CuriosApi.registerCurioPredicate(Apocrypha.id("new_testament_grace_only"),
				result -> result.stack().is(Apocrypha.WING_FEATHER_SACRAMENT.get()));
	}
}
