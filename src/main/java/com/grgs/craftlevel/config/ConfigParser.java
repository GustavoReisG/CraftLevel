package com.grgs.craftlevel.config;

import com.google.gson.JsonObject;
import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.scaling.ApplyMode;
import com.grgs.craftlevel.scaling.ScalingDefinition;
import com.grgs.craftlevel.scaling.ScalingMode;
import com.grgs.craftlevel.scaling.ScalingRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Parse dos JSONs em estruturas runtime.
 * <p>
 * {@code config.json} → debug, lp_scaling, escaladores<br>
 * {@code player_stats / mob_stats} → wiring (attribute, hurt, heal → nome do escalador)<br>
 * {@code mob_overrides} → wiring isolado por mob<br>
 * {@code mob_levels} → lvl_min / lvl_max por entity type
 */
public class ConfigParser {

    public static final String DEFAULT_KEY = "default";

    public record StatWiringConfig(
            /** attribute id → nome do escalador */
            Map<String, String> attribute,
            /** damage type id (ou wildcard) → nome do escalador */
            Map<String, String> hurt,
            /** heal key → nome do escalador */
            Map<String, String> heal
    ) {
        public static StatWiringConfig empty() {
            return new StatWiringConfig(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }

        public String lookupHurt(String damageTypeId) {
            return resolveHurtRef(hurt, damageTypeId, true);
        }

        public String lookupHeal(String healKey) {
            if (healKey != null && heal.containsKey(healKey)) {
                return heal.get(healKey);
            }
            return heal.get(DEFAULT_KEY);
        }
    }

    public static boolean debug = false;
    public static int mobLevelVariation = 10;
    public static int maxLevel = 200;

    public static StatWiringConfig playerStats = StatWiringConfig.empty();
    public static StatWiringConfig mobStats = StatWiringConfig.empty();
    public static Map<String, StatWiringConfig> mobOverrides = new HashMap<>();
    public static Map<String, int[]> mobLevels = new HashMap<>();

    public static void parseConfig(JsonObject json) {
        debug = readBoolean(json, "debug", false);

        JsonObject lpScaling = json.has("lp_scaling") && json.get("lp_scaling").isJsonObject()
                ? json.getAsJsonObject("lp_scaling")
                : new JsonObject();

        mobLevelVariation = readInt(lpScaling, "mob_level_variation", 10);
        maxLevel = readInt(lpScaling, "lvl_max", 200);
        ScalingDefinition lpDef = readScalingDefinition(lpScaling, 100, 1.06, ScalingMode.POWER, ApplyMode.OVERWRITE);

        Map<String, ScalingDefinition> scalers = new HashMap<>();
        if (json.has("scaling") && json.get("scaling").isJsonObject()) {
            json.getAsJsonObject("scaling").asMap().forEach((name, value) -> {
                if (!value.isJsonObject()) {
                    CraftLevel.LOGGER.warn("[CraftLevel] Scaler '{}' must be an object, skipping", name);
                    return;
                }
                scalers.put(name, readScalingDefinition(value.getAsJsonObject(), 1.0, 1.0, ScalingMode.POWER, ApplyMode.MULTIPLY));
            });
        }

        ScalingRegistry.load(scalers, lpDef);

        CraftLevel.LOGGER.info(
                "[CraftLevel] Config: debug={} maxLevel={} mobVariation={} scalers={}",
                debug, maxLevel, mobLevelVariation, scalers.size()
        );
    }

    public static void parsePlayerStats(JsonObject json) {
        playerStats = parseWiring(json);
    }

    public static void parseMobStats(JsonObject json) {
        mobStats = parseWiring(json);
    }

    public static void parseMobOverrides(JsonObject json) {
        mobOverrides.clear();
        json.asMap().forEach((mobId, value) -> {
            if (!value.isJsonObject()) return;
            mobOverrides.put(mobId, parseWiring(value.getAsJsonObject()));
        });
    }

    public static void parseMobLevels(JsonObject json) {
        mobLevels.clear();
        json.asMap().forEach((mobId, value) -> {
            if (!value.isJsonObject()) return;
            JsonObject entry = value.getAsJsonObject();
            mobLevels.put(mobId, new int[]{
                    readInt(entry, "lvl_min", 1),
                    readInt(entry, "lvl_max", 200)
            });
        });
    }

    public static StatWiringConfig getPlayerWiring() {
        return playerStats;
    }

    public static StatWiringConfig resolveMobWiring(String mobId) {
        StatWiringConfig override = mobOverrides.get(mobId);
        return override != null ? override : mobStats;
    }

    public static String resolvePlayerHurtScaler(String damageTypeId) {
        return playerStats.lookupHurt(damageTypeId);
    }

    public static String resolveMobHurtScaler(String mobId, String damageTypeId) {
        return resolveMobWiring(mobId).lookupHurt(damageTypeId);
    }

    public static String resolvePlayerHealScaler(String healKey) {
        return playerStats.lookupHeal(healKey);
    }

    public static String resolveMobHealScaler(String mobId, String healKey) {
        return resolveMobWiring(mobId).lookupHeal(healKey);
    }

    public static int getMaxLevel() {
        return maxLevel;
    }

    public static int getMobMinLevel(String mobId) {
        return mobLevels.containsKey(mobId) ? mobLevels.get(mobId)[0] : 1;
    }

    public static int getMobMaxLevel(String mobId) {
        return mobLevels.containsKey(mobId) ? mobLevels.get(mobId)[1] : 200;
    }

    private static StatWiringConfig parseWiring(JsonObject json) {
        Map<String, String> attribute = new HashMap<>();
        Map<String, String> hurt = new HashMap<>();
        Map<String, String> heal = new HashMap<>();

        if (json.has("attribute") && json.get("attribute").isJsonObject()) {
            readStringMap(json.getAsJsonObject("attribute"), attribute);
        }
        if (json.has("hurt") && json.get("hurt").isJsonObject()) {
            readStringMap(json.getAsJsonObject("hurt"), hurt);
        }
        if (json.has("heal") && json.get("heal").isJsonObject()) {
            readStringMap(json.getAsJsonObject("heal"), heal);
        }

        return new StatWiringConfig(attribute, hurt, heal);
    }

    private static ScalingDefinition readScalingDefinition(
            JsonObject json,
            double defaultBase,
            double defaultScaling,
            ScalingMode defaultMode,
            ApplyMode defaultApply
    ) {
        double baseValue = readDouble(json, "base_value", defaultBase);
        double scaling = readDouble(json, "scaling", defaultScaling);
        ScalingMode scalingMode = ScalingMode.fromString(readString(json, "scaling_mode"), defaultMode);
        ApplyMode applyMode = ApplyMode.fromString(readString(json, "apply_mode"), defaultApply);
        return new ScalingDefinition(baseValue, scaling, scalingMode, applyMode);
    }

    private static String resolveHurtRef(Map<String, String> hurt, String damageTypeId, boolean useDefault) {
        if (damageTypeId != null && hurt.containsKey(damageTypeId)) {
            return hurt.get(damageTypeId);
        }

        String wildcardMatch = findWildcardHurtMatch(hurt, damageTypeId);
        if (wildcardMatch != null) {
            return wildcardMatch;
        }

        if (useDefault && hurt.containsKey(DEFAULT_KEY)) {
            return hurt.get(DEFAULT_KEY);
        }
        return null;
    }

    private static String findWildcardHurtMatch(Map<String, String> hurt, String damageTypeId) {
        if (damageTypeId == null) {
            return null;
        }

        String bestMatch = null;
        int bestKeywordLength = -1;

        for (Map.Entry<String, String> entry : hurt.entrySet()) {
            String key = entry.getKey();
            if (DEFAULT_KEY.equals(key) || !key.contains("*")) {
                continue;
            }

            String keyword = key.replace("*", "");
            if (keyword.isEmpty() || !damageTypeId.contains(keyword)) {
                continue;
            }

            if (keyword.length() > bestKeywordLength) {
                bestKeywordLength = keyword.length();
                bestMatch = entry.getValue();
            }
        }

        return bestMatch;
    }

    private static void readStringMap(JsonObject section, Map<String, String> target) {
        section.asMap().forEach((key, value) -> {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                target.put(key, value.getAsString());
            }
        });
    }

    private static double readDouble(JsonObject json, String key, double fallback) {
        try {
            return json.get(key).getAsDouble();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean readBoolean(JsonObject json, String key, boolean fallback) {
        try {
            return json.get(key).getAsBoolean();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String readString(JsonObject json, String key) {
        try {
            return json.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
