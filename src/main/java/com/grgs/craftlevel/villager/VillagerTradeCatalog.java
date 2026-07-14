package com.grgs.craftlevel.villager;

import com.google.gson.JsonObject;
import com.grgs.craftlevel.config.VillagerParser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Catálogo de trades e profissões — trades parseados sob demanda por profissão. */
public final class VillagerTradeCatalog {

    private static JsonObject rawJson = new JsonObject();
    private static Map<String, VillagerProfessionDef> professions = Collections.emptyMap();
    private static final Map<String, List<VillagerTradeDef>> TRADE_CACHE = new ConcurrentHashMap<>();

    private VillagerTradeCatalog() {}

    public static void load(JsonObject json) {
        rawJson = json != null ? json : new JsonObject();
        professions = Collections.unmodifiableMap(VillagerParser.parseProfessions(rawJson));
        TRADE_CACHE.clear();
        TagExpansionCache.clear();
    }

    public static void load(VillagerParser.VillagerData parsed) {
        rawJson = VillagerParser.toJson(parsed);
        professions = parsed.professions();
        TRADE_CACHE.clear();
        parsed.trades().forEach(TRADE_CACHE::put);
        TagExpansionCache.clear();
    }

    public static Map<String, VillagerProfessionDef> professions() {
        return professions;
    }

    public static List<VillagerTradeDef> tradesFor(String professionId) {
        return TRADE_CACHE.computeIfAbsent(professionId, VillagerTradeCatalog::parseProfessionTrades);
    }

    public static VillagerProfessionDef profession(String professionId) {
        return professions.get(professionId);
    }

    public static VillagerParser.VillagerData data() {
        Map<String, List<VillagerTradeDef>> allTrades = new ConcurrentHashMap<>();
        if (rawJson.has("trades") && rawJson.get("trades").isJsonObject()) {
            rawJson.getAsJsonObject("trades").asMap().keySet()
                    .forEach(id -> allTrades.put(id, tradesFor(id)));
        }
        for (String professionId : professions.keySet()) {
            allTrades.putIfAbsent(professionId, List.of());
        }
        return new VillagerParser.VillagerData(professions, Collections.unmodifiableMap(allTrades));
    }

    private static List<VillagerTradeDef> parseProfessionTrades(String professionId) {
        return Collections.unmodifiableList(VillagerParser.parseTradesForProfession(rawJson, professionId));
    }
}
