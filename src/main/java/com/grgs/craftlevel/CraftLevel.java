package com.grgs.craftlevel;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import com.grgs.craftlevel.network.PacketHandler;
import org.slf4j.Logger;
import com.grgs.craftlevel.capability.IPlayerLevel;
import com.grgs.craftlevel.capability.IMobLevel;

/** Classe principal do mod — ponto de entrada registrado pelo Forge. */
@Mod(CraftLevel.MODID)
public class CraftLevel {

    public static final String MODID = "craftlevel";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Registra listeners de setup e o event bus global do Forge. */
    public CraftLevel(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Craft Level initialized!");
    }

    /** Inicialização comum: registra capabilities e pacotes de rede. */
    private void commonSetup(final FMLCommonSetupEvent event) {
        CapabilityManager.get(new CapabilityToken<IPlayerLevel>() {});
        CapabilityManager.get(new CapabilityToken<IMobLevel>() {});
        PacketHandler.register();
        LOGGER.info("Craft Level common setup complete!");
    }
}
