package com.bundleintooltip.client.gui;

import java.util.function.IntSupplier;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public final class SelectableBundleTooltip implements TooltipComponent {
    private final NonNullList<ItemStack> items;
    private final int weight;
    private final IntSupplier selectionSupplier;
    private final int displayWidth;

    public SelectableBundleTooltip(NonNullList<ItemStack> items, int weight, IntSupplier selectionSupplier, int displayWidth) {
        this.items = NonNullList.create();
        this.items.addAll(items);
        this.weight = weight;
        this.selectionSupplier = selectionSupplier;
        this.displayWidth = displayWidth;
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public int getWeight() {
        return this.weight;
    }

    public int getSelectionIndex() {
        return this.selectionSupplier.getAsInt();
    }

    public int getDisplayWidth() {
        return this.displayWidth;
    }
}
