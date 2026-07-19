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

    public static final RegistryObject<Item> EMERALD_WALLET = ITEMS.register(
            "emerald_wallet",
            () -> new EmeraldWalletItem(new Item.Properties().stacksTo(1))
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
