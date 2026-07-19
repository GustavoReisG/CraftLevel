package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega e sobrescreve souls.json a partir da sessão de edição. */
final class SoulsPersistence {

    private static SoulsEditorState session;

    private SoulsPersistence() {}

    static SoulsEditorState session() {
        if (session == null) {
            session = new SoulsEditorState();
        }
        return session;
    }

    static void beginEditing(Minecraft minecraft) {
        session = SoulsEditorState.loadFrom(configFile(minecraft));
    }

    static void save(Minecraft minecraft) {
        try {
            Path dir = configDir(minecraft);
            Files.createDirectories(dir);
            Files.writeString(configFile(minecraft), ConfigManager.toPrettyJson(session().toJson()));
            ConfigManager.loadAllConfigs(dir);
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save souls.json", e);
        }
    }

    private static Path configDir(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel");
    }

    private static Path configFile(Minecraft minecraft) {
        return configDir(minecraft).resolve("souls.json");
    }
}
