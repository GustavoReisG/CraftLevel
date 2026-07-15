package com.grgs.craftlevel.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.grgs.craftlevel.villager.ProfessionIds;
import com.grgs.craftlevel.villager.VillagerProfessionDef;
import com.grgs.craftlevel.villager.VillagerTradeDef;
import com.grgs.craftlevel.villager.VillagerTradeSlotDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parse de villager.json para modelos imutáveis. */
public final class VillagerParser {

    public record VillagerData(
            Map<String, VillagerProfessionDef> professions,
            Map<String, List<VillagerTradeDef>> trades
    ) {}

    private VillagerParser() {}

    public static VillagerData parse(JsonObject json) {
        if (json == null) {
            return new VillagerData(Collections.emptyMap(), Collections.emptyMap());
        }

        Map<String, VillagerProfessionDef> professions = parseProfessions(json);
        Map<String, List<VillagerTradeDef>> trades = parseAllTrades(json);

        for (String professionId : professions.keySet()) {
            trades.putIfAbsent(professionId, List.of());
        }

        return new VillagerData(Collections.unmodifiableMap(professions), Collections.unmodifiableMap(trades));
    }

    public static Map<String, VillagerProfessionDef> parseProfessions(JsonObject json) {
        if (!json.has("professions") || !json.get("professions").isJsonObject()) {
            return Collections.emptyMap();
        }

        Map<String, VillagerProfessionDef> result = new LinkedHashMap<>();
        json.getAsJsonObject("professions").asMap().forEach((id, element) -> {
            if (!element.isJsonObject()) {
                return;
            }
            JsonObject obj = element.getAsJsonObject();
            String fullId = ProfessionIds.fullIdFromInput(id);
            String station = readString(obj, "station", "");
            result.put(fullId, new VillagerProfessionDef(station));
        });
        return result;
    }

    public static List<VillagerTradeDef> parseTradesForProfession(JsonObject json, String professionId) {
        if (!json.has("trades") || !json.get("trades").isJsonObject()) {
            return List.of();
        }
        JsonElement element = json.getAsJsonObject("trades").get(professionId);
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        return parseTradeList(element);
    }

    private static Map<String, List<VillagerTradeDef>> parseAllTrades(JsonObject json) {
        if (!json.has("trades") || !json.get("trades").isJsonObject()) {
            return Collections.emptyMap();
        }

        Map<String, List<VillagerTradeDef>> result = new LinkedHashMap<>();
        json.getAsJsonObject("trades").asMap().forEach((professionId, element) -> {
            List<VillagerTradeDef> trades = parseTradeList(element);
            result.put(professionId, Collections.unmodifiableList(trades));
        });
        return result;
    }

    private static List<VillagerTradeDef> parseTradeList(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (element.isJsonArray()) {
            return parseTradeArray(element.getAsJsonArray());
        }
        return List.of();
    }

    private static List<VillagerTradeDef> parseTradeArray(JsonArray array) {
        List<VillagerTradeDef> trades = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            VillagerTradeDef trade = parseTrade(element.getAsJsonObject());
            if (trade != null) {
                trades.add(trade);
            }
        }
        return VillagerJsonMigrator.collapseParsedTrades(trades);
    }

    private static VillagerTradeDef parseTrade(JsonObject obj) {
        int limit = readInt(obj, "limit", 0);
        VillagerTradeSlotDef input1 = parseSlot(obj, "input1");
        VillagerTradeSlotDef input2 = parseSlot(obj, "input2");
        VillagerTradeSlotDef output = parseSlot(obj, "output");
        return new VillagerTradeDef(limit, input1, input2, output);
    }

    private static VillagerTradeSlotDef parseSlot(JsonObject trade, String key) {
        if (!trade.has(key) || !trade.get(key).isJsonObject()) {
            return new VillagerTradeSlotDef("", 0, 0, List.of(), List.of());
        }
        JsonObject slot = trade.getAsJsonObject(key);
        return new VillagerTradeSlotDef(
                readString(slot, "item", ""),
                readInt(slot, "min", 0),
                readInt(slot, "max", 0),
                readStringList(slot, "whitelist"),
                readStringList(slot, "blacklist")
        );
    }

    private static List<String> readStringList(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString());
            }
        }
        return result;
    }

    private static String readString(JsonObject json, String key, String fallback) {
        try {
            return json.get(key).getAsString();
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

    public static JsonObject toJson(VillagerData data) {
        JsonObject root = new JsonObject();

        JsonObject professions = new JsonObject();
        data.professions().forEach((id, def) -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("station", def.station());
            professions.add(id, obj);
        });
        root.add("professions", professions);

        JsonObject trades = new JsonObject();
        data.trades().forEach((professionId, tradeList) -> {
            JsonArray array = new JsonArray();
            for (VillagerTradeDef trade : tradeList) {
                array.add(tradeToJson(trade));
            }
            trades.add(professionId, array);
        });
        root.add("trades", trades);

        return root;
    }

    private static JsonObject tradeToJson(VillagerTradeDef trade) {
        JsonObject obj = new JsonObject();
        obj.addProperty("limit", trade.limit());
        obj.add("input1", slotToJson(trade.input1()));
        obj.add("input2", slotToJson(trade.input2()));
        obj.add("output", slotToJson(trade.output()));
        return obj;
    }

    private static JsonObject slotToJson(VillagerTradeSlotDef slot) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", slot.item() != null ? slot.item() : "");
        obj.addProperty("min", slot.min());
        obj.addProperty("max", slot.max());
        obj.add("whitelist", stringListToJson(slot.whitelist()));
        obj.add("blacklist", stringListToJson(slot.blacklist()));
        return obj;
    }

    private static JsonArray stringListToJson(List<String> values) {
        JsonArray array = new JsonArray();
        if (values != null) {
            values.forEach(array::add);
        }
        return array;
    }
}
