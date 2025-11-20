package com.bundleintooltip;

import com.bundleintooltip.client.BundleInTooltipClient;
import com.bundleintooltip.client.gui.BundleInTooltipConfigScreen;
import com.bundleintooltip.config.BundleInTooltipConfig;
import com.bundleintooltip.network.BundleInTooltipNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BundleInTooltipMod.MOD_ID)
public final class BundleInTooltipMod {
    public static final String MOD_ID = "bundleintooltip";

    public BundleInTooltipMod() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.CLIENT, BundleInTooltipConfig.CLIENT_SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new BundleInTooltipConfigScreen(parent)))
        );
        BundleInTooltipNetwork.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BundleInTooltipClient::init);
        MinecraftForge.EVENT_BUS.register(this);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onConfigEvent);
    }

    private void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == BundleInTooltipConfig.CLIENT_SPEC) {
            BundleInTooltipConfig.setClientConfig(event.getConfig());
        }
    }
}
