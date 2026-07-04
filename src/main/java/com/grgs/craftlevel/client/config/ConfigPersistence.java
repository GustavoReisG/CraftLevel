package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega e sobrescreve config.json a partir da sessão de edição da UI. */
final class ConfigPersistence {

    private static ConfigEditorState session;

    private ConfigPersistence() {}

    static ConfigEditorState session() {
        if (session == null) {
            session = new ConfigEditorState();
        }
        return session;
    }

    static void beginEditing(Minecraft minecraft) {
        session = ConfigEditorState.loadFrom(configFile(minecraft));
    }

    static void save(Minecraft minecraft) {
        try {
            Path dir = configDir(minecraft);
            Files.createDirectories(dir);
            Files.writeString(configFile(minecraft), ConfigManager.toPrettyJson(session().toJson()));
            ConfigManager.loadAllConfigs(dir);
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save config.json", e);
        }
    }

    private static Path configDir(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel");
    }

    private static Path configFile(Minecraft minecraft) {
        return configDir(minecraft).resolve("config.json");
    }
}
