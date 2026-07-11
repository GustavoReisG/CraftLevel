package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.client.config.CraftLevelConfigScreen;
import com.grgs.craftlevel.menu.ModMenus;
import com.grgs.craftlevel.client.EmeraldWalletScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registro client-side: botão Config na lista de mods. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CraftLevelClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CraftLevelClient.registerConfigScreen();
            CraftLevelClient.registerScreens();
        });
    }

    private static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(CraftLevelConfigScreen::new)
        );
    }

    private static void registerScreens() {
        MenuScreens.register(ModMenus.EMERALD_WALLET.get(), EmeraldWalletScreen::new);
    }
}
