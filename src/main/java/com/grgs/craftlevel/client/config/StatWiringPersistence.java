package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega e sobrescreve player_stats.json / mob_stats.json a partir da sessão de edição. */
final class StatWiringPersistence {

    private static StatWiringEditorState session;
    private static String fileName;

    private StatWiringPersistence() {}

    static StatWiringEditorState session() {
        if (session == null) {
            session = new StatWiringEditorState();
        }
        return session;
    }

    static void beginEditing(Minecraft minecraft, String fileName) {
        StatWiringPersistence.fileName = fileName;
        session = StatWiringEditorState.loadFrom(configFile(minecraft, fileName));
    }

    static void save(Minecraft minecraft) {
        try {
            Path dir = configDir(minecraft);
            Files.createDirectories(dir);
            Files.writeString(configFile(minecraft, fileName), ConfigManager.toPrettyJson(session().toJson()));
            ConfigManager.loadAllConfigs(dir);
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save {}.json", fileName, e);
        }
    }

    private static Path configDir(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel");
    }

    private static Path configFile(Minecraft minecraft, String fileName) {
        return configDir(minecraft).resolve(fileName + ".json");
    }
}
