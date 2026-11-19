package com.grgs.craftlevel.recipe;

import com.grgs.craftlevel.item.ItemRunes;
import com.grgs.craftlevel.item.RuneItem;
import com.grgs.craftlevel.item.RuneKind;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;

/**
 * Smithing: template = runa, base = arma/armadura, addition = vazio.
 * Até {@link ItemRunes#MAX_PER_ITEM} runas distintas por item.
 */
public class RuneSmithingRecipe implements SmithingRecipe {

    private final ResourceLocation id;

    public RuneSmithingRecipe(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return assembleResult(container) != null;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        ItemStack result = assembleResult(container);
        return result == null ? ItemStack.EMPTY : result;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.getItem() instanceof RuneItem;
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return ItemRunes.canReceiveRunes(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RUNE_SMITHING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    private static ItemStack assembleResult(Container container) {
        ItemStack template = container.getItem(0);
        ItemStack base = container.getItem(1);
        ItemStack addition = container.getItem(2);
        if (!(template.getItem() instanceof RuneItem runeItem) || !addition.isEmpty()) {
            return null;
        }
        RuneKind kind = runeItem.kind();
        if (!ItemRunes.canApply(base, kind)) {
            return null;
        }
        ItemStack result = ItemRunes.withRune(base, kind);
        return result.isEmpty() ? null : result;
    }

    public static class Serializer implements RecipeSerializer<RuneSmithingRecipe> {
        @Override
        public RuneSmithingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new RuneSmithingRecipe(id);
        }

        @Override
        public RuneSmithingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new RuneSmithingRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RuneSmithingRecipe recipe) {
            // sem payload
        }
    }
}
