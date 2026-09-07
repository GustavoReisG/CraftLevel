package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega/salva altar_conversion.json. */
final class AltarConversionPersistence {

    private static AltarConversionEditorState session;

    private AltarConversionPersistence() {}

    static AltarConversionEditorState session() {
        if (session == null) {
            session = new AltarConversionEditorState();
        }
        return session;
    }

    static void beginEditing(Minecraft minecraft) {
        session = AltarConversionEditorState.loadFrom(configFile(minecraft));
    }

    static void ensureSession(Minecraft minecraft) {
        if (session == null) {
            beginEditing(minecraft);
        }
    }

    static void save(Minecraft minecraft) {
        try {
            Path dir = configDir(minecraft);
            Files.createDirectories(dir);
            Files.writeString(configFile(minecraft), ConfigManager.toPrettyJson(session().toJson()));
            ConfigManager.loadAllConfigs(dir);
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save altar_conversion.json", e);
        }
    }

    private static Path configDir(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel");
    }

    private static Path configFile(Minecraft minecraft) {
        return configDir(minecraft).resolve("altar_conversion.json");
    }
}
