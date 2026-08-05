package com.grgs.craftlevel.item;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registro de items do mod. */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CraftLevel.MODID);

    public static final RegistryObject<Item> ANCESTRAL_GEM = ITEMS.register(
            "ancestral_gem",
            () -> new AncestralGemItem(new Item.Properties())
    );

    public static final RegistryObject<Item> IMBUE_FLASK = ITEMS.register(
            "imbue_flask",
            () -> new Item(new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> IMBUE_POTION = ITEMS.register(
            "imbue_potion",
            () -> new ImbuePotionItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> EMERALD_WALLET = ITEMS.register(
            "emerald_wallet",
            () -> new EmeraldWalletItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> EMERALD_STORAGE_1 = ITEMS.register(
            "emerald_storage_1",
            () -> new EmeraldStorageItem(1, new Item.Properties())
    );

    public static final RegistryObject<Item> EMERALD_STORAGE_9 = ITEMS.register(
            "emerald_storage_9",
            () -> new EmeraldStorageItem(9, new Item.Properties())
    );

    public static final RegistryObject<Item> EMERALD_STORAGE_64 = ITEMS.register(
            "emerald_storage_64",
            () -> new EmeraldStorageItem(64, new Item.Properties())
    );

    public static final RegistryObject<Item> EMERALD_STORAGE_81 = ITEMS.register(
            "emerald_storage_81",
            () -> new EmeraldStorageItem(81, new Item.Properties())
    );

    public static final RegistryObject<Item> EMERALD_STORAGE_576 = ITEMS.register(
            "emerald_storage_576",
            () -> new EmeraldStorageItem(576, new Item.Properties())
    );

    public static final RegistryObject<Item> EMERALD_STORAGE_5184 = ITEMS.register(
            "emerald_storage_5184",
            () -> new EmeraldStorageItem(5184, new Item.Properties())
    );

    public static final RegistryObject<Item> COMPRESSED_EMERALD_BLOCK = ITEMS.register(
            "compressed_emerald_block",
            () -> new BlockItem(ModBlocks.COMPRESSED_EMERALD_BLOCK.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> PLAYER_GRAVESTONE = ITEMS.register(
            "player_gravestone",
            () -> new BlockItem(ModBlocks.PLAYER_GRAVESTONE.get(), new Item.Properties())
    );

    private ModItems() {}
}
