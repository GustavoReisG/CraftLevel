package com.grgs.craftlevel.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.CraftLevel;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Leitura de villager.json da pasta config (disponível no boot do mod). */
public final class VillagerConfigFiles {

    private static final String DEATH_MERCHANT = "craftlevel:death_merchant";

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
            JsonObject json = JsonParser.parseReader(Files.newBufferedReader(file)).getAsJsonObject();
            if (mergeDeathMerchantIfMissing(json)) {
                Files.writeString(file, ConfigManager.toPrettyJson(json));
                CraftLevel.LOGGER.info("[CraftLevel] Merged {} into villager.json", DEATH_MERCHANT);
            }
            return json;
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to load villager.json from config, using empty defaults", e);
            JsonObject empty = new JsonObject();
            empty.add("professions", new JsonObject());
            empty.add("trades", new JsonObject());
            return empty;
        }
    }

    /** Garante Death Merchant em installs antigas que já tinham villager.json. */
    private static boolean mergeDeathMerchantIfMissing(JsonObject json) {
        JsonObject defaults = readDefaultVillager();
        if (defaults == null) {
            return false;
        }
        if (!defaults.has("professions") || !defaults.get("professions").isJsonObject()) {
            return false;
        }
        JsonObject defaultProfessions = defaults.getAsJsonObject("professions");
        if (!defaultProfessions.has(DEATH_MERCHANT)) {
            return false;
        }

        if (!json.has("professions") || !json.get("professions").isJsonObject()) {
            json.add("professions", new JsonObject());
        }
        if (!json.has("trades") || !json.get("trades").isJsonObject()) {
            json.add("trades", new JsonObject());
        }

        JsonObject professions = json.getAsJsonObject("professions");
        JsonObject trades = json.getAsJsonObject("trades");
        boolean changed = false;
        if (!professions.has(DEATH_MERCHANT)) {
            professions.add(DEATH_MERCHANT, defaultProfessions.get(DEATH_MERCHANT).deepCopy());
            changed = true;
        }
        if (!trades.has(DEATH_MERCHANT)
                && defaults.has("trades")
                && defaults.get("trades").isJsonObject()
                && defaults.getAsJsonObject("trades").has(DEATH_MERCHANT)) {
            trades.add(DEATH_MERCHANT, defaults.getAsJsonObject("trades").get(DEATH_MERCHANT).deepCopy());
            changed = true;
        }
        return changed;
    }

    private static JsonObject readDefaultVillager() {
        try (InputStream stream = ConfigManager.class.getClassLoader()
                .getResourceAsStream("assets/craftlevel/defaults/villager.json")) {
            if (stream == null) {
                return null;
            }
            JsonElement element = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to read default villager.json", e);
            return null;
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
