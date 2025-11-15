package com.bundleintooltip.network;

import com.bundleintooltip.BundleInTooltipMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BundleInTooltipNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(BundleInTooltipMod.MOD_ID, "channel"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    private BundleInTooltipNetwork() {
    }

    public static void init() {
        CHANNEL.messageBuilder(SelectBundleItemPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder(SelectBundleItemPacket::encode)
            .decoder(SelectBundleItemPacket::decode)
            .consumerMainThread(SelectBundleItemPacket::handle)
            .add();
    }

    public static void sendSelection(SelectBundleItemPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
