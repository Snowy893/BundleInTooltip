package com.bundleintooltip.client;

import com.bundleintooltip.client.gui.SelectableBundleTooltip;
import com.bundleintooltip.client.gui.SelectableClientBundleTooltip;
import com.bundleintooltip.common.BundleHelper;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class BundleInTooltipClient {
    private BundleInTooltipClient() {
    }

    public static void init() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(BundleInTooltipClient::registerTooltipFactories);
        MinecraftForge.EVENT_BUS.addListener(BundleInTooltipClient::onTooltipGathered);
        MinecraftForge.EVENT_BUS.addListener(BundleInTooltipClient::onMouseScrolled);
        MinecraftForge.EVENT_BUS.addListener(BundleInTooltipClient::onMouseClicked);
    }

    private static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(SelectableBundleTooltip.class, SelectableClientBundleTooltip::new);
    }

    private static void onTooltipGathered(RenderTooltipEvent.GatherComponents event) {
        ItemStack hoveredStack = event.getItemStack();
        if (!BundleHelper.isBundle(hoveredStack)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        AbstractContainerScreen<?> containerScreen = minecraft.screen instanceof AbstractContainerScreen<?> screen ? screen : null;
        Slot hoveredSlot = containerScreen != null ? containerScreen.getSlotUnderMouse() : null;

        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        for (int i = 0; i < elements.size(); i++) {
            TooltipComponent component = elements.get(i).right().orElse(null);
            if (component instanceof BundleTooltip bundleTooltip) {
                NonNullList<ItemStack> copy = NonNullList.create();
                copy.addAll(bundleTooltip.getItems());
                IntSupplier selection = BundleSelectionTracker.INSTANCE.bindTooltip(containerScreen, hoveredSlot, hoveredStack, copy);
                elements.set(i, Either.right(new SelectableBundleTooltip(copy, bundleTooltip.getWeight(), selection)));
                break;
            }
        }
    }

    private static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null) {
            hoveredSlot = findHoveredSlot(containerScreen, event.getMouseX(), event.getMouseY());
        }
        Slot slot = BundleSelectionTracker.INSTANCE.resolveSlot(containerScreen, hoveredSlot);
        if (slot == null || !BundleHelper.isBundle(slot.getItem())) {
            return;
        }

        if (BundleSelectionTracker.INSTANCE.handleScroll(containerScreen, slot, event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    private static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        if (event.getButton() != 1) {
            return;
        }

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null) {
            hoveredSlot = findHoveredSlot(containerScreen, event.getMouseX(), event.getMouseY());
        }
        Slot slot = BundleSelectionTracker.INSTANCE.resolveSlot(containerScreen, hoveredSlot);
        if (slot == null || !BundleHelper.isBundle(slot.getItem())) {
            return;
        }

        if (BundleSelectionTracker.INSTANCE.handleClick(containerScreen, slot)) {
            event.setCanceled(true);
        }
    }

    @Nullable
    private static Slot findHoveredSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) {
                continue;
            }

            if (isWithinSlot(screen, slot, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean isWithinSlot(AbstractContainerScreen<?> screen, Slot slot, double mouseX, double mouseY) {
        double localX = mouseX - screen.getGuiLeft();
        double localY = mouseY - screen.getGuiTop();
        return localX >= slot.x - 1 && localX < slot.x + 17 && localY >= slot.y - 1 && localY < slot.y + 17;
    }
}
