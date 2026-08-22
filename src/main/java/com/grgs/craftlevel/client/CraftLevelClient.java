package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.client.config.CraftLevelConfigScreen;
import com.grgs.craftlevel.entity.ModEntities;
import com.grgs.craftlevel.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
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

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.INFERNO_BOLT.get(),
                context -> new ThrownItemRenderer<>(context, 3.0F, true)
        );
        event.registerEntityRenderer(ModEntities.SOUL_LANCE.get(), ThrownTridentRenderer::new);
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
