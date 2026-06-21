package com.grgs.craftlevel.config;

import com.google.gson.*;
import com.grgs.craftlevel.CraftLevel;

import java.util.HashMap;
import java.util.Map;

public class ConfigParser {

    // =========================================================
    // ESTRUTURAS
    // =========================================================

    public record TierValues(double T1, double T2, double T3, double T4, String operation) {}

    // System
    public static boolean enableSystem = true;
    public static boolean debug = false;
    public static int mobLevelVariation = 10;

    // LP
    public static int lpBase = 50;
    public static TierValues lpMultiplier = new TierValues(1.0, 1.0, 1.0, 1.0, "addition");
    public static Map<String, Double> playerBaseStats = new HashMap<>();

    // Player Stats padrão: attributeId -> TierValues
    public static Map<String, TierValues> playerStats = new HashMap<>();

    // Mob Stats padrão: attributeId -> TierValues
    public static Map<String, TierValues> mobStats = new HashMap<>();

    // Mob Overrides: mobId -> (attributeId -> TierValues)
    public static Map<String, Map<String, TierValues>> mobOverrides = new HashMap<>();

    // Mob Levels: mobId -> int[]{min, max}
    public static Map<String, int[]> mobLevels = new HashMap<>();

    // =========================================================
    // PARSERS
    // =========================================================

