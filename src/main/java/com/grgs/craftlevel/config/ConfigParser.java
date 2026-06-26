package com.grgs.craftlevel.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Converte JSONs de configuração em estruturas usadas pelo mod em runtime. */
public class ConfigParser {

    public static final String DEFAULT_KEY = "default";
    public static final String MAX_HEALTH_ID = "minecraft:generic.max_health";

    private static final double DEFAULT_SCALING = 1.0;

    /** Valor direto (double) ou referência a scaling da seção attribute (string). */
    public record ScalingRef(Double direct, String attributeId) {
        public static ScalingRef direct(double value) {
            return new ScalingRef(value, null);
        }

        public static ScalingRef attribute(String attributeId) {
            return new ScalingRef(null, attributeId);
        }

        public double resolveBaseScaling(Map<String, Double> attributes) {
            if (direct != null) {
                return direct;
            }
            if (attributeId != null) {
                return attributes.getOrDefault(attributeId, DEFAULT_SCALING);
            }
            return DEFAULT_SCALING;
        }
    }

    public record StatScalingConfig(
            Map<String, Double> attributes,
            Map<String, ScalingRef> hurt,
            Map<String, ScalingRef> heal
    ) {
        public static StatScalingConfig empty() {
            return new StatScalingConfig(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }

        public Set<String> attributeIds() {
            return attributes.keySet();
        }

        public ScalingRef lookupHurt(String damageTypeId) {
            ScalingRef ref = resolveHurtRef(hurt, damageTypeId, true);
            return ref != null ? ref : ScalingRef.direct(DEFAULT_SCALING);
        }

        public ScalingRef lookupHeal(String healKey) {
            if (healKey != null && heal.containsKey(healKey)) {
                return heal.get(healKey);
            }
            return heal.getOrDefault(DEFAULT_KEY, ScalingRef.direct(DEFAULT_SCALING));
        }
    }

    public static int mobLevelVariation = 10;
    public static boolean debug = false;
    public static int lpBase = 100;
    public static double lpMultiplier = 1.06;

    public static StatScalingConfig playerStats = StatScalingConfig.empty();
    public static StatScalingConfig mobStats = StatScalingConfig.empty();
    public static Map<String, StatScalingConfig> mobOverrides = new HashMap<>();
    public static Map<String, int[]> mobLevels = new HashMap<>();

    public static void parseSystem(JsonObject json) {
        mobLevelVariation = readInt(json, "mob_level_variation", 10);
        debug = readBoolean(json, "debug", false);
    }

    public static void parseLp(JsonObject json) {
        lpBase = readInt(json, "lpBase", 100);
        lpMultiplier = readScaling(json.get("lpMultiplier"));
    }

    public static void parsePlayerStats(JsonObject json) {
        playerStats = parseStats(json);
    }

    public static void parseMobStats(JsonObject json) {
        mobStats = parseStats(json);
    }

    public static void parseMobOverrides(JsonObject json) {
        mobOverrides.clear();
        json.asMap().forEach((mobId, value) -> {
            if (!value.isJsonObject()) return;
            mobOverrides.put(mobId, parseStats(value.getAsJsonObject()));
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

    public static double getPlayerAttributeScaling(String attributeId) {
        return playerStats.attributes().getOrDefault(attributeId, DEFAULT_SCALING);
    }

    public static double getMobAttributeScaling(String mobId, String attributeId) {
        StatScalingConfig override = mobOverrides.get(mobId);
        if (override != null && override.attributes().containsKey(attributeId)) {
            return override.attributes().get(attributeId);
        }
        return mobStats.attributes().getOrDefault(attributeId, DEFAULT_SCALING);
    }

    public static Set<String> getPlayerAttributeIds() {
        return playerStats.attributeIds();
    }

    public static Set<String> getMobAttributeIds(String mobId) {
        StatScalingConfig override = mobOverrides.get(mobId);
        if (override != null && !override.attributes().isEmpty()) {
            return override.attributeIds();
        }
        return mobStats.attributeIds();
    }

    public static double getPlayerHurtMultiplier(String damageTypeId, int level) {
        ScalingRef ref = playerStats.lookupHurt(damageTypeId);
        double scaling = ref.resolveBaseScaling(playerStats.attributes());
        return getScaledMultiplier(scaling, level);
    }

    public static double getPlayerHealMultiplier(String healKey, int level) {
        return resolveHealMultiplier(playerStats, healKey, level);
    }

    public static double getMobHurtMultiplier(String mobId, String damageTypeId, int level) {
        ScalingRef ref = resolveMobHurtRef(mobId, damageTypeId);
        double scaling = ref.resolveBaseScaling(getMobAttributes(mobId));
        return getScaledMultiplier(scaling, level);
    }

    public static double getMobHealMultiplier(String mobId, String healKey, int level) {
        ScalingRef ref = resolveMobHealRef(mobId, healKey);
        double scaling = ref.resolveBaseScaling(getMobAttributes(mobId));
        return getScaledMultiplier(scaling, level);
    }

    public static Map<String, Double> getMobAttributes(String mobId) {
        Map<String, Double> merged = new HashMap<>(mobStats.attributes());
        StatScalingConfig override = mobOverrides.get(mobId);
        if (override != null) {
            merged.putAll(override.attributes());
        }
        return merged;
    }

    public static double getScaledMultiplier(double scaling, int level) {
        return Math.pow(scaling, level);
    }

    public static int getMobMinLevel(String mobId) {
        return mobLevels.containsKey(mobId) ? mobLevels.get(mobId)[0] : 1;
    }

    public static int getMobMaxLevel(String mobId) {
        return mobLevels.containsKey(mobId) ? mobLevels.get(mobId)[1] : 200;
    }

    private static ScalingRef resolveMobHurtRef(String mobId, String damageTypeId) {
        StatScalingConfig override = mobOverrides.get(mobId);
        if (override != null) {
            ScalingRef ref = lookupHurtRef(override, damageTypeId);
            if (ref != null) {
                return ref;
            }
        }
        return mobStats.lookupHurt(damageTypeId);
    }

    private static ScalingRef resolveMobHealRef(String mobId, String healKey) {
        StatScalingConfig override = mobOverrides.get(mobId);
        if (override != null) {
            ScalingRef ref = lookupHealRef(override, healKey);
            if (ref != null) {
                return ref;
            }
        }
        return mobStats.lookupHeal(healKey);
    }

    private static double resolveHealMultiplier(StatScalingConfig config, String healKey, int level) {
        ScalingRef ref = config.lookupHeal(healKey);
        double scaling = ref.resolveBaseScaling(config.attributes());
        return getScaledMultiplier(scaling, level);
    }

    private static ScalingRef lookupHurtRef(StatScalingConfig config, String damageTypeId) {
        return resolveHurtRef(config.hurt(), damageTypeId, true);
    }

    /**
     * Resolve hurt: match exato → chave com * (keyword contida no damage type) → default.
     * Várias chaves wildcard: vence a keyword mais longa.
     */
    private static ScalingRef resolveHurtRef(
            Map<String, ScalingRef> hurt,
            String damageTypeId,
            boolean useDefault
    ) {
        if (damageTypeId != null && hurt.containsKey(damageTypeId)) {
            return hurt.get(damageTypeId);
        }

        ScalingRef wildcardMatch = findWildcardHurtMatch(hurt, damageTypeId);
        if (wildcardMatch != null) {
            return wildcardMatch;
        }

        if (useDefault && hurt.containsKey(DEFAULT_KEY)) {
            return hurt.get(DEFAULT_KEY);
        }
        return null;
    }

    private static ScalingRef findWildcardHurtMatch(Map<String, ScalingRef> hurt, String damageTypeId) {
        if (damageTypeId == null) {
            return null;
        }

        ScalingRef bestMatch = null;
        int bestKeywordLength = -1;

        for (Map.Entry<String, ScalingRef> entry : hurt.entrySet()) {
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

    private static ScalingRef lookupHealRef(StatScalingConfig config, String healKey) {
        if (healKey != null && config.heal().containsKey(healKey)) {
            return config.heal().get(healKey);
        }
        if (config.heal().containsKey(DEFAULT_KEY)) {
            return config.heal().get(DEFAULT_KEY);
        }
        return null;
    }

    private static StatScalingConfig parseStats(JsonObject json) {
        Map<String, Double> attributes = new HashMap<>();
        Map<String, ScalingRef> hurt = new HashMap<>();
        Map<String, ScalingRef> heal = new HashMap<>();

        if (json.has("attribute") && json.get("attribute").isJsonObject()) {
            readScalingMap(json.getAsJsonObject("attribute"), attributes);
        }

        if (json.has("hurt") && json.get("hurt").isJsonObject()) {
            readEventScalingMap(json.getAsJsonObject("hurt"), hurt);
        }

        if (json.has("heal") && json.get("heal").isJsonObject()) {
            readEventScalingMap(json.getAsJsonObject("heal"), heal);
        }

        // Compatibilidade com formato antigo: event.onLivingHurt
        if (hurt.isEmpty()
                && json.has("event")
                && json.get("event").isJsonObject()
                && json.getAsJsonObject("event").has("onLivingHurt")
                && json.getAsJsonObject("event").get("onLivingHurt").isJsonObject()) {
            readLegacyHurtMap(json.getAsJsonObject("event").getAsJsonObject("onLivingHurt"), hurt);
        }

        return new StatScalingConfig(attributes, hurt, heal);
    }

    private static void readScalingMap(JsonObject section, Map<String, Double> target) {
        section.asMap().forEach((key, value) -> target.put(key, readScaling(value)));
    }

    private static void readEventScalingMap(JsonObject section, Map<String, ScalingRef> target) {
        section.asMap().forEach((key, value) -> target.put(key, readScalingRef(value)));
    }

    private static void readLegacyHurtMap(JsonObject section, Map<String, ScalingRef> target) {
        section.asMap().forEach((key, value) -> {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                target.put(ConfigParser.DEFAULT_KEY, ScalingRef.attribute(key));
            }
        });
    }

    private static ScalingRef readScalingRef(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                return ScalingRef.direct(element.getAsDouble());
            }
            if (element.getAsJsonPrimitive().isString()) {
                return ScalingRef.attribute(element.getAsString());
            }
        }
        return ScalingRef.direct(DEFAULT_SCALING);
    }

    private static double readScaling(JsonElement element) {
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsDouble();
        }
        return DEFAULT_SCALING;
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
}
