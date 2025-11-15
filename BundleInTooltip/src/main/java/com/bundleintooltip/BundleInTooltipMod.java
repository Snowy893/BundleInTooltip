package com.bundleintooltip;

import com.bundleintooltip.client.BundleInTooltipClient;
import com.bundleintooltip.network.BundleInTooltipNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(BundleInTooltipMod.MOD_ID)
public final class BundleInTooltipMod {
    public static final String MOD_ID = "bundleintooltip";

    public BundleInTooltipMod() {
        BundleInTooltipNetwork.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BundleInTooltipClient::init);
        MinecraftForge.EVENT_BUS.register(this);
    }
}