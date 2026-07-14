package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.client.CraftLevelTexturesPack;
import com.grgs.craftlevel.config.ConfigManager;
import com.grgs.craftlevel.villager.VillagerTradeService;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega e sobrescreve villager.json a partir da sessão de edição. */
final class VillagerPersistence {

    private static VillagerEditorState session;

    private VillagerPersistence() {}

    static VillagerEditorState session() {
        if (session == null) {
            session = new VillagerEditorState();
        }
        return session;
    }

    static void beginEditing(Minecraft minecraft) {
        session = VillagerEditorState.loadFrom(configFile(minecraft));
    }

    static void save(Minecraft minecraft) {
        try {
            Path dir = configDir(minecraft);
            Files.createDirectories(dir);
            Files.writeString(configFile(minecraft), ConfigManager.toPrettyJson(session().toJson()));
            ConfigManager.loadAllConfigs(dir);
            CraftLevelTexturesPack.seedProfessionTextures();
            MinecraftServer server = minecraft.getSingleplayerServer();
            if (server != null) {
                VillagerTradeService.onReload();
            }
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save villager.json", e);
        }
    }

    private static Path configDir(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel");
    }

    private static Path configFile(Minecraft minecraft) {
        return configDir(minecraft).resolve("villager.json");
    }
}
