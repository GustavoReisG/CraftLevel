package com.grgs.craftlevel.menu;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registro de menus do mod. */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CraftLevel.MODID);

    public static final RegistryObject<MenuType<EmeraldWalletMenu>> EMERALD_WALLET = MENUS.register(
            "emerald_wallet",
            () -> IForgeMenuType.create((id, inv, data) -> new EmeraldWalletMenu(id, inv))
    );

    private ModMenus() {}
}
