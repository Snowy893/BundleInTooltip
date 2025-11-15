package com.bundleintooltip.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public final class SelectableClientBundleTooltip implements ClientTooltipComponent {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/bundle.png");
    private final NonNullList<ItemStack> items;
    private final int weight;
    private final SelectableBundleTooltip data;

    public SelectableClientBundleTooltip(SelectableBundleTooltip tooltip) {
        this.items = tooltip.getItems();
        this.weight = tooltip.getWeight();
        this.data = tooltip;
    }

    @Override
    public int getHeight() {
        return gridSizeY() * 20 + 6;
    }

    @Override
    public int getWidth(Font font) {
        return gridSizeX() * 18 + 2;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int columns = gridSizeX();
        int rows = gridSizeY();
        boolean full = this.weight >= 64;
        int slotIndex = 0;
        int selected = resolveSelection();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int slotX = x + column * 18 + 1;
                int slotY = y + row * 20 + 1;
                renderSlot(slotX, slotY, slotIndex++, full, selected, graphics, font);
            }
        }

        drawBorder(x, y, columns, rows, graphics);
    }

    private void renderSlot(int x, int y, int index, boolean blocked, int selectedIndex, GuiGraphics graphics, Font font) {
        if (index >= this.items.size()) {
            blit(graphics, x, y, blocked ? Texture.BLOCKED_SLOT : Texture.SLOT);
            return;
        }

        ItemStack stack = this.items.get(index);
        blit(graphics, x, y, Texture.SLOT);
        graphics.renderItem(stack, x + 1, y + 1, index);
        graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        if (index == selectedIndex) {
            AbstractContainerScreen.renderSlotHighlight(graphics, x + 1, y + 1, 0);
        }
    }

    private void drawBorder(int x, int y, int columns, int rows, GuiGraphics graphics) {
        blit(graphics, x, y, Texture.BORDER_CORNER_TOP);
        blit(graphics, x + columns * 18 + 1, y, Texture.BORDER_CORNER_TOP);
        for (int i = 0; i < columns; i++) {
            blit(graphics, x + 1 + i * 18, y, Texture.BORDER_HORIZONTAL_TOP);
            blit(graphics, x + 1 + i * 18, y + rows * 20, Texture.BORDER_HORIZONTAL_BOTTOM);
        }
        for (int row = 0; row < rows; row++) {
            blit(graphics, x, y + row * 20 + 1, Texture.BORDER_VERTICAL);
            blit(graphics, x + columns * 18 + 1, y + row * 20 + 1, Texture.BORDER_VERTICAL);
        }
        blit(graphics, x, y + rows * 20, Texture.BORDER_CORNER_BOTTOM);
        blit(graphics, x + columns * 18 + 1, y + rows * 20, Texture.BORDER_CORNER_BOTTOM);
    }

    private void blit(GuiGraphics graphics, int x, int y, Texture texture) {
        graphics.blit(TEXTURE, x, y, 0, texture.u, texture.v, texture.w, texture.h, 128, 128);
    }

    private int gridSizeX() {
        return Math.max(2, (int)Math.ceil(Math.sqrt(this.items.size() + 1.0D)));
    }

    private int gridSizeY() {
        return (int)Math.ceil((this.items.size() + 1.0D) / (double)gridSizeX());
    }

    private int resolveSelection() {
        if (this.items.isEmpty()) {
            return -1;
        }
        int index = this.data.getSelectionIndex();
        if (index < 0) {
            return 0;
        }
        if (index >= this.items.size()) {
            return this.items.size() - 1;
        }
        return index;
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
