package com.grgs.craftlevel.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Valores carregados de economy.json. */
public final class EconomyParser {

    public record ProfessionOverride(int balance, Integer refreshTicks) {}

    public static long playerStartingBalance = 0L;
    public static int villagerRefreshTicks = 24000;
    public static int villagerDefaultStarting = 1000;
    public static Map<String, ProfessionOverride> villagerPerProfession = Collections.emptyMap();

    /** Emeralds when mob level is within {@link #killLevelGap} of the player. */
    public static int killSameLevelEmeralds = 2;
    /** Fixed emeralds when mob is {@link #killLevelGap}+ levels below the player. */
    public static int killLowLevelEmeralds = 1;
    /** Bonus emeralds when mob is {@link #killLevelGap}+ levels above the player. */
    public static int killHighLevelBonus = 3;
    /** Level difference that separates same / low / high brackets. */
    public static int killLevelGap = 5;
    /** Extra emeralds when the killed mob is listed in {@link #killBosses}. */
    public static int killBossBonus = 50;
    public static Set<String> killBosses = Collections.emptySet();
    /** Mobs that never grant kill-reward emeralds (peaceful animals by default). */
    public static Set<String> killBlacklist = Collections.emptySet();

    private EconomyParser() {}

    public static void parseEconomy(JsonObject json) {
        playerStartingBalance = 0L;
        villagerRefreshTicks = 24000;
        villagerDefaultStarting = 1000;
        villagerPerProfession = new HashMap<>();
        killSameLevelEmeralds = 2;
        killLowLevelEmeralds = 1;
        killHighLevelBonus = 3;
        killLevelGap = 5;
        killBossBonus = 50;
        killBosses = new LinkedHashSet<>();
        killBlacklist = new LinkedHashSet<>(defaultBlacklist());

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

        if (json.has("kill_rewards") && json.get("kill_rewards").isJsonObject()) {
            JsonObject kill = json.getAsJsonObject("kill_rewards");
            killSameLevelEmeralds = Math.max(0, readInt(kill, "same_level_emeralds", 2));
            killLowLevelEmeralds = Math.max(0, readInt(kill, "low_level_emeralds", 1));
            killHighLevelBonus = Math.max(0, readInt(kill, "high_level_bonus", 3));
            killLevelGap = Math.max(1, readInt(kill, "level_gap", 5));
            killBossBonus = Math.max(0, readInt(kill, "boss_bonus", 50));
            killBosses = readIdSet(kill, "bosses");
            if (kill.has("blacklist")) {
                killBlacklist = readIdSet(kill, "blacklist");
            } else {
                killBlacklist = new LinkedHashSet<>(defaultBlacklist());
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

    public static boolean isKillBoss(String entityId) {
        return entityId != null && killBosses.contains(entityId);
    }

    public static boolean isKillBlacklisted(String entityId) {
        return entityId != null && killBlacklist.contains(entityId);
    }

    private static Set<String> readIdSet(JsonObject kill, String key) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (!kill.has(key) || !kill.get(key).isJsonArray()) {
            return ids;
        }
        JsonArray array = kill.getAsJsonArray(key);
        for (JsonElement element : array) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String id = element.getAsString().trim();
                if (!id.isEmpty()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    public static List<String> defaultBosses() {
        return List.of(
                "minecraft:wither",
                "minecraft:warden",
                "minecraft:elder_guardian",
                "minecraft:ender_dragon",
                "cataclysm:the_leviathan",
                "cataclysm:the_harbinger",
                "cataclysm:netherite_monstrosity",
                "cataclysm:scylla",
                "cataclysm:ignis",
                "cataclysm:ender_guardian",
                "cataclysm:maledictus",
                "cataclysm:ancient_remnant",
                "irons_spellbooks:dead_king"
        );
    }

    /** Animais pacíficos / criaturas não-hostis que não dão esmeralda por kill. */
    public static List<String> defaultBlacklist() {
        return List.of(
                "minecraft:allay",
                "minecraft:axolotl",
                "minecraft:bat",
                "minecraft:bee",
                "minecraft:camel",
                "minecraft:cat",
                "minecraft:chicken",
                "minecraft:cod",
                "minecraft:cow",
                "minecraft:dolphin",
                "minecraft:donkey",
                "minecraft:fox",
                "minecraft:frog",
                "minecraft:glow_squid",
                "minecraft:goat",
                "minecraft:horse",
                "minecraft:llama",
                "minecraft:mooshroom",
                "minecraft:mule",
                "minecraft:ocelot",
                "minecraft:panda",
                "minecraft:parrot",
                "minecraft:pig",
                "minecraft:pufferfish",
                "minecraft:rabbit",
                "minecraft:salmon",
                "minecraft:sheep",
                "minecraft:sniffer",
                "minecraft:snow_golem",
                "minecraft:squid",
                "minecraft:strider",
                "minecraft:tadpole",
                "minecraft:trader_llama",
                "minecraft:tropical_fish",
                "minecraft:turtle",
                "minecraft:wolf"
        );
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
