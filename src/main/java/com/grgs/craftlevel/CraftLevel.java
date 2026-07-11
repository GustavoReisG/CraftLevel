package com.grgs.craftlevel;

import com.mojang.logging.LogUtils;
import com.grgs.craftlevel.capability.IMobLevel;
import com.grgs.craftlevel.capability.IPlayerEmeraldWallet;
import com.grgs.craftlevel.capability.IPlayerLevel;
import com.grgs.craftlevel.capability.IPlayerVillagerBalance;
import com.grgs.craftlevel.item.ModItems;
import com.grgs.craftlevel.menu.ModMenus;
import com.grgs.craftlevel.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Ponto de entrada do mod Forge.
 * <p>
 * Registra capabilities de level, pacotes de rede e o event bus global.
 */
@Mod(CraftLevel.MODID)
public class CraftLevel {

    public static final String MODID = "craftlevel";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CraftLevel(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Craft Level initialized!");
    }

    /** Setup comum: capabilities + canal de rede. */
    private void commonSetup(final FMLCommonSetupEvent event) {
        CapabilityManager.get(new CapabilityToken<IPlayerLevel>() {});
        CapabilityManager.get(new CapabilityToken<IMobLevel>() {});
        CapabilityManager.get(new CapabilityToken<IPlayerEmeraldWallet>() {});
        CapabilityManager.get(new CapabilityToken<IPlayerVillagerBalance>() {});
        PacketHandler.register();
        LOGGER.info("Craft Level common setup complete!");
    }
}