    public static void parseSystem(JsonObject json) {
        try {
            enableSystem = json.get("enableSystem").getAsBoolean();
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] system.json: error reading enableSystem, using true");
            enableSystem = true;
        }
        try {
            debug = json.get("debug").getAsBoolean();
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] system.json: error reading debug, using false");
            debug = false;
        }
        try {
            mobLevelVariation = json.get("mob_level_variation").getAsInt();
        } catch (Exception e) {
            mobLevelVariation = 10;
        }
    }

    public static void parseLp(JsonObject json) {
        try {
            lpBase = json.get("lpBase").getAsInt();
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] lp.json: error reading lpBase, using 100");
            lpBase = 100;
        }

        try {
            JsonObject multiplier = json.getAsJsonObject("lpMultiplier");
            lpMultiplier = parseTiers(multiplier, "lp.json > lpMultiplier");
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] lp.json: error reading lpMultiplier, using defaults");
        }

        try {
            JsonObject stats = json.getAsJsonObject("playerBaseStats");

            playerBaseStats.clear();

            for (Map.Entry<String, JsonElement> entry : stats.entrySet()) {
                playerBaseStats.put(
                        entry.getKey(),
                        entry.getValue().getAsDouble()
                );
            }

        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] lp.json: error reading playerBaseStats");
        }
    }

    public static void parsePlayerStats(JsonObject json) {
        playerStats.clear();

        for (String attributeId : json.keySet()) {
            try {
                JsonObject tiers = json.getAsJsonObject(attributeId);
                TierValues values = parseTiers(tiers, "player_stats.json > " + attributeId);
                playerStats.put(attributeId, values);
            } catch (Exception e) {
                CraftLevel.LOGGER.warn("[CraftLevel] player_stats.json: error reading attribute '{}', skipping", attributeId);
            }
        }
    }

    public static void parseMobStats(JsonObject json) {
        mobStats.clear();

        for (String attributeId : json.keySet()) {
            try {
                JsonObject tiers = json.getAsJsonObject(attributeId);
                TierValues values = parseTiers(tiers, "mob_stats.json > " + attributeId);
                mobStats.put(attributeId, values);
            } catch (Exception e) {
                CraftLevel.LOGGER.warn("[CraftLevel] mob_stats.json: error reading attribute '{}', skipping", attributeId);
            }
        }
    }

    public static void parseMobOverrides(JsonObject json) {
        mobOverrides.clear();

        for (String mobId : json.keySet()) {
            try {
                JsonObject mobObj = json.getAsJsonObject(mobId);
                Map<String, TierValues> attrMap = new HashMap<>();

                for (String attributeId : mobObj.keySet()) {
                    try {
                        JsonObject tiers = mobObj.getAsJsonObject(attributeId);
                        TierValues values = parseTiers(tiers, "mob_overrides.json > " + mobId + " > " + attributeId);
                        attrMap.put(attributeId, values);
                    } catch (Exception e) {
                        CraftLevel.LOGGER.warn("[CraftLevel] mob_overrides.json: error reading attribute '{}' for mob '{}', skipping", attributeId, mobId);
                    }
                }

                mobOverrides.put(mobId, attrMap);
            } catch (Exception e) {
                CraftLevel.LOGGER.warn("[CraftLevel] mob_overrides.json: error reading mob '{}', skipping", mobId);
            }
        }
    }

    public static void parseMobLevels(JsonObject json) {
        mobLevels.clear();

        for (String mobId : json.keySet()) {
            try {
                JsonObject entry = json.getAsJsonObject(mobId);

                int min = 1;
                int max = 200;

                try {
                    min = entry.get("lvl_min").getAsInt();
                } catch (Exception e) {
                    CraftLevel.LOGGER.warn("[CraftLevel] mob_levels.json: error reading lvl_min for '{}', using 1", mobId);
                }

                try {
                    max = entry.get("lvl_max").getAsInt();
                } catch (Exception e) {
                    CraftLevel.LOGGER.warn("[CraftLevel] mob_levels.json: error reading lvl_max for '{}', using 200", mobId);
                }

                mobLevels.put(mobId, new int[]{min, max});
            } catch (Exception e) {
                CraftLevel.LOGGER.warn("[CraftLevel] mob_levels.json: error reading mob '{}', skipping", mobId);
            }
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private static TierValues parseTiers(JsonObject tiers, String context) {
        double T1 = getTierValue(tiers, "T1", context);
        double T2 = getTierValue(tiers, "T2", context);
        double T3 = getTierValue(tiers, "T3", context);
        double T4 = getTierValue(tiers, "T4", context);
        String operation = "addition"; // padrão
        try {
            if (tiers.has("operation")) {
                operation = tiers.get("operation").getAsString();
            }
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] {}: error reading operation, using addition", context);
        }
        return new TierValues(T1, T2, T3, T4, operation);
    }

    private static double getTierValue(JsonObject tiers, String key, String context) {
        try {
            return tiers.get(key).getAsDouble();
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] {}: error reading '{}', using 1.0", context, key);
            return 1.0;
        }
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

    public static TierValues getStatForMob(String mobId, String attributeId) {
        if (mobOverrides.containsKey(mobId) && mobOverrides.get(mobId).containsKey(attributeId)) {
            return mobOverrides.get(mobId).get(attributeId);
        }
        return mobStats.getOrDefault(attributeId, new TierValues(1.0, 1.0, 1.0, 1.0, "addition"));
    }

    public static double getScaledMultiplier(TierValues tiers, int level) {
        double result = 1.0;

        int remaining = level;

        int tier1Levels = Math.min(remaining, 50);
        result *= Math.pow(tiers.T1(), tier1Levels);
        remaining -= tier1Levels;

        if (remaining > 0) {
            int tier2Levels = Math.min(remaining, 50);
            result *= Math.pow(tiers.T2(), tier2Levels);
            remaining -= tier2Levels;
        }

        if (remaining > 0) {
            int tier3Levels = Math.min(remaining, 50);
            result *= Math.pow(tiers.T3(), tier3Levels);
            remaining -= tier3Levels;
        }

        if (remaining > 0) {
            int tier4Levels = Math.min(remaining, 50);
            result *= Math.pow(tiers.T4(), tier4Levels);
        }

        return result;
    }

    public static int getMobMinLevel(String mobId) {
        return mobLevels.containsKey(mobId) ? mobLevels.get(mobId)[0] : 1;
    }

    public static int getMobMaxLevel(String mobId) {
        return mobLevels.containsKey(mobId) ? mobLevels.get(mobId)[1] : 200;
    }
}