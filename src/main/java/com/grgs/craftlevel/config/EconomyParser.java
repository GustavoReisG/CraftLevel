package com.grgs.craftlevel.config;

import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Valores carregados de economy.json. */
public final class EconomyParser {

    public record ProfessionOverride(int balance, Integer refreshTicks) {}

    public static long playerStartingBalance = 0L;
    public static int villagerRefreshTicks = 24000;
    public static int villagerDefaultStarting = 1000;
    public static Map<String, ProfessionOverride> villagerPerProfession = Collections.emptyMap();

    private EconomyParser() {}

    public static void parseEconomy(JsonObject json) {
        playerStartingBalance = 0L;
        villagerRefreshTicks = 24000;
        villagerDefaultStarting = 1000;
        villagerPerProfession = new HashMap<>();

        if (json == null) {
            return;
        }

        if (json.has("player") && json.get("player").isJsonObject()) {
            JsonObject player = json.getAsJsonObject("player");
            playerStartingBalance = readLong(player, "starting_balance", 0L);
        }

        if (json.has("villager_balance") && json.get("villager_balance").isJsonObject()) {
            JsonObject villager = json.getAsJsonObject("villager_balance");
            villagerRefreshTicks = readInt(villager, "refresh_ticks", 24000);
            villagerDefaultStarting = readInt(villager, "default_starting", 1000);

            if (villager.has("per_profession") && villager.get("per_profession").isJsonObject()) {
                villager.getAsJsonObject("per_profession").asMap().forEach((profession, value) -> {
                    if (value.isJsonObject()) {
                        JsonObject entry = value.getAsJsonObject();
                        villagerPerProfession.put(profession, new ProfessionOverride(
                                readInt(entry, "balance", villagerDefaultStarting),
                                readOptionalInt(entry, "refresh_ticks")
                        ));
                    } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                        villagerPerProfession.put(profession, new ProfessionOverride(value.getAsInt(), null));
                    }
                });
            }
        }
    }

    public static int startingBalanceForProfession(String professionId) {
        ProfessionOverride override = villagerPerProfession.get(professionId);
        return override != null ? override.balance() : villagerDefaultStarting;
    }

    public static int refreshTicksForProfession(String professionId) {
        ProfessionOverride override = villagerPerProfession.get(professionId);
        if (override != null && override.refreshTicks() != null) {
            return override.refreshTicks();
        }
        return villagerRefreshTicks;
    }

    public static Map<String, Integer> professionStartingBalances() {
        Map<String, Integer> balances = new HashMap<>();
        villagerPerProfession.forEach((profession, override) ->
                balances.put(profession, override.balance()));
        return balances;
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Integer readOptionalInt(JsonObject json, String key) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return null;
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
