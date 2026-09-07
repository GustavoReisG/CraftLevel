package com.grgs.craftlevel.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.config.AltarPotionParser;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Estado editável de altar_of_death.json (potion + improvement). */
final class AltarOfDeathEditorState {

    private int gloryCostTime = 10;
    private int gloryCostStrength = 10;
    private int maxUpgradesTime = 5;
    private int maxUpgradesStrength = 5;
    private int improvementGloryCost = 20;
    private int genericEnchantmentLimit = 50;
    private final LinkedHashMap<String, Integer> enchantmentLimits = new LinkedHashMap<>();
    private final LinkedHashMap<String, AltarPotionParser.EffectLimit> effectLimits = new LinkedHashMap<>();

    static AltarOfDeathEditorState loadFrom(Path file) {
        AltarOfDeathEditorState state = new AltarOfDeathEditorState();
        if (!Files.exists(file)) {
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("potion") && json.get("potion").isJsonObject()) {
                JsonObject potion = json.getAsJsonObject("potion");
                state.gloryCostTime = Math.max(1, readInt(potion, "glory_cost_time", 10));
                state.gloryCostStrength = Math.max(1, readInt(potion, "glory_cost_strength", 10));
                if (potion.has("max_upgrades_time") || potion.has("max_upgrades_strength")) {
                    state.maxUpgradesTime = Math.max(1, readInt(potion, "max_upgrades_time", 5));
                    state.maxUpgradesStrength = Math.max(1, readInt(potion, "max_upgrades_strength", 5));
                } else if (potion.has("max_upgrades")) {
                    int total = Math.max(2, readInt(potion, "max_upgrades", 10));
                    state.maxUpgradesTime = Math.max(1, total / 2);
                    state.maxUpgradesStrength = Math.max(1, total - state.maxUpgradesTime);
                }
                state.effectLimits.clear();
                if (potion.has("effect_limits") && potion.get("effect_limits").isJsonObject()) {
                    readEffectLimits(potion.getAsJsonObject("effect_limits"), state.effectLimits, state.maxUpgradesTime);
                }
            }
            if (json.has("improvement") && json.get("improvement").isJsonObject()) {
                JsonObject improvement = json.getAsJsonObject("improvement");
                state.improvementGloryCost = Math.max(1, readInt(improvement, "glory_cost", 20));
                state.genericEnchantmentLimit = Math.max(1, readInt(improvement, "generic_enchantment_limit", 50));
                state.enchantmentLimits.clear();
                if (improvement.has("enchantment_limits") && improvement.get("enchantment_limits").isJsonObject()) {
                    readIntLimits(improvement.getAsJsonObject("enchantment_limits"), state.enchantmentLimits);
                }
            }
        } catch (Exception ignored) {
        }
        return state;
    }

    JsonObject toJson() {
        JsonObject root = new JsonObject();

        JsonObject potion = new JsonObject();
        potion.addProperty("glory_cost_time", Math.max(1, gloryCostTime));
        potion.addProperty("glory_cost_strength", Math.max(1, gloryCostStrength));
        potion.addProperty("max_upgrades_time", Math.max(1, maxUpgradesTime));
        potion.addProperty("max_upgrades_strength", Math.max(1, maxUpgradesStrength));
        potion.add("effect_limits", writeEffectLimits(effectLimits));
        root.add("potion", potion);

        JsonObject improvement = new JsonObject();
        improvement.addProperty("glory_cost", Math.max(1, improvementGloryCost));
        improvement.addProperty("generic_enchantment_limit", Math.max(1, genericEnchantmentLimit));
        improvement.add("enchantment_limits", writeIntLimits(enchantmentLimits));
        root.add("improvement", improvement);
        return root;
    }

    int gloryCostTime() {
        return gloryCostTime;
    }

    void setGloryCostTime(int value) {
        gloryCostTime = Math.max(1, value);
    }

    int gloryCostStrength() {
        return gloryCostStrength;
    }

    void setGloryCostStrength(int value) {
        gloryCostStrength = Math.max(1, value);
    }

    int maxUpgradesTime() {
        return maxUpgradesTime;
    }

    void setMaxUpgradesTime(int value) {
        maxUpgradesTime = Math.max(1, value);
    }

    int maxUpgradesStrength() {
        return maxUpgradesStrength;
    }

    void setMaxUpgradesStrength(int value) {
        maxUpgradesStrength = Math.max(1, value);
    }

    int improvementGloryCost() {
        return improvementGloryCost;
    }

    void setImprovementGloryCost(int value) {
        improvementGloryCost = Math.max(1, value);
    }

    int genericEnchantmentLimit() {
        return genericEnchantmentLimit;
    }

    void setGenericEnchantmentLimit(int value) {
        genericEnchantmentLimit = Math.max(1, value);
    }

    LinkedHashMap<String, Integer> enchantmentLimits() {
        return enchantmentLimits;
    }

    void setEnchantmentLimits(Map<String, Integer> next) {
        enchantmentLimits.clear();
        for (Map.Entry<String, Integer> entry : next.entrySet()) {
            String id = entry.getKey() == null ? "" : entry.getKey().trim();
            if (id.isEmpty() || entry.getValue() == null) {
                continue;
            }
            enchantmentLimits.put(id, Math.max(1, entry.getValue()));
        }
    }

    LinkedHashMap<String, AltarPotionParser.EffectLimit> effectLimits() {
        return effectLimits;
    }

    void setEffectLimits(Map<String, AltarPotionParser.EffectLimit> next) {
        effectLimits.clear();
        for (Map.Entry<String, AltarPotionParser.EffectLimit> entry : next.entrySet()) {
            String id = entry.getKey() == null ? "" : entry.getKey().trim();
            if (id.isEmpty() || entry.getValue() == null) {
                continue;
            }
            effectLimits.put(id, entry.getValue());
        }
    }

    private static void readEffectLimits(
            JsonObject source,
            LinkedHashMap<String, AltarPotionParser.EffectLimit> target,
            int defaultTime
    ) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String id = entry.getKey() == null ? "" : entry.getKey().trim();
            if (id.isEmpty()) {
                continue;
            }
            AltarPotionParser.EffectLimit limit = AltarPotionParser.readEffectLimit(entry.getValue(), defaultTime);
            if (limit != null) {
                target.put(id, limit);
            }
        }
    }

    private static JsonObject writeEffectLimits(Map<String, AltarPotionParser.EffectLimit> source) {
        JsonObject limits = new JsonObject();
        for (Map.Entry<String, AltarPotionParser.EffectLimit> entry : source.entrySet()) {
            String id = entry.getKey() == null ? "" : entry.getKey().trim();
            if (id.isEmpty() || entry.getValue() == null) {
                continue;
            }
            JsonObject obj = new JsonObject();
            obj.addProperty("time", entry.getValue().time());
            obj.addProperty("level", entry.getValue().level());
            limits.add(id, obj);
        }
        return limits;
    }

    private static void readIntLimits(JsonObject source, LinkedHashMap<String, Integer> target) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String id = entry.getKey() == null ? "" : entry.getKey().trim();
            if (id.isEmpty()) {
                continue;
            }
            try {
                target.put(id, Math.max(1, entry.getValue().getAsInt()));
            } catch (Exception ignored) {
            }
        }
    }

    private static JsonObject writeIntLimits(Map<String, Integer> source) {
        JsonObject limits = new JsonObject();
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String id = entry.getKey() == null ? "" : entry.getKey().trim();
            if (id.isEmpty() || entry.getValue() == null) {
                continue;
            }
            limits.addProperty(id, Math.max(1, entry.getValue()));
        }
        return limits;
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
