package com.grgs.craftlevel.recipe;

import com.grgs.craftlevel.item.ImbuePotionItem;
import com.grgs.craftlevel.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraftforge.common.brewing.IBrewingRecipe;

/**
 * Espelha as receitas vanilla do brewing stand para {@link ModItems#IMBUE_POTION}.
 * Preserva a cor da rolha ({@link ImbuePotionItem#CORK_COLOR_TAG}).
 */
public final class ImbuePotionBrewingRecipe implements IBrewingRecipe {

    @Override
    public boolean isInput(ItemStack input) {
        return input.is(ModItems.IMBUE_POTION.get());
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return PotionBrewing.isIngredient(ingredient);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) {
            return ItemStack.EMPTY;
        }

        ItemStack vanilla = new ItemStack(Items.POTION);
        CompoundTag tag = input.getTag();
        if (tag != null) {
            CompoundTag copy = tag.copy();
            copy.remove(ImbuePotionItem.CORK_COLOR_TAG);
            vanilla.setTag(copy);
        }

        ItemStack mixed = PotionBrewing.mix(ingredient, vanilla);
        if (mixed.isEmpty() || !mixed.is(Items.POTION)) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(ModItems.IMBUE_POTION.get());
        if (mixed.getTag() != null) {
            result.setTag(mixed.getTag().copy());
        }
        if (tag != null && tag.contains(ImbuePotionItem.CORK_COLOR_TAG)) {
            result.getOrCreateTag().putInt(ImbuePotionItem.CORK_COLOR_TAG, tag.getInt(ImbuePotionItem.CORK_COLOR_TAG));
        }
        return result;
    }
}
