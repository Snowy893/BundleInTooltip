package com.bundleintooltip.client.gui;

import com.bundleintooltip.BundleInTooltipMod;
import com.bundleintooltip.config.BundleInTooltipConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class SelectableClientBundleTooltip implements ClientTooltipComponent {
    private static final ResourceLocation CLASSIC_TEXTURE = new ResourceLocation("textures/gui/container/bundle.png");
    private static final ResourceLocation VANILLA_BLOCKED_SLOT = new ResourceLocation(BundleInTooltipMod.MOD_ID, "textures/gui/sprites/container/bundle/blocked_slot.png");

    private final NonNullList<ItemStack> items;
    private final int weight;
    private final SelectableBundleTooltip data;
    private final BundleInTooltipConfig.SlotTextureMode slotMode;
    private final int displayWidth;

    public SelectableClientBundleTooltip(SelectableBundleTooltip tooltip) {
        this.items = tooltip.getItems();
        this.weight = tooltip.getWeight();
        this.data = tooltip;
        this.slotMode = BundleInTooltipConfig.slotTextureMode();
        this.displayWidth = tooltip.getDisplayWidth();
    }

    @Override
    public int getHeight() {
        int margin = this.slotMode.isModern() ? 2 : 6;
        if (this.items.isEmpty()) {
            return margin + 28;
        }
        return gridSizeY() * verticalSpacing() + margin;
    }

    @Override
    public int getWidth(Font font) {
        return this.displayWidth;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        if (this.items.isEmpty()) {
            renderEmptyState(font, x, y, graphics);
            return;
        }

        int columns = gridSizeX();
        int rows = gridSizeY();
        int contentWidth = gridPixelWidth(columns);
        int offset = Math.max(0, (this.displayWidth - contentWidth) / 2);
        if (this.slotMode == BundleInTooltipConfig.SlotTextureMode.CLASSIC) {
            renderClassic(font, x + offset, y, graphics, columns, rows);
        } else {
            boolean drawBackground = this.slotMode == BundleInTooltipConfig.SlotTextureMode.VANILLA;
            renderModern(font, x + offset, y, graphics, columns, rows, drawBackground);
            if (shouldRenderSelectionBar()) {
                renderSelectionBar(font, x + offset, y - 33, graphics, contentWidth);
            }
        }
    }

    private void renderClassic(Font font, int x, int y, GuiGraphics graphics, int columns, int rows) {
        boolean full = this.weight >= 64;
        int slotIndex = 0;
        int selected = resolveSelection();
        int spacingX = horizontalSpacing();
        int spacingY = verticalSpacing();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int slotX = x + column * spacingX + 1;
                int slotY = y + row * spacingY + 1;
                renderClassicSlot(slotX, slotY, slotIndex++, full, selected, graphics, font);
            }
        }

        drawClassicBorder(x, y, columns, rows, graphics);
    }

    private void renderModern(Font font, int x, int y, GuiGraphics graphics, int columns, int rows, boolean drawBackground) {
        int slotIndex = 0;
        int selected = resolveSelection();
        int spacingX = horizontalSpacing();
        int spacingY = verticalSpacing();
        int offsetX = drawBackground || this.slotMode == BundleInTooltipConfig.SlotTextureMode.EXPERIMENT ? 2 : 1;
        int offsetY = drawBackground || this.slotMode == BundleInTooltipConfig.SlotTextureMode.EXPERIMENT ? 2 : 1;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int slotX = x + column * spacingX + offsetX;
                int slotY = y + row * spacingY + offsetY;
                renderModernSlot(slotX, slotY, slotIndex++, selected, graphics, font, drawBackground);
            }
        }
    }

    private void renderEmptyState(Font font, int x, int y, GuiGraphics graphics) {
        int drawX = x + 2;
        graphics.drawString(font, Component.translatable("bundleintooltip.tooltip.empty_hint.line1"), drawX, y + 4, 0xFFBFBFC4, false);
        graphics.drawString(font, Component.translatable("bundleintooltip.tooltip.empty_hint.line2"), drawX, y + 4 + font.lineHeight, 0xFFBFBFC4, false);
    }

    private void renderClassicSlot(int x, int y, int index, boolean blocked, int selectedIndex, GuiGraphics graphics, Font font) {
        if (index >= this.items.size()) {
            blitClassic(graphics, x, y, Texture.SLOT);
            return;
        }

        ItemStack stack = this.items.get(index);
        blitClassic(graphics, x, y, Texture.SLOT);
        graphics.renderItem(stack, x + 1, y + 1, index);
        graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        if (index == selectedIndex) {
            AbstractContainerScreen.renderSlotHighlight(graphics, x + 1, y + 1, 0);
        }
    }

    private void renderModernSlot(int x, int y, int index, int selectedIndex, GuiGraphics graphics, Font font, boolean drawBackground) {
        if (index >= this.items.size()) {
            return;
        }

        ItemStack stack = this.items.get(index);
        if (drawBackground) {
            drawVanillaSlotBackground(graphics, x, y);
        }
        graphics.renderItem(stack, x, y, index);
        graphics.renderItemDecorations(font, stack, x, y);
        if (index == selectedIndex) {
            drawVanillaHighlight(graphics, x, y);
        }
    }

    private void drawClassicBorder(int x, int y, int columns, int rows, GuiGraphics graphics) {
        blitClassic(graphics, x, y, Texture.BORDER_CORNER_TOP);
        blitClassic(graphics, x + columns * 18 + 1, y, Texture.BORDER_CORNER_TOP);
        for (int i = 0; i < columns; i++) {
            blitClassic(graphics, x + 1 + i * 18, y, Texture.BORDER_HORIZONTAL_TOP);
            blitClassic(graphics, x + 1 + i * 18, y + rows * 20, Texture.BORDER_HORIZONTAL_BOTTOM);
        }
        for (int row = 0; row < rows; row++) {
            blitClassic(graphics, x, y + row * 20 + 1, Texture.BORDER_VERTICAL);
            blitClassic(graphics, x + columns * 18 + 1, y + row * 20 + 1, Texture.BORDER_VERTICAL);
        }
        blitClassic(graphics, x, y + rows * 20, Texture.BORDER_CORNER_BOTTOM);
        blitClassic(graphics, x + columns * 18 + 1, y + rows * 20, Texture.BORDER_CORNER_BOTTOM);
    }

    private void blitClassic(GuiGraphics graphics, int x, int y, Texture texture) {
        graphics.blit(CLASSIC_TEXTURE, x, y, 0, texture.u, texture.v, texture.w, texture.h, 128, 128);
    }

    private int gridSizeX() {
        int count = Math.max(1, this.items.size());
        double adjustment = this.slotMode.isModern() ? 0.1D : 1.0D;
        int base = Math.max(2, (int)Math.ceil(Math.sqrt(count + adjustment)));
        base = Math.min(8, base);
        if (this.slotMode.isModern()) {
            base = Math.max(4, base);
        }
        return base;
    }

    private int gridSizeY() {
        int columns = gridSizeX();
        int count = Math.max(1, this.items.size());
        return Math.max(1, (int)Math.ceil((double)count / (double)columns));
    }

    private int resolveSelection() {
        if (this.items.isEmpty()) {
            return -1;
        }
        int index = this.data.getSelectionIndex();
        if (index < 0) {
            return -1;
        }
        if (index >= this.items.size()) {
            return this.items.size() - 1;
        }
        return index;
    }

    private void drawVanillaSlotBackground(GuiGraphics graphics, int x, int y) {
        int left = x - 2;
        int top = y - 2;
        int right = x + 18;
        int bottom = y + 18;
        int fillColor = 0xFF221C27;

        // center rectangle excluding corners
        graphics.fill(left + 1, top + 2, right - 1, bottom - 2, fillColor);
        graphics.fill(left + 2, top + 1, right - 2, top + 2, fillColor);
        graphics.fill(left + 2, bottom - 2, right - 2, bottom - 1, fillColor);
    }

    private void drawVanillaHighlight(GuiGraphics graphics, int x, int y) {
        int left = x - 2;
        int top = y - 2;
        int right = x + 18;
        int bottom = y + 18;
        int color = 0x60FFFFFF;
        graphics.fill(left + 1, top + 2, right - 1, bottom - 2, color);
        graphics.fill(left + 2, top + 1, right - 2, top + 2, color);
        graphics.fill(left + 2, bottom - 2, right - 2, bottom - 1, color);
    }

    private void renderSelectionBar(Font font, int x, int y, GuiGraphics graphics, int contentWidth) {
        if (!shouldRenderSelectionBar()) {
            return;
        }
        int selection = this.data.getSelectionIndex();
        Component text = selection >= 0 && selection < this.items.size()
            ? this.items.get(selection).getHoverName()
            : Component.translatable("bundleintooltip.tooltip.selection.none");

        int textWidth = font.width(text);
        int padding = 12;
        int barWidth = Math.max(40, textWidth + padding);
        int barX = x + contentWidth / 2 - barWidth / 2;
        int barY = y;
        int barHeight = 16;
        int outerBorder = 0xFF1C0A2E;
        int border = 0xFF2A1157;
        int background = 0xF011071B;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        drawRoundedFrame(graphics, barX - 1, barY - 1, barWidth + 2, barHeight + 2, outerBorder);
        drawRoundedFrame(graphics, barX, barY, barWidth, barHeight, border);
        fillCorners(graphics, barX, barY, barWidth, barHeight, border);
        int innerWidth = Math.max(0, barWidth - 2);
        int innerHeight = Math.max(0, barHeight - 2);
        drawRoundedPanel(graphics, barX + 1, barY + 1, innerWidth, innerHeight, background);
        int textColor = 0xFFFFFFFF;
        int textX = barX + Math.max(4, (barWidth - textWidth) / 2);
        graphics.drawString(font, text, textX, barY + 4, textColor, false);
        graphics.pose().popPose();
    }

    private static void drawRoundedPanel(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + height, color);
    }

    private static void drawRoundedFrame(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x + 1, y, x + width - 1, y + 1, color);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private static void fillCorners(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x, y, x + 1, y + 1, color);
        graphics.fill(x + width - 1, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + 1, y + height, color);
        graphics.fill(x + width - 1, y + height - 1, x + width, y + height, color);
    }

    private boolean shouldRenderSelectionBar() {
        return BundleInTooltipConfig.showSelectionBar()
            && this.slotMode != BundleInTooltipConfig.SlotTextureMode.CLASSIC
            && !this.items.isEmpty()
            && this.data.getSelectionIndex() >= 0;
    }

    private int horizontalSpacing() {
        return switch (this.slotMode) {
            case VANILLA, EXPERIMENT -> 24;
            case CLASSIC -> 18;
        };
    }

    private int verticalSpacing() {
        return switch (this.slotMode) {
            case VANILLA, EXPERIMENT -> 26;
            case CLASSIC -> 20;
        };
    }

    private int gridPixelWidth(int columns) {
        return switch (this.slotMode) {
            case VANILLA -> (columns - 1) * horizontalSpacing() + 20;
            case EXPERIMENT -> (columns - 1) * horizontalSpacing() + 20;
            case CLASSIC -> columns * 18 + 2;
        };
    }

    private enum Texture {
        SLOT(0, 0, 18, 20),
        BLOCKED_SLOT(0, 40, 18, 20),
        BORDER_VERTICAL(0, 18, 1, 20),
        BORDER_HORIZONTAL_TOP(0, 20, 18, 1),
        BORDER_HORIZONTAL_BOTTOM(0, 60, 18, 1),
        BORDER_CORNER_TOP(0, 20, 1, 1),
        BORDER_CORNER_BOTTOM(0, 60, 1, 1);

        final int u;
        final int v;
        final int w;
        final int h;

        Texture(int u, int v, int w, int h) {
            this.u = u;
            this.v = v;
            this.w = w;
            this.h = h;
        }
    }
}
