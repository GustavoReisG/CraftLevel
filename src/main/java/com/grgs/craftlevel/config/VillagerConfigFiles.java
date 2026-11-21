package com.grgs.craftlevel.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.CraftLevel;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        JsonObject defaults = ConfigDefaults.readJson("villager.json");
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

    private static void copyDefaultVillager(Path target) throws IOException {
        if (!ConfigDefaults.copyIfMissing("villager.json", target) && !Files.exists(target)) {
            Files.writeString(target, "{\"professions\":{},\"trades\":{}}");
        }
    }
}
