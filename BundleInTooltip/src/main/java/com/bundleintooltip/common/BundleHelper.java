package com.bundleintooltip.common;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class BundleHelper {
    private static final String ITEMS_TAG = "Items";
    private static final int MAX_WEIGHT = 64;
    private static final TagKey<Item> FORGE_BUNDLE_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "bundles"));

    private BundleHelper() {
    }

    public static boolean isBundle(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof BundleItem || stack.is(FORGE_BUNDLE_TAG);
    }

    public static NonNullList<ItemStack> copyContents(ItemStack bundle) {
        NonNullList<ItemStack> contents = NonNullList.create();
        CompoundTag tag = bundle.getTag();
        if (tag == null || !tag.contains(ITEMS_TAG, Tag.TAG_LIST)) {
            return contents;
        }

        ListTag listTag = tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            contents.add(ItemStack.of(listTag.getCompound(i)));
        }
        return contents;
    }

    public static Optional<ItemStack> removeAt(ItemStack bundle, int index) {
        CompoundTag tag = bundle.getOrCreateTag();
        if (!tag.contains(ITEMS_TAG, Tag.TAG_LIST)) {
            return Optional.empty();
        }

        ListTag listTag = tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        if (index < 0 || index >= listTag.size()) {
            return Optional.empty();
        }

        ItemStack extracted = ItemStack.of(listTag.getCompound(index));
        listTag.remove(index);
        if (listTag.isEmpty()) {
            bundle.removeTagKey(ITEMS_TAG);
        }
        return Optional.of(extracted);
    }

    public static int getContentWeight(ItemStack bundle) {
        return copyContents(bundle).stream().mapToInt(BundleHelper::getStackWeight).sum();
    }

    public static int getRemainingCapacity(ItemStack bundle) {
        return Math.max(0, MAX_WEIGHT - getContentWeight(bundle));
    }

    public static int addToBundle(ItemStack bundle, ItemStack source, int maximum) {
        if (!isBundle(bundle) || source.isEmpty() || maximum <= 0) {
            return 0;
        }

        int unitWeight = getUnitWeight(source);
        if (unitWeight <= 0) {
            return 0;
        }

        int remaining = getRemainingCapacity(bundle);
        if (remaining < unitWeight) {
            return 0;
        }

        int allowed = Math.min(Math.min(source.getCount(), maximum), remaining / unitWeight);
        if (allowed <= 0) {
            return 0;
        }

        NonNullList<ItemStack> contents = copyContents(bundle);
        int toInsert = allowed;
        for (ItemStack existing : contents) {
            if (ItemStack.isSameItemSameTags(existing, source)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space <= 0) {
                    continue;
                }
                int moved = Math.min(space, toInsert);
                if (moved > 0) {
                    existing.grow(moved);
                    toInsert -= moved;
                    if (toInsert <= 0) {
                        break;
                    }
                }
            }
        }

        while (toInsert > 0) {
            int amount = Math.min(source.getMaxStackSize(), toInsert);
            contents.add(source.copyWithCount(amount));
            toInsert -= amount;
        }

        writeContents(bundle, contents);
        return allowed;
    }

    private static int getStackWeight(ItemStack stack) {
        if (isBundle(stack)) {
            return Math.max(1, getContentWeight(stack)) * stack.getCount();
        }
        return getUnitWeight(stack) * stack.getCount();
    }

    private static int getUnitWeight(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (isBundle(stack)) {
            return Math.max(1, getContentWeight(stack));
        }
        return Math.max(1, (int)Math.ceil((double)MAX_WEIGHT / (double)stack.getMaxStackSize()));
    }

    private static void writeContents(ItemStack bundle, NonNullList<ItemStack> contents) {
        ListTag listTag = new ListTag();
        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                listTag.add(stack.save(new CompoundTag()));
            }
        }

        if (listTag.isEmpty()) {
            bundle.removeTagKey(ITEMS_TAG);
        } else {
            bundle.getOrCreateTag().put(ITEMS_TAG, listTag);
        }
    }
}
