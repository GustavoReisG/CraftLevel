package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigManager;
import com.grgs.craftlevel.economy.EconomyService;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega e sobrescreve economy.json a partir da sessão de edição. */
final class EconomyBalancePersistence {

    private static EconomyBalanceEditorState session;

    private EconomyBalancePersistence() {}

    static EconomyBalanceEditorState session() {
        if (session == null) {
            session = new EconomyBalanceEditorState();
        }
        return session;
    }

    static void beginEditing(Minecraft minecraft) {
        session = EconomyBalanceEditorState.loadFrom(configFile(minecraft));
    }

    static void save(Minecraft minecraft) {
        try {
            Path dir = configDir(minecraft);
            Files.createDirectories(dir);
            Files.writeString(configFile(minecraft), ConfigManager.toPrettyJson(session().toJson()));
            ConfigManager.loadAllConfigs(dir);
            MinecraftServer server = minecraft.getSingleplayerServer();
            if (server != null) {
                EconomyService.onEconomyConfigReload(server);
            }
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save economy.json", e);
        }
    }

    private static Path configDir(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel");
    }

    private static Path configFile(Minecraft minecraft) {
        return configDir(minecraft).resolve("economy.json");
    }
}
