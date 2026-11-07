package com.summy.reliquary.apocrypha.loot;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.loot.HumbleingBundleBonusChestEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 慈善捆绑包 BONUS 开箱与 SRA 十件保底的桥接。 */
@Mod.EventBusSubscriber(modid = Apocrypha.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HumbleingBundleCompat {
	private HumbleingBundleCompat() {
	}

	@SubscribeEvent
	public static void onBonusChest(HumbleingBundleBonusChestEvent event) {
		ApocryphaAcquisition.tryGrantForBonusPass(event.getContext());
	}
}
