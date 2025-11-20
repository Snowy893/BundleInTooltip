package com.bundleintooltip.client;

import com.bundleintooltip.client.gui.BundleCapacityClientTooltip;
import com.bundleintooltip.client.gui.BundleCapacityTooltip;
import com.bundleintooltip.client.gui.SelectableBundleTooltip;
import com.bundleintooltip.client.gui.SelectableClientBundleTooltip;
import com.bundleintooltip.common.BundleHelper;
import com.bundleintooltip.config.BundleInTooltipConfig;
import com.mojang.datafixers.util.Either;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.Event;
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
        MinecraftForge.EVENT_BUS.addListener(BundleInTooltipClient::onMouseReleased);
        MinecraftForge.EVENT_BUS.addListener(BundleInTooltipClient::onItemTooltip);
    }

    private static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        if (event.getButton() != 1) {
            return;
        }

        if (BundleSelectionTracker.INSTANCE.consumeReleaseSuppression()) {
            event.setCanceled(true);
            event.setResult(Event.Result.DENY);
            return;
        }

        if (!containerScreen.getMenu().getCarried().isEmpty() && BundleHelper.isBundle(containerScreen.getMenu().getCarried())) {
            event.setCanceled(true);
        }
    }

    private static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(SelectableBundleTooltip.class, SelectableClientBundleTooltip::new);
        event.register(BundleCapacityTooltip.class, BundleCapacityClientTooltip::new);
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
                int gridColumns = Math.max(2, (int)Math.ceil(Math.sqrt(copy.size())));
                if (BundleInTooltipConfig.slotTextureMode().isModern()) {
                    gridColumns = Math.max(4, gridColumns);
                }
                int slotStyleSpacing = BundleInTooltipConfig.slotTextureMode() == BundleInTooltipConfig.SlotTextureMode.VANILLA ? 26 : 18;
                int gridWidth;
                if (BundleInTooltipConfig.slotTextureMode() == BundleInTooltipConfig.SlotTextureMode.VANILLA) {
                    gridWidth = (gridColumns - 1) * slotStyleSpacing + 20;
                } else {
                    gridWidth = gridColumns * slotStyleSpacing + 2;
                }
                int titleWidth = minecraft.font.width(hoveredStack.getHoverName());
                int displayWidth;
                if (copy.isEmpty()) {
                    int emptyWidth = Math.max(
                        minecraft.font.width(Component.translatable("bundleintooltip.tooltip.empty_hint.line1")),
                        minecraft.font.width(Component.translatable("bundleintooltip.tooltip.empty_hint.line2"))
                    ) + 10;
                    displayWidth = Math.max(titleWidth, emptyWidth);
                } else {
                    displayWidth = Math.max(gridWidth, titleWidth);
                }
                IntSupplier selection = BundleSelectionTracker.INSTANCE.bindTooltip(containerScreen, hoveredSlot, hoveredStack, copy);
                SelectableBundleTooltip selectable = new SelectableBundleTooltip(copy, bundleTooltip.getWeight(), selection, displayWidth);
                elements.set(i, Either.right(selectable));
                if (BundleInTooltipConfig.showCapacityBar()) {
                    elements.add(i + 1, Either.right(new BundleCapacityTooltip(bundleTooltip.getWeight(), displayWidth)));
                }
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

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null) {
            hoveredSlot = findHoveredSlot(containerScreen, event.getMouseX(), event.getMouseY());
        }

        if (event.getButton() == 1) {
            ItemStack carried = containerScreen.getMenu().getCarried();
            if (hoveredSlot != null && hoveredSlot.hasItem()) {
                if (BundleSelectionTracker.INSTANCE.swapWithBundle(containerScreen, hoveredSlot)) {
                    event.setCanceled(true);
                    event.setResult(Event.Result.DENY);
                    return;
                }
            }

            if (!carried.isEmpty() && BundleHelper.isBundle(carried)) {
                if (BundleSelectionTracker.INSTANCE.dropFromCarriedBundle(containerScreen, hoveredSlot)) {
                    event.setCanceled(true);
                    event.setResult(Event.Result.DENY);
                    return;
                }
            }

            Slot slot = BundleSelectionTracker.INSTANCE.resolveSlot(containerScreen, hoveredSlot);
            if (slot == null || !BundleHelper.isBundle(slot.getItem())) {
                return;
            }

            if (BundleSelectionTracker.INSTANCE.handleClick(containerScreen, slot)) {
                event.setCanceled(true);
                event.setResult(Event.Result.DENY);
            }
            return;
        }

        if (event.getButton() == 0) {
            if (hoveredSlot != null && BundleHelper.isBundle(hoveredSlot.getItem())) {
                if (BundleSelectionTracker.INSTANCE.handleDeposit(containerScreen, hoveredSlot)) {
                    event.setCanceled(true);
                    event.setResult(Event.Result.DENY);
                    return;
                }
            }

            if (BundleSelectionTracker.INSTANCE.handleCarriedBundlePickup(containerScreen, hoveredSlot)) {
                event.setCanceled(true);
                event.setResult(Event.Result.DENY);
            }
        }
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        if (!BundleInTooltipConfig.showCapacityBar() || !BundleHelper.isBundle(event.getItemStack())) {
            return;
        }

        Iterator<Component> iterator = event.getToolTip().iterator();
        while (iterator.hasNext()) {
            Component component = iterator.next();
            if (component.getContents() instanceof TranslatableContents contents
                && "item.minecraft.bundle.fullness".equals(contents.getKey())) {
                iterator.remove();
                break;
            }
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
