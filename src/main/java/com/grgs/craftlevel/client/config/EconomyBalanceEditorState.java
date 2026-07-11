package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Estado editável de economy.json na UI. */
final class EconomyBalanceEditorState {

    record ProfessionEntry(int balance, int refreshTicks) {}

    private long playerStartingBalance;
    private int villagerDefaultStarting = 1000;
    private int villagerRefreshTicks = 24000;
    private final LinkedHashMap<String, ProfessionEntry> perProfession = new LinkedHashMap<>();

    static EconomyBalanceEditorState loadFrom(Path file) {
        EconomyBalanceEditorState state = new EconomyBalanceEditorState();
        if (!Files.exists(file)) {
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            state.importFrom(json);
        } catch (Exception ignored) {
            // JSON inválido: defaults
        }
        return state;
    }

    void importFrom(JsonObject json) {
        if (json.has("player") && json.get("player").isJsonObject()) {
            JsonObject player = json.getAsJsonObject("player");
            playerStartingBalance = readLong(player, "starting_balance", 0L);
        }

        if (json.has("villager_balance") && json.get("villager_balance").isJsonObject()) {
            JsonObject villager = json.getAsJsonObject("villager_balance");
            villagerRefreshTicks = readInt(villager, "refresh_ticks", 24000);
            villagerDefaultStarting = readInt(villager, "default_starting", 1000);

            if (villager.has("per_profession") && villager.get("per_profession").isJsonObject()) {
                perProfession.clear();
                villager.getAsJsonObject("per_profession").asMap().forEach((profession, value) -> {
                    if (value.isJsonObject()) {
                        JsonObject entry = value.getAsJsonObject();
                        perProfession.put(profession, new ProfessionEntry(
                                readInt(entry, "balance", villagerDefaultStarting),
                                readInt(entry, "refresh_ticks", villagerRefreshTicks)
                        ));
                    } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                        perProfession.put(profession, new ProfessionEntry(value.getAsInt(), villagerRefreshTicks));
                    }
                });
            }
        }
    }

    JsonObject toJson() {
        JsonObject root = new JsonObject();

        JsonObject player = new JsonObject();
        player.addProperty("starting_balance", playerStartingBalance);
        root.add("player", player);

        JsonObject villager = new JsonObject();
        villager.addProperty("refresh_ticks", villagerRefreshTicks);
        villager.addProperty("default_starting", villagerDefaultStarting);

        JsonObject professions = new JsonObject();
        perProfession.forEach((professionId, entry) -> {
            JsonObject profession = new JsonObject();
            profession.addProperty("balance", entry.balance());
            profession.addProperty("refresh_ticks", entry.refreshTicks());
            professions.add(professionId, profession);
        });
        villager.add("per_profession", professions);
        root.add("villager_balance", villager);

        return root;
    }

    long playerStartingBalance() {
        return playerStartingBalance;
    }

    void setPlayerStartingBalance(long value) {
        playerStartingBalance = Math.max(0L, value);
    }

    int villagerRefreshTicks() {
        return villagerRefreshTicks;
    }

    void setVillagerRefreshTicks(int value) {
        villagerRefreshTicks = Math.max(1, value);
    }

    int villagerDefaultStarting() {
        return villagerDefaultStarting;
    }

    void setVillagerDefaultStarting(int value) {
        villagerDefaultStarting = Math.max(0, value);
    }

    LinkedHashMap<String, ProfessionEntry> perProfession() {
        return perProfession;
    }

    void clearProfessions() {
        perProfession.clear();
    }

    void setProfession(String professionId, int balance, int refreshTicks) {
        if (professionId == null || professionId.isBlank()) {
            return;
        }
        perProfession.put(professionId.trim(), new ProfessionEntry(
                Math.max(0, balance),
                Math.max(1, refreshTicks)
        ));
    }

    void removeProfession(String professionId) {
        perProfession.remove(professionId);
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long readLong(JsonObject json, String key, long fallback) {
        try {
            return json.get(key).getAsLong();
        } catch (Exception e) {
            return fallback;
        }
    }
}
