package com.summy.reliquary.apocrypha.slot;

import com.summy.reliquary.apocrypha.Apocrypha;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 本模组的 Curios 栏位。
 *
 * <p>栏位本体是数据驱动的（见 data/summy-reliquary-apocrypha/curios/slots/*.json），
 * 这里只注册「哪个物品能进哪个栏位」的校验谓词，谓词 id 与 json 里的 validators 对应。
 */
public final class ApocryphaSlots {
	/** 新约-光环栏位 */
	public static final String NEW_TESTAMENT_HALO = "new_testament_halo";

	private ApocryphaSlots() {
	}

	/** 在模组构造阶段注册校验谓词 */
	public static void registerValidators() {
		// 新约-光环：目前只接受「新约」
		CuriosApi.registerCurioPredicate(Apocrypha.id("new_testament_halo_only"),
				result -> result.stack().is(Apocrypha.NEW_TESTAMENT.get()));
	}
}
