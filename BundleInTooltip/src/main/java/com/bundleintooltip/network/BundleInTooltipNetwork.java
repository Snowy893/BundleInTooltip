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
        CHANNEL.messageBuilder(StoreBundleItemPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
            .encoder(StoreBundleItemPacket::encode)
            .decoder(StoreBundleItemPacket::decode)
            .consumerMainThread(StoreBundleItemPacket::handle)
            .add();
        CHANNEL.messageBuilder(DropBundleItemPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
            .encoder(DropBundleItemPacket::encode)
            .decoder(DropBundleItemPacket::decode)
            .consumerMainThread(DropBundleItemPacket::handle)
            .add();
        CHANNEL.messageBuilder(SwapBundleItemPacket.class, 3, NetworkDirection.PLAY_TO_SERVER)
            .encoder(SwapBundleItemPacket::encode)
            .decoder(SwapBundleItemPacket::decode)
            .consumerMainThread(SwapBundleItemPacket::handle)
            .add();
    }

    public static void sendSelection(SelectBundleItemPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendStore(StoreBundleItemPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendDrop(DropBundleItemPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendSwap(SwapBundleItemPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
