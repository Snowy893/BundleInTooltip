package com.bundleintooltip.network;

import com.bundleintooltip.common.BundleHelper;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record SwapBundleItemPacket(int containerId, int slotIndex) {
    public static SwapBundleItemPacket decode(FriendlyByteBuf buf) {
        return new SwapBundleItemPacket(buf.readVarInt(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.containerId);
        buf.writeVarInt(this.slotIndex);
    }

    public static void handle(SwapBundleItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            AbstractContainerMenu menu = player.containerMenu;
            if (menu.containerId != packet.containerId || packet.slotIndex < 0 || packet.slotIndex >= menu.slots.size()) {
                return;
            }

            Slot slot = menu.slots.get(packet.slotIndex);
            if (slot == null || !slot.hasItem()) {
                return;
            }

            ItemStack carried = menu.getCarried();
            ItemStack slotStack = slot.getItem();
            if (carried.isEmpty() || (!BundleHelper.isBundle(carried) && !BundleHelper.isBundle(slotStack))) {
                return;
            }

            menu.setCarried(slotStack.copy());
            slot.set(carried.copy());
            slot.setChanged();
            menu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }
}
