package com.bundleintooltip.client.gui;

import com.bundleintooltip.config.BundleInTooltipConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class BundleCapacityClientTooltip implements ClientTooltipComponent {
    private final int width;
    private final int containerWidth;
    private static final int HEIGHT = 12;

    private final float ratio;
    private final int weight;
    private final BundleInTooltipConfig.CapacityLabelMode mode;

    public BundleCapacityClientTooltip(BundleCapacityTooltip tooltip) {
        this.weight = tooltip.getWeight();
        this.ratio = Mth.clamp(this.weight / 64.0F, 0.0F, 1.0F);
        this.mode = BundleInTooltipConfig.capacityLabelMode();
        this.containerWidth = tooltip.getDisplayWidth();
        this.width = calculateWidth();
    }

    @Override
    public int getHeight() {
        return HEIGHT + 6;
    }

    @Override
    public int getWidth(Font font) {
        return this.width;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int offset = Math.max(0, (this.containerWidth - this.width) / 2);
        int barX = x + offset;
        int barY = y + 3;
        int fill = (int)(this.width * this.ratio);

        int borderColor = 0xFF4D4D4D;
        int backgroundColor = 0xFF171317;
        int fillColor = this.ratio >= 1.0F ? 0xFFD14E48 : 0xFF5A64F4;

        fillBarInterior(graphics, barX, barY, this.width, HEIGHT, backgroundColor);
        if (fill > 0) {
            drawFillSegment(graphics, barX, barY, this.width, HEIGHT, fill, fillColor);
        }
        drawRoundedOutline(graphics, barX, barY, this.width, HEIGHT, borderColor);

        Component label = createLabel();
        if (label != null) {
            int textWidth = font.width(label);
            graphics.drawString(font, label, barX + (this.width - textWidth) / 2, barY + 2, 0xFFFFFFFF, true);
        }
    }

    private int calculateWidth() {
        boolean modern = BundleInTooltipConfig.slotTextureMode().isModern();
        int min = modern ? 90 : 124;
        int max = modern ? 170 : 190;
        int margin = modern ? 0 : 8;
        int target = this.containerWidth - margin;
        return Mth.clamp(target, min, max);
    }

    private Component createLabel() {
        return switch (this.mode) {
            case NONE -> null;
            case VANILLA -> {
                if (this.weight <= 0) {
                    yield Component.translatable("bundleintooltip.tooltip.empty");
                } else if (this.weight >= 64) {
                    yield Component.translatable("bundleintooltip.tooltip.full");
                }
                yield null;
            }
            case CLASSIC -> Component.literal(this.weight + "/64");
            case HYBRID -> {
                if (this.weight <= 0) {
                    yield Component.translatable("bundleintooltip.tooltip.empty");
                } else if (this.weight >= 64) {
                    yield Component.translatable("bundleintooltip.tooltip.full");
                } else {
                    yield Component.literal(this.weight + "/64");
                }
            }
            case VANILLA_REVISED -> {
                if (this.weight <= 0) {
                    yield Component.translatable("bundleintooltip.tooltip.empty");
                } else if (this.weight >= 64) {
                    yield Component.translatable("bundleintooltip.tooltip.full");
                } else {
                    yield Component.translatable("bundleintooltip.tooltip.partial");
                }
            }
        };
    }

    private static void fillBarInterior(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 2 || height <= 2) {
            return;
        }
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    private static void drawRoundedOutline(GuiGraphics graphics, int x, int y, int width, int height, int outlineColor) {
        if (width <= 0 || height <= 0) {
            return;
        }

        graphics.fill(x + 1, y, x + width - 1, y + 1, outlineColor);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, outlineColor);
        graphics.fill(x, y + 1, x + 1, y + height - 1, outlineColor);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, outlineColor);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, outlineColor);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + 2, outlineColor);
        graphics.fill(x + 1, y + height - 2, x + 2, y + height - 1, outlineColor);
        graphics.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, outlineColor);
    }

    private static void drawFillSegment(GuiGraphics graphics, int x, int y, int totalWidth, int height, int fillWidth, int color) {
        if (fillWidth <= 0 || totalWidth <= 2 || height <= 2) {
            return;
        }

        int innerLeft = x + 1;
        int innerTop = y + 1;
        int innerRight = x + totalWidth - 1;
        int innerBottom = y + height - 1;
        int innerWidth = Math.max(0, innerRight - innerLeft);
        if (innerWidth <= 0) {
            return;
        }

        float portion = Math.min(1.0F, fillWidth / (float)totalWidth);
        int drawWidth = Mth.clamp(Mth.ceil(portion * innerWidth), 0, innerWidth);
        if (drawWidth <= 0) {
            return;
        }

        graphics.fill(innerLeft, innerTop, innerLeft + drawWidth, innerBottom, color);
        if (drawWidth < innerWidth) {
            graphics.fill(innerLeft + drawWidth, innerTop + 1, innerLeft + drawWidth + 1, innerBottom - 1, color);
        }
    }

}
