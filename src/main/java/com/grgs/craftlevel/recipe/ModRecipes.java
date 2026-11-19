package com.grgs.craftlevel.recipe;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Serializers de crafting do Craft Level. */
public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, CraftLevel.MODID);

    public static final RegistryObject<RecipeSerializer<ImbuePotionRecipe>> IMBUE_POTION =
            SERIALIZERS.register(
                    "imbue_potion",
                    () -> new SimpleCraftingRecipeSerializer<>(ImbuePotionRecipe::new)
            );

    public static final RegistryObject<RecipeSerializer<RuneSmithingRecipe>> RUNE_SMITHING =
            SERIALIZERS.register("rune_smithing", RuneSmithingRecipe.Serializer::new);

    private ModRecipes() {}
}
