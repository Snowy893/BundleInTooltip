package com.bundleintooltip.client;

import com.bundleintooltip.common.BundleHelper;
import com.bundleintooltip.network.BundleInTooltipNetwork;
import com.bundleintooltip.network.SelectBundleItemPacket;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Tracks which bundle stack is currently under the cursor on the client so that
 * scrolling and clicking can be converted into focused selection changes and
 * network requests.
 */
public final class BundleSelectionTracker {
    public static final BundleSelectionTracker INSTANCE = new BundleSelectionTracker();

    private final IntSupplier selectionSupplier = () -> this.selectedIndex;
    private ItemStack trackedStack = ItemStack.EMPTY;
    private int trackedSlotIndex = -1;
    private int trackedContainerId = -1;
    private int selectedIndex = 0;
    private int cachedSize = 0;

    private BundleSelectionTracker() {
    }

    public IntSupplier bindTooltip(@Nullable AbstractContainerScreen<?> screen, @Nullable Slot slot, ItemStack stack, List<ItemStack> contents) {
        bindState(screen, slot, stack, contents.size());
        return this.selectionSupplier;
    }

    public boolean handleScroll(AbstractContainerScreen<?> screen, Slot slot, double scrollDelta) {
        int size = BundleHelper.copyContents(slot.getItem()).size();
        bindState(screen, slot, slot.getItem(), size);
        if (scrollDelta == 0 || this.cachedSize <= 0) {
            return false;
        }

        int direction = scrollDelta > 0 ? -1 : 1;
        this.selectedIndex = Math.floorMod(this.selectedIndex + direction, this.cachedSize);
        return true;
    }

    public boolean handleClick(AbstractContainerScreen<?> screen, Slot slot) {
        ItemStack slotStack = slot.getItem();
        int size = BundleHelper.copyContents(slotStack).size();
        bindState(screen, slot, slotStack, size);
        if (this.cachedSize <= 0) {
            return false;
        }

        if (Minecraft.getInstance().player != null && !Minecraft.getInstance().player.containerMenu.getCarried().isEmpty()) {
            return false;
        }

        Optional<ItemStack> simulatedExtraction = BundleHelper.removeAt(slotStack, this.selectedIndex);
        if (simulatedExtraction.isEmpty()) {
            return false;
        }

        slot.set(slotStack);
        slot.setChanged();
        screen.getMenu().setCarried(simulatedExtraction.get());

        BundleInTooltipNetwork.sendSelection(new SelectBundleItemPacket(screen.getMenu().containerId, slot.index, this.selectedIndex));
        return true;
    }

    private void bindState(@Nullable AbstractContainerScreen<?> screen, @Nullable Slot slot, ItemStack stack, int size) {
        if (stack.isEmpty()) {
            this.trackedStack = ItemStack.EMPTY;
            this.cachedSize = 0;
            this.selectedIndex = 0;
            this.trackedContainerId = -1;
            this.trackedSlotIndex = -1;
            return;
        }

        if (this.trackedStack.isEmpty() || !ItemStack.isSameItemSameTags(stack, this.trackedStack)) {
            this.trackedStack = stack.copyWithCount(1);
            this.selectedIndex = 0;
        }

        this.cachedSize = size;
        clampSelection();

        if (screen == null) {
            this.trackedContainerId = -1;
            this.trackedSlotIndex = -1;
        } else if (slot != null) {
            this.trackedContainerId = screen.getMenu().containerId;
            this.trackedSlotIndex = slot.index;
        }
    }

    private void clampSelection() {
        if (this.cachedSize <= 0) {
            this.selectedIndex = 0;
        } else {
            this.selectedIndex = Mth.clamp(this.selectedIndex, 0, this.cachedSize - 1);
        }
    }

    @Nullable
    public Slot resolveSlot(AbstractContainerScreen<?> screen, @Nullable Slot hoveredSlot) {
        if (hoveredSlot != null) {
            return hoveredSlot;
        }

        if (screen.getMenu().containerId == this.trackedContainerId) {
            if (this.trackedSlotIndex >= 0 && this.trackedSlotIndex < screen.getMenu().slots.size()) {
                Slot slot = screen.getMenu().slots.get(this.trackedSlotIndex);
                if (slot != null && !this.trackedStack.isEmpty() && ItemStack.isSameItemSameTags(slot.getItem(), this.trackedStack)) {
                    return slot;
                }
            }

            if (!this.trackedStack.isEmpty()) {
                for (Slot slot : screen.getMenu().slots) {
                    if (slot != null && ItemStack.isSameItemSameTags(slot.getItem(), this.trackedStack)) {
                        this.trackedSlotIndex = slot.index;
                        return slot;
                    }
                }
            }
        }

        return null;
    }
}
