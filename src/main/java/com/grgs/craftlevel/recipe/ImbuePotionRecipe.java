package com.grgs.craftlevel.recipe;

import com.grgs.craftlevel.item.ImbuePotionItem;
import com.grgs.craftlevel.item.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Frasco de embuir + poção + botão de carvalho (+ tinta opcional)
 * → poção de embuir daquela poção; tinta pinta a tampa (sem tinta = marrom).
 */
public class ImbuePotionRecipe extends CustomRecipe {

    public ImbuePotionRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return findInputs(container) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        Inputs inputs = findInputs(container);
        if (inputs == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(ModItems.IMBUE_POTION.get());
        PotionUtils.setPotion(result, PotionUtils.getPotion(inputs.potion));

        CompoundTag potionTag = inputs.potion.getTag();
        if (potionTag != null && potionTag.contains("CustomPotionEffects", Tag.TAG_LIST)) {
            result.getOrCreateTag().put(
                    "CustomPotionEffects",
                    potionTag.getList("CustomPotionEffects", Tag.TAG_COMPOUND).copy()
            );
        }
        if (potionTag != null && potionTag.contains("CustomPotionColor", Tag.TAG_INT)) {
            result.getOrCreateTag().putInt("CustomPotionColor", potionTag.getInt("CustomPotionColor"));
        }

        if (inputs.dye != null) {
            int color = inputs.dye.getDyeColor().getFireworkColor();
            result.getOrCreateTag().putInt(ImbuePotionItem.CORK_COLOR_TAG, color);
        }

        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return new ItemStack(ModItems.IMBUE_POTION.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.IMBUE_POTION.get();
    }

    private static Inputs findInputs(CraftingContainer container) {
        ItemStack flask = ItemStack.EMPTY;
        ItemStack potion = ItemStack.EMPTY;
        ItemStack button = ItemStack.EMPTY;
        DyeItem dye = null;
        int dyeCount = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.IMBUE_FLASK.get())) {
                if (!flask.isEmpty()) {
                    return null;
                }
                flask = stack;
            } else if (stack.is(Items.POTION)) {
                if (!potion.isEmpty()) {
                    return null;
                }
                potion = stack;
            } else if (stack.is(Items.OAK_BUTTON)) {
                if (!button.isEmpty()) {
                    return null;
                }
                button = stack;
            } else if (stack.getItem() instanceof DyeItem dyeItem) {
                dyeCount++;
                if (dyeCount > 1) {
                    return null;
                }
                dye = dyeItem;
            } else {
                return null;
            }
        }

        if (flask.isEmpty() || potion.isEmpty() || button.isEmpty()) {
            return null;
        }
        return new Inputs(potion, dye);
    }

    private record Inputs(ItemStack potion, DyeItem dye) {}
}
