package com.grgs.craftlevel.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Helpers para identificar e avaliar itens Storage do emerald bank. */
public final class EmeraldStorageItems {

    private EmeraldStorageItems() {}

    public static boolean isStorage(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof EmeraldStorageItem;
    }

    public static boolean isStorage(Item item) {
        return item instanceof EmeraldStorageItem;
    }

    public static int unitValue(ItemStack stack) {
        if (stack.getItem() instanceof EmeraldStorageItem storage) {
            return storage.getBankValue();
        }
        return 0;
    }

    public static int unitValue(Item item) {
        if (item instanceof EmeraldStorageItem storage) {
            return storage.getBankValue();
        }
        return 0;
    }

    public static long stackValue(ItemStack stack) {
        return (long) unitValue(stack) * stack.getCount();
    }
}
