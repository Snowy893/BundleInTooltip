package com.bundleintooltip.client;

import com.bundleintooltip.common.BundleHelper;
import com.bundleintooltip.network.BundleInTooltipNetwork;
import com.bundleintooltip.network.DropBundleItemPacket;
import com.bundleintooltip.network.SelectBundleItemPacket;
import com.bundleintooltip.network.StoreBundleItemPacket;
import com.bundleintooltip.network.SwapBundleItemPacket;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
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
    private int selectedIndex = -1;
    private int cachedSize = 0;
    private boolean suppressRelease = false;

    private BundleSelectionTracker() {
    }

    public IntSupplier bindTooltip(@Nullable AbstractContainerScreen<?> screen, @Nullable Slot slot, ItemStack stack, List<ItemStack> contents) {
        bindState(screen, slot, stack, contents.size(), false);
        return this.selectionSupplier;
    }

    public boolean handleScroll(AbstractContainerScreen<?> screen, Slot slot, double scrollDelta) {
        int size = BundleHelper.copyContents(slot.getItem()).size();
        bindState(screen, slot, slot.getItem(), size, true);
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
        bindState(screen, slot, slotStack, size, true);
        if (this.cachedSize <= 0) {
            return false;
        }

        if (Minecraft.getInstance().player != null && !Minecraft.getInstance().player.containerMenu.getCarried().isEmpty()) {
            return false;
        }

        if (this.selectedIndex < 0) {
            return false;
        }

        Optional<ItemStack> simulatedExtraction = BundleHelper.removeAt(slotStack, this.selectedIndex);
        if (simulatedExtraction.isEmpty()) {
            return false;
        }

        slot.set(slotStack);
        slot.setChanged();
        screen.getMenu().setCarried(simulatedExtraction.get());
        this.suppressRelease = true;

        BundleInTooltipNetwork.sendSelection(new SelectBundleItemPacket(screen.getMenu().containerId, slot.index, this.selectedIndex));
        return true;
    }

    public boolean handleDeposit(AbstractContainerScreen<?> screen, Slot slot) {
        if (!BundleHelper.isBundle(slot.getItem())) {
            return false;
        }
        ItemStack carried = screen.getMenu().getCarried();
        if (carried.isEmpty()) {
            return false;
        }

        ItemStack bundleStack = slot.getItem();
        int moved = BundleHelper.addToBundle(bundleStack, carried, carried.getCount());
        if (moved <= 0) {
            return false;
        }

        carried.shrink(moved);
        slot.set(bundleStack);
        slot.setChanged();
        screen.getMenu().setCarried(carried);
        NonNullList<ItemStack> contents = BundleHelper.copyContents(bundleStack);
        bindState(screen, slot, bundleStack, contents.size(), true);
        BundleInTooltipNetwork.sendStore(new StoreBundleItemPacket(screen.getMenu().containerId, slot.index, -1, false));
        return true;
    }

    public boolean consumeReleaseSuppression() {
        if (this.suppressRelease) {
            this.suppressRelease = false;
            return true;
        }
        return false;
    }

    public boolean handleCarriedBundlePickup(AbstractContainerScreen<?> screen, Slot clickedSlot) {
        ItemStack carried = screen.getMenu().getCarried();
        if (carried.isEmpty() || !BundleHelper.isBundle(carried)) {
            return false;
        }
        if (clickedSlot == null) {
            return false;
        }

        ItemStack slotStack = clickedSlot.getItem();
        if (slotStack.isEmpty()) {
            return false;
        }

        int moved = BundleHelper.addToBundle(carried, slotStack, slotStack.getCount());
        if (moved <= 0) {
            return false;
        }

        slotStack.shrink(moved);
        clickedSlot.set(slotStack);
        clickedSlot.setChanged();
        screen.getMenu().setCarried(carried);
        this.suppressRelease = true;
        BundleInTooltipNetwork.sendStore(new StoreBundleItemPacket(screen.getMenu().containerId, -1, clickedSlot.index, true));
        return true;
    }

    public boolean dropFromCarriedBundle(AbstractContainerScreen<?> screen, Slot targetSlot) {
        ItemStack carried = screen.getMenu().getCarried();
        if (carried.isEmpty() || !BundleHelper.isBundle(carried) || targetSlot == null || targetSlot.hasItem()) {
            return false;
        }

        NonNullList<ItemStack> contents = BundleHelper.copyContents(carried);
        if (contents.isEmpty()) {
            return false;
        }

        int removalIndex = contents.size() - 1;
        Optional<ItemStack> extracted = BundleHelper.removeAt(carried, removalIndex);
        if (extracted.isEmpty()) {
            return false;
        }

        targetSlot.set(extracted.get());
        targetSlot.setChanged();
        screen.getMenu().setCarried(carried);
        BundleInTooltipNetwork.sendDrop(new DropBundleItemPacket(screen.getMenu().containerId, targetSlot.index, removalIndex));
        return true;
    }

    public boolean swapWithBundle(AbstractContainerScreen<?> screen, Slot targetSlot) {
        if (targetSlot == null || !targetSlot.hasItem()) {
            return false;
        }

        ItemStack carried = screen.getMenu().getCarried();
        if (carried.isEmpty()) {
            return false;
        }

        ItemStack slotStack = targetSlot.getItem().copy();
        if (!BundleHelper.isBundle(carried) && !BundleHelper.isBundle(slotStack)) {
            return false;
        }

        targetSlot.set(carried.copy());
        targetSlot.setChanged();
        screen.getMenu().setCarried(slotStack);
        this.suppressRelease = true;
        BundleInTooltipNetwork.sendSwap(new SwapBundleItemPacket(screen.getMenu().containerId, targetSlot.index));
        return true;
    }


    private void bindState(@Nullable AbstractContainerScreen<?> screen, @Nullable Slot slot, ItemStack stack, int size, boolean markInteracted) {
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
            this.selectedIndex = markInteracted ? 0 : -1;
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
        } else if (this.selectedIndex >= 0) {
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
