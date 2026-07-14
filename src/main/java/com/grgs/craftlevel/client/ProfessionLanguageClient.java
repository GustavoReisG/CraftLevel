package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.villager.ProfessionLanguages;
import net.minecraft.locale.Language;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Injeta traduções dinâmicas de profissões no Language do cliente. */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ProfessionLanguageClient {

    private ProfessionLanguageClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ProfessionLanguages::refresh);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ProfessionLanguageReloadListener());
    }

    public static void inject() {
        Language current = Language.getInstance();
        ProfessionInjectedLanguage injected;
        if (current instanceof ProfessionInjectedLanguage existing) {
            injected = existing;
        } else {
            injected = new ProfessionInjectedLanguage(current);
            Language.inject(injected);
        }
        injected.setDynamic(ProfessionLanguages.entries());
    }
}
