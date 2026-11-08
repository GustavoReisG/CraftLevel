package com.grgs.craftlevel.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.config.AltarPotionParser;
import com.grgs.craftlevel.config.AltarTradeParser;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Estado editável de altar_of_death.json (potion + improvement + trade). */
final class AltarOfDeathEditorState {

    private int gloryCostTime = 10;
    private int gloryCostStrength = 10;
    private int maxDurationSeconds = AltarPotionParser.DEFAULT_MAX_DURATION_SECONDS;
    private int maxLevel = AltarPotionParser.DEFAULT_MAX_LEVEL;
    private int improvementGloryCost = 20;
    private int genericEnchantmentLimit = 50;
    private final LinkedHashMap<String, Integer> enchantmentLimits = new LinkedHashMap<>();
    private final LinkedHashMap<String, AltarPotionParser.EffectLimit> effectLimits = new LinkedHashMap<>();
    private final List<AltarTradeParser.TradeEntry> tradeOffers = new ArrayList<>(AltarTradeParser.defaultEntries());

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
                state.maxDurationSeconds = AltarPotionParser.readMaxDurationSeconds(potion);
                state.maxLevel = AltarPotionParser.readMaxLevel(potion);
                state.effectLimits.clear();
                if (potion.has("effect_limits") && potion.get("effect_limits").isJsonObject()) {
                    readEffectLimits(potion.getAsJsonObject("effect_limits"), state.effectLimits, state.maxDurationSeconds);
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
            state.tradeOffers.clear();
            if (json.has("trade") && json.get("trade").isJsonObject()) {
                JsonObject trade = json.getAsJsonObject("trade");
                if (trade.has("offers") && trade.get("offers").isJsonArray()) {
                    for (JsonElement element : trade.getAsJsonArray("offers")) {
                        if (!element.isJsonObject()) {
                            continue;
                        }
                        JsonObject obj = element.getAsJsonObject();
                        String itemId = readString(obj, "item", "");
                        if (itemId.isEmpty()) {
                            continue;
                        }
                        int glory = Math.max(0, readInt(obj, "glory_cost", 1));
                        int count = Math.max(1, readInt(obj, "count", 1));
                        state.tradeOffers.add(new AltarTradeParser.TradeEntry(itemId, glory, count));
                    }
                }
            }
            if (state.tradeOffers.isEmpty()) {
                state.tradeOffers.addAll(AltarTradeParser.defaultEntries());
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
        potion.addProperty("max_duration_seconds", Math.max(1, maxDurationSeconds));
        potion.addProperty("max_level", Math.max(1, maxLevel));
        potion.add("effect_limits", writeEffectLimits(effectLimits));
        root.add("potion", potion);

        JsonObject improvement = new JsonObject();
        improvement.addProperty("glory_cost", Math.max(1, improvementGloryCost));
        improvement.addProperty("generic_enchantment_limit", Math.max(1, genericEnchantmentLimit));
        improvement.add("enchantment_limits", writeIntLimits(enchantmentLimits));
        root.add("improvement", improvement);

        JsonObject trade = new JsonObject();
        trade.add("offers", AltarTradeParser.writeOffers(tradeOffers));
        root.add("trade", trade);
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

    int maxDurationSeconds() {
        return maxDurationSeconds;
    }

    void setMaxDurationSeconds(int value) {
        maxDurationSeconds = Math.max(1, value);
    }

    int maxLevel() {
        return maxLevel;
    }

    void setMaxLevel(int value) {
        maxLevel = Math.max(1, value);
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

    List<AltarTradeParser.TradeEntry> tradeOffers() {
        return tradeOffers;
    }

    void setTradeOffers(List<AltarTradeParser.TradeEntry> next) {
        tradeOffers.clear();
        if (next == null) {
            return;
        }
        for (AltarTradeParser.TradeEntry entry : next) {
            if (entry == null || entry.itemId().isEmpty()) {
                continue;
            }
            tradeOffers.add(entry);
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

    private static String readString(JsonObject json, String key, String fallback) {
        try {
            return json.get(key).getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }
}
