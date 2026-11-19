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

    public static final RegistryObject<Item> ALTAR_OF_DEATH = ITEMS.register(
            "altar_of_death",
            () -> new BlockItem(ModBlocks.ALTAR_OF_DEATH.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> ARENA_PORTAL = ITEMS.register(
            "arena_portal",
            () -> new BlockItem(ModBlocks.ARENA_PORTAL.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> RECALL_RUNE = ITEMS.register(
            "recall_rune",
            () -> new RecallRuneItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> TOTEM_OF_VITALITY = ITEMS.register(
            "totem_of_vitality",
            () -> new ArenaTotemItem(ArenaTotemItem.Kind.VITALITY, new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> TOTEM_OF_OVERCHARGE = ITEMS.register(
            "totem_of_overcharge",
            () -> new ArenaTotemItem(ArenaTotemItem.Kind.OVERCHARGE, new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> TOTEM_OF_BLESSING = ITEMS.register(
            "totem_of_blessing",
            () -> new ArenaTotemItem(ArenaTotemItem.Kind.BLESSING, new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> RUNE_OF_UNBREAKABLE = ITEMS.register(
            "rune_of_unbreakable",
            () -> new RuneItem(RuneKind.UNBREAKABLE, new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> RUNE_OF_VELOCE = ITEMS.register(
            "rune_of_veloce",
            () -> new RuneItem(RuneKind.VELOCE, new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> RUNE_OF_REACH = ITEMS.register(
            "rune_of_reach",
            () -> new RuneItem(RuneKind.REACH, new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> RUNE_OF_FORTUNE = ITEMS.register(
            "rune_of_fortune",
            () -> new RuneItem(RuneKind.FORTUNE, new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> RUNE_OF_RETURN = ITEMS.register(
            "rune_of_return",
            () -> new RuneItem(RuneKind.RETURN, new Item.Properties().stacksTo(16))
    );

    public static final RegistryObject<Item> RUNE_OF_SOULBOUND = ITEMS.register(
            "rune_of_soulbound",
            () -> new RuneItem(RuneKind.SOULBOUND, new Item.Properties().stacksTo(16))
    );

    private ModItems() {}
}
