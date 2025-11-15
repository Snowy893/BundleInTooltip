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
}
