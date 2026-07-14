package com.grgs.craftlevel.villager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Slot resolvido (item concreto + quantidade) para cache de roll. */
public record ResolvedTradeSlot(String itemId, int count) {

    public static ResolvedTradeSlot fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return new ResolvedTradeSlot("", 0);
        }
        return new ResolvedTradeSlot(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount()
        );
    }

    public ItemStack toStack() {
        if (itemId == null || itemId.isBlank() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        if (key == null) {
            return ItemStack.EMPTY;
        }
        var item = BuiltInRegistries.ITEM.get(key);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }
}
