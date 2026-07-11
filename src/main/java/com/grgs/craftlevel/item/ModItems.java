package com.grgs.craftlevel.item;

import com.grgs.craftlevel.CraftLevel;
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

    private ModItems() {}
}
