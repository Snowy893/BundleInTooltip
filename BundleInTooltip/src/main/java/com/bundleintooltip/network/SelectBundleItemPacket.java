package com.bundleintooltip.network;

import com.bundleintooltip.common.BundleHelper;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Sent when the client scroll-selects a bundle entry and requests the server to hand it to the cursor.
 * The server double-checks the container id, slot contents, and cursor status before mutating inventory state.
 */
public record SelectBundleItemPacket(int containerId, int slotIndex, int selectionIndex) {
    public static SelectBundleItemPacket decode(FriendlyByteBuf buffer) {
        return new SelectBundleItemPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.containerId);
        buffer.writeVarInt(this.slotIndex);
        buffer.writeVarInt(this.selectionIndex);
    }

    public static void handle(SelectBundleItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

            if (!menu.getCarried().isEmpty()) {
                return;
            }

            Slot slot = menu.slots.get(packet.slotIndex);
            ItemStack bundleStack = slot.getItem();
            if (!BundleHelper.isBundle(bundleStack)) {
                return;
            }

            NonNullList<ItemStack> contents = BundleHelper.copyContents(bundleStack);
            if (contents.isEmpty()) {
                return;
            }

            int safeIndex = Mth.clamp(packet.selectionIndex, 0, contents.size() - 1);
            Optional<ItemStack> extracted = BundleHelper.removeAt(bundleStack, safeIndex);
            if (extracted.isEmpty()) {
                return;
            }

            slot.set(bundleStack);
            slot.setChanged();
            menu.setCarried(extracted.get());
            menu.broadcastChanges();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.75F, 1.0F);
        });
        context.setPacketHandled(true);
    }
}
