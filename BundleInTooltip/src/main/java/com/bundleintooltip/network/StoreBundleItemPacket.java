package com.bundleintooltip.network;

import com.bundleintooltip.common.BundleHelper;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record StoreBundleItemPacket(int containerId, int bundleSlotIndex, int sourceSlotIndex, boolean bundleInCursor) {
    public static StoreBundleItemPacket decode(FriendlyByteBuf buffer) {
        return new StoreBundleItemPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.containerId);
        buffer.writeVarInt(this.bundleSlotIndex);
        buffer.writeVarInt(this.sourceSlotIndex);
        buffer.writeBoolean(this.bundleInCursor);
    }

    public static void handle(StoreBundleItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            AbstractContainerMenu menu = player.containerMenu;
            if (menu.containerId != packet.containerId) {
                return;
            }

            boolean updated = false;
            if (packet.bundleInCursor) {
                ItemStack carried = menu.getCarried();
                if (!BundleHelper.isBundle(carried)) {
                    return;
                }
                if (packet.sourceSlotIndex < 0 || packet.sourceSlotIndex >= menu.slots.size()) {
                    return;
                }
                Slot sourceSlot = menu.slots.get(packet.sourceSlotIndex);
                ItemStack sourceStack = sourceSlot.getItem();
                if (sourceStack.isEmpty()) {
                    return;
                }
                int moved = BundleHelper.addToBundle(carried, sourceStack, sourceStack.getCount());
                if (moved <= 0) {
                    return;
                }
                sourceStack.shrink(moved);
                sourceSlot.set(sourceStack);
                sourceSlot.setChanged();
                menu.setCarried(carried);
                updated = true;
            } else {
                if (packet.bundleSlotIndex < 0 || packet.bundleSlotIndex >= menu.slots.size()) {
                    return;
                }
                Slot bundleSlot = menu.slots.get(packet.bundleSlotIndex);
                ItemStack bundleStack = bundleSlot.getItem();
                if (!BundleHelper.isBundle(bundleStack)) {
                    return;
                }
                ItemStack carried = menu.getCarried();
                if (carried.isEmpty()) {
                    return;
                }
                int moved = BundleHelper.addToBundle(bundleStack, carried, carried.getCount());
                if (moved <= 0) {
                    return;
                }
                carried.shrink(moved);
                bundleSlot.set(bundleStack);
                bundleSlot.setChanged();
                menu.setCarried(carried);
                updated = true;
            }

            if (updated) {
                menu.broadcastChanges();
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.75F, 1.0F);
            }
        });
        context.setPacketHandled(true);
    }
}
