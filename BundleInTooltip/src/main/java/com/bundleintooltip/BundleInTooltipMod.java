package com.bundleintooltip;

import com.bundleintooltip.network.BundleInTooltipNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(BundleInTooltipMod.MOD_ID)
public final class BundleInTooltipMod {
    public static final String MOD_ID = "bundleintooltip";

    public BundleInTooltipMod() {
        BundleInTooltipNetwork.init();
        MinecraftForge.EVENT_BUS.register(this);
    }
}
