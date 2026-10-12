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
	/** 新约-祝福栏位（新约系列 #4） */
	public static final String NEW_TESTAMENT_BLESSING = "new_testament_blessing";
	/** 新约-启示栏位（新约系列 #5） */
	public static final String NEW_TESTAMENT_REVELATION = "new_testament_revelation";
	/** 殁亡之提喻栏位（凋亡系列 #1，恶魔线） */
	public static final String PERDITION_SYNECDOCHE = "perdition_synecdoche";
	/** 殁亡之释经栏位（凋亡系列 #2，恶魔线） */
	public static final String PERDITION_EXEGESIS = "perdition_exegesis";
	/** 殁亡之弥撒栏位（凋亡系列 #3，恶魔线） */
	public static final String PERDITION_MASS = "perdition_mass";

	/**
	 * 「新约系列」的栏位：**仅天使线开放**（有天使标记时 1 格、否则 0 格）。
	 *
	 * <p>后续系列饰品只需在这里追加一行，动态格数由 {@code ApocryphaSlotSizing} 统一维护。
	 */
	public static final List<String> ANGEL_SERIES_SLOTS =
			List.of(NEW_TESTAMENT_HALO, NEW_TESTAMENT_TALENT, NEW_TESTAMENT_GRACE, NEW_TESTAMENT_BLESSING,
					NEW_TESTAMENT_REVELATION);

	/**
	 * 「凋亡系列」的栏位：**仅恶魔线开放**（有恶魔标记时 1 格、否则 0 格）。
	 *
	 * <p>与 {@link #ANGEL_SERIES_SLOTS} 同款处理，后续 4 件各追加一行。
	 */
	public static final List<String> DEMON_SERIES_SLOTS =
			List.of(PERDITION_SYNECDOCHE, PERDITION_EXEGESIS, PERDITION_MASS);

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
		// 新约-祝福：只接受「逆羽·滞空」
		CuriosApi.registerCurioPredicate(Apocrypha.id("new_testament_blessing_only"),
				result -> result.stack().is(Apocrypha.INVERSE_FEATHER_HOVER.get()));
		// 新约-启示：只接受「比翼·连携」
		CuriosApi.registerCurioPredicate(Apocrypha.id("new_testament_revelation_only"),
				result -> result.stack().is(Apocrypha.PAIRED_WINGS.get()));
		// 殁亡之提喻：只接受「堕天」
		CuriosApi.registerCurioPredicate(Apocrypha.id("perdition_synecdoche_only"),
				result -> result.stack().is(Apocrypha.FALLEN_HEAVEN.get()));
		// 殁亡之释经：只接受「坠羽」
		CuriosApi.registerCurioPredicate(Apocrypha.id("perdition_exegesis_only"),
				result -> result.stack().is(Apocrypha.FALLEN_PLUME.get()));
		// 殁亡之弥撒：只接受「笞心」
		CuriosApi.registerCurioPredicate(Apocrypha.id("perdition_mass_only"),
				result -> result.stack().is(Apocrypha.SCOURGED_HEART.get()));
	}
}
