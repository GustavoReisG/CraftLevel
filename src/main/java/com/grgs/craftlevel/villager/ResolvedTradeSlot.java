package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.CraftLevel;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Slot resolvido (item concreto + quantidade + NBT) para cache de roll. */
public record ResolvedTradeSlot(String itemId, int count, String nbt) {

    public static ResolvedTradeSlot fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return new ResolvedTradeSlot("", 0, "");
        }
        CompoundTag tag = stack.getTag();
        return new ResolvedTradeSlot(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(),
                tag != null && !tag.isEmpty() ? tag.toString() : ""
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
        ItemStack stack = new ItemStack(item, count);
        applyNbt(stack, nbt);
        return stack;
    }

    /** Aplica SNBT sem curingas (já resolvidos no roll). */
    public static void applyNbt(ItemStack stack, String nbt) {
        if (stack.isEmpty() || nbt == null || nbt.isBlank()) {
            return;
        }
        try {
            stack.setTag(TagParser.parseTag(nbt));
        } catch (CommandSyntaxException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Invalid trade slot NBT: {}", nbt, e);
        }
    }
}
