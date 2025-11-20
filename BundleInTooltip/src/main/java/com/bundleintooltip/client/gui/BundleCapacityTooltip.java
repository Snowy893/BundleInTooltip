package com.bundleintooltip.client.gui;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

public final class BundleCapacityTooltip implements TooltipComponent {
    private final int weight;
    private final int displayWidth;

    public BundleCapacityTooltip(int weight, int displayWidth) {
        this.weight = weight;
        this.displayWidth = displayWidth;
    }

    public int getWeight() {
        return this.weight;
    }

    public int getDisplayWidth() {
        return this.displayWidth;
    }
}
