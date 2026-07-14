package com.grgs.craftlevel.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.CraftLevel;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Leitura de villager.json da pasta config (disponível no boot do mod). */
public final class VillagerConfigFiles {

    private VillagerConfigFiles() {}

    public static Path configDir() {
        return FMLPaths.GAMEDIR.get().resolve("config/craftlevel");
    }

    public static Path villagerFile() {
        return configDir().resolve("villager.json");
    }

    public static JsonObject loadVillagerJson() {
        Path file = villagerFile();
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                copyDefaultVillager(file);
            }
            return JsonParser.parseReader(Files.newBufferedReader(file)).getAsJsonObject();
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to load villager.json from config, using empty defaults", e);
            JsonObject empty = new JsonObject();
            empty.add("professions", new JsonObject());
            empty.add("trades", new JsonObject());
            return empty;
        }
    }

    private static void copyDefaultVillager(Path target) throws IOException {
        try (InputStream stream = ConfigManager.class.getClassLoader()
                .getResourceAsStream("assets/craftlevel/defaults/villager.json")) {
            if (stream == null) {
                Files.writeString(target, "{\"professions\":{},\"trades\":{}}");
                return;
            }
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
