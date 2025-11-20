package com.bundleintooltip.client.gui;

import com.bundleintooltip.config.BundleInTooltipConfig;
import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BundleInTooltipConfigScreen extends Screen {
    private final Screen parent;
    private boolean showCapacityBar;
    private BundleInTooltipConfig.CapacityLabelMode labelMode;
    private BundleInTooltipConfig.SlotTextureMode slotTextureMode;
    private boolean showSelectionBar;

    public BundleInTooltipConfigScreen(Screen parent) {
        super(Component.translatable("bundleintooltip.config.title"));
        this.parent = parent;
        this.showCapacityBar = BundleInTooltipConfig.showCapacityBar();
        this.labelMode = BundleInTooltipConfig.capacityLabelMode();
        this.slotTextureMode = BundleInTooltipConfig.slotTextureMode();
        this.showSelectionBar = BundleInTooltipConfig.showSelectionBar();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        this.addRenderableWidget(
            CycleButton.builder(BundleInTooltipConfigScreen::labelForDisplayMode)
                .withValues(Arrays.asList(Boolean.TRUE, Boolean.FALSE))
                .withInitialValue(this.showCapacityBar)
                .create(centerX - 100, y, 200, 20, Component.translatable("bundleintooltip.config.show_bar"),
                    (button, value) -> this.showCapacityBar = value)
        );

        y += 28;

        this.addRenderableWidget(
            CycleButton.builder(BundleInTooltipConfigScreen::labelForMode)
                .withValues(Arrays.asList(BundleInTooltipConfig.CapacityLabelMode.values()))
                .withInitialValue(this.labelMode)
                .create(centerX - 100, y, 200, 20, Component.translatable("bundleintooltip.config.label_mode"),
                    (button, value) -> this.labelMode = value)
        );

        y += 40;

        this.addRenderableWidget(
            CycleButton.onOffBuilder()
                .withInitialValue(this.showSelectionBar)
                .create(centerX - 100, y, 200, 20, Component.translatable("bundleintooltip.config.show_selection_bar"),
                    (button, value) -> this.showSelectionBar = value)
        );

        y += 28;

        this.addRenderableWidget(
            CycleButton.builder(BundleInTooltipConfigScreen::labelForSlotStyle)
                .withValues(Arrays.asList(BundleInTooltipConfig.SlotTextureMode.values()))
                .withInitialValue(this.slotTextureMode)
                .create(centerX - 100, y, 200, 20, Component.translatable("bundleintooltip.config.slot_style"),
                    (button, value) -> this.slotTextureMode = value)
        );

        y += 40;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
            .bounds(centerX - 100, y, 200, 20)
            .build());
    }

    @Override
    public void onClose() {
        BundleInTooltipConfig.CLIENT.showCapacityBar.set(this.showCapacityBar);
        BundleInTooltipConfig.CLIENT.capacityLabelMode.set(this.labelMode);
        BundleInTooltipConfig.CLIENT.slotTextureMode.set(this.slotTextureMode);
        BundleInTooltipConfig.CLIENT.showSelectionBar.set(this.showSelectionBar);
        BundleInTooltipConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    private static Component labelForMode(BundleInTooltipConfig.CapacityLabelMode mode) {
        return switch (mode) {
            case NONE -> Component.translatable("bundleintooltip.config.mode.none");
            case VANILLA -> Component.translatable("bundleintooltip.config.mode.vanilla");
            case CLASSIC -> Component.translatable("bundleintooltip.config.mode.classic");
            case HYBRID -> Component.translatable("bundleintooltip.config.mode.hybrid");
            case VANILLA_REVISED -> Component.translatable("bundleintooltip.config.mode.vanilla_revised");
        };
    }

    private static Component labelForDisplayMode(boolean showBar) {
        return showBar
            ? Component.translatable("bundleintooltip.config.slot_style.vanilla")
            : Component.translatable("bundleintooltip.config.slot_style.classic");
    }

    private static Component labelForSlotStyle(BundleInTooltipConfig.SlotTextureMode mode) {
        return switch (mode) {
            case VANILLA -> Component.translatable("bundleintooltip.config.slot_style.vanilla");
            case EXPERIMENT -> Component.translatable("bundleintooltip.config.slot_style.experiment");
            case CLASSIC -> Component.translatable("bundleintooltip.config.slot_style.classic");
        };
    }
}
