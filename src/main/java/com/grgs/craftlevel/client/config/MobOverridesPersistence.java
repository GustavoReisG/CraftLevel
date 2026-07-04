package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega e salva mob_overrides.json a partir da sessão de edição. */
final class MobOverridesPersistence {

    private static MobOverridesEditorState session;

    private MobOverridesPersistence() {}

    static MobOverridesEditorState session() {
        if (session == null) {
            session = new MobOverridesEditorState();
        }
        return session;
    }

    static void beginEditing(Minecraft minecraft) {
        session = MobOverridesEditorState.loadFrom(configFile(minecraft));
    }

    static void save(Minecraft minecraft) {
        try {
            Path dir = configDir(minecraft);
            Files.createDirectories(dir);
            Files.writeString(configFile(minecraft), ConfigManager.toPrettyJson(session().toJson()));
            ConfigManager.loadAllConfigs(dir);
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save mob_overrides.json", e);
        }
    }

    private static Path configDir(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel");
    }

    private static Path configFile(Minecraft minecraft) {
        return configDir(minecraft).resolve("mob_overrides.json");
    }
}
