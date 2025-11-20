package com.bundleintooltip.network;

import com.bundleintooltip.common.BundleHelper;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record DropBundleItemPacket(int containerId, int targetSlotIndex, int removalIndex) {
    public static DropBundleItemPacket decode(FriendlyByteBuf buf) {
        return new DropBundleItemPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.containerId);
        buf.writeVarInt(this.targetSlotIndex);
        buf.writeVarInt(this.removalIndex);
    }

    public static void handle(DropBundleItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            AbstractContainerMenu menu = player.containerMenu;
            if (menu.containerId != packet.containerId || packet.targetSlotIndex < 0 || packet.targetSlotIndex >= menu.slots.size()) {
                return;
            }

            Slot target = menu.slots.get(packet.targetSlotIndex);
            if (target == null || !target.getItem().isEmpty()) {
                return;
            }

            ItemStack carried = menu.getCarried();
            if (!BundleHelper.isBundle(carried)) {
                return;
            }

            var contents = BundleHelper.copyContents(carried);
            if (contents.isEmpty()) {
                return;
            }

            int safeIndex = packet.removalIndex < 0 ? contents.size() - 1 : Mth.clamp(packet.removalIndex, 0, contents.size() - 1);
            Optional<ItemStack> extracted = BundleHelper.removeAt(carried, safeIndex);
            if (extracted.isEmpty()) {
                return;
            }

            target.set(extracted.get());
            target.setChanged();
            menu.setCarried(carried);
            menu.broadcastChanges();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.75F, 1.0F);
        });
        context.setPacketHandled(true);
    }
}
