package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega e sobrescreve mob_levels.json a partir da sessão de edição. */
final class MobLevelsPersistence {

    private static MobLevelsEditorState session;

    private MobLevelsPersistence() {}

    static MobLevelsEditorState session() {
        if (session == null) {
            session = new MobLevelsEditorState();
        }
        return session;
    }

    static void beginEditing(Minecraft minecraft) {
        session = MobLevelsEditorState.loadFrom(configFile(minecraft));
    }

    static void save(Minecraft minecraft) {
        try {
            Path dir = configDir(minecraft);
            Files.createDirectories(dir);
            Files.writeString(configFile(minecraft), ConfigManager.toPrettyJson(session().toJson()));
            ConfigManager.loadAllConfigs(dir);
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save mob_levels.json", e);
        }
    }

    private static Path configDir(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel");
    }

    private static Path configFile(Minecraft minecraft) {
        return configDir(minecraft).resolve("mob_levels.json");
    }
}
