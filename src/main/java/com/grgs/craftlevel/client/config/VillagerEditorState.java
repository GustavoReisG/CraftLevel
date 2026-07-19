package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.config.VillagerParser;
import com.grgs.craftlevel.villager.VillagerProfessionDef;
import com.grgs.craftlevel.villager.VillagerTradeDef;
import com.grgs.craftlevel.villager.VillagerTradeSlotDef;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Estado editável de villager.json na UI. */
final class VillagerEditorState {

    private final LinkedHashMap<String, VillagerProfessionDef> professions = new LinkedHashMap<>();
    private final LinkedHashMap<String, List<VillagerTradeDef>> trades = new LinkedHashMap<>();

    static VillagerEditorState loadFrom(Path file) {
        VillagerEditorState state = new VillagerEditorState();
        if (!Files.exists(file)) {
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            VillagerParser.VillagerData data = VillagerParser.parse(json);
            state.professions.putAll(data.professions());
            data.trades().forEach((id, list) -> state.trades.put(id, new ArrayList<>(list)));
            for (String professionId : state.professions.keySet()) {
                state.trades.putIfAbsent(professionId, new ArrayList<>());
            }
        } catch (Exception ignored) {
            // JSON inválido: defaults vazios
        }
        return state;
    }

    JsonObject toJson() {
        for (String professionId : professions.keySet()) {
            trades.putIfAbsent(professionId, new ArrayList<>());
        }
        return VillagerParser.toJson(new VillagerParser.VillagerData(professions, trades));
    }

    Map<String, VillagerProfessionDef> professions() {
        return professions;
    }

    Map<String, List<VillagerTradeDef>> trades() {
        return trades;
    }

    List<VillagerTradeDef> tradesFor(String professionId) {
        return trades.computeIfAbsent(professionId, id -> new ArrayList<>());
    }

    void setProfession(String id, VillagerProfessionDef def) {
        professions.put(id, def);
        trades.putIfAbsent(id, new ArrayList<>());
    }

    void removeProfession(String id) {
        professions.remove(id);
        trades.remove(id);
    }

    void addTrade(String professionId, VillagerTradeDef trade) {
        tradesFor(professionId).add(trade);
    }

    void setTrade(String professionId, int index, VillagerTradeDef trade) {
        List<VillagerTradeDef> list = tradesFor(professionId);
        while (list.size() <= index) {
            list.add(emptyTrade());
        }
        list.set(index, trade);
    }

    void removeTrade(String professionId, int index) {
        List<VillagerTradeDef> list = tradesFor(professionId);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
        }
    }

    static VillagerTradeDef emptyTrade() {
        VillagerTradeSlotDef empty = new VillagerTradeSlotDef("", 0, 0, List.of(), List.of(), "");
        return new VillagerTradeDef(0, empty, empty, empty);
    }
}
