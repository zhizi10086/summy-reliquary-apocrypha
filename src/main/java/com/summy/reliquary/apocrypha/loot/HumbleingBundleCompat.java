package com.summy.reliquary.apocrypha.loot;

import com.summy.reliquary.apocrypha.Apocrypha;
import com.summy.reliquary.loot.HumbleingBundleBonusChestEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/** 慈善捆绑包 BONUS 开箱与 SRA 十件保底的桥接。 */
@EventBusSubscriber(modid = Apocrypha.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class HumbleingBundleCompat {
	private HumbleingBundleCompat() {
	}

	@SubscribeEvent
	public static void onBonusChest(HumbleingBundleBonusChestEvent event) {
		ApocryphaAcquisition.tryGrantForBonusPass(event.getContext());
	}
}
