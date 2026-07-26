package com.grgs.craftlevel.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.config.EconomyParser;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Estado editável da seção kill_rewards em economy.json. */
final class KillRewardsEditorState {

    private int sameLevelEmeralds = 2;
    private int lowLevelEmeralds = 1;
    private int highLevelBonus = 3;
    private int levelGap = 5;
    private int bossBonus = 50;
    private final LinkedHashSet<String> bosses = new LinkedHashSet<>(EconomyParser.defaultBosses());
    private final LinkedHashSet<String> blacklist = new LinkedHashSet<>(EconomyParser.defaultBlacklist());

    static KillRewardsEditorState loadFrom(Path file) {
        KillRewardsEditorState state = new KillRewardsEditorState();
        if (!Files.exists(file)) {
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            state.importFrom(json);
        } catch (Exception ignored) {
            // defaults
        }
        return state;
    }

    void importFrom(JsonObject root) {
        if (root == null || !root.has("kill_rewards") || !root.get("kill_rewards").isJsonObject()) {
            return;
        }
        JsonObject kill = root.getAsJsonObject("kill_rewards");
        sameLevelEmeralds = Math.max(0, readInt(kill, "same_level_emeralds", 2));
        lowLevelEmeralds = Math.max(0, readInt(kill, "low_level_emeralds", 1));
        highLevelBonus = Math.max(0, readInt(kill, "high_level_bonus", 3));
        levelGap = Math.max(1, readInt(kill, "level_gap", 5));
        bossBonus = Math.max(0, readInt(kill, "boss_bonus", 50));

        bosses.clear();
        if (kill.has("bosses") && kill.get("bosses").isJsonArray()) {
            for (JsonElement element : kill.getAsJsonArray("bosses")) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    String id = element.getAsString().trim();
                    if (!id.isEmpty()) {
                        bosses.add(id);
                    }
                }
            }
        }
        if (bosses.isEmpty()) {
            bosses.addAll(EconomyParser.defaultBosses());
        }

        blacklist.clear();
        if (kill.has("blacklist") && kill.get("blacklist").isJsonArray()) {
            for (JsonElement element : kill.getAsJsonArray("blacklist")) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    String id = element.getAsString().trim();
                    if (!id.isEmpty()) {
                        blacklist.add(id);
                    }
                }
            }
        } else {
            blacklist.addAll(EconomyParser.defaultBlacklist());
        }
    }

    JsonObject toKillRewardsJson() {
        JsonObject kill = new JsonObject();
        kill.addProperty("same_level_emeralds", sameLevelEmeralds);
        kill.addProperty("low_level_emeralds", lowLevelEmeralds);
        kill.addProperty("high_level_bonus", highLevelBonus);
        kill.addProperty("level_gap", levelGap);
        kill.addProperty("boss_bonus", bossBonus);

        JsonArray bossesArray = new JsonArray();
        for (String boss : bosses) {
            bossesArray.add(boss);
        }
        kill.add("bosses", bossesArray);

        JsonArray blacklistArray = new JsonArray();
        for (String mob : blacklist) {
            blacklistArray.add(mob);
        }
        kill.add("blacklist", blacklistArray);
        return kill;
    }

    int sameLevelEmeralds() {
        return sameLevelEmeralds;
    }

    void setSameLevelEmeralds(int value) {
        sameLevelEmeralds = Math.max(0, value);
    }

    int lowLevelEmeralds() {
        return lowLevelEmeralds;
    }

    void setLowLevelEmeralds(int value) {
        lowLevelEmeralds = Math.max(0, value);
    }

    int highLevelBonus() {
        return highLevelBonus;
    }

    void setHighLevelBonus(int value) {
        highLevelBonus = Math.max(0, value);
    }

    int levelGap() {
        return levelGap;
    }

    void setLevelGap(int value) {
        levelGap = Math.max(1, value);
    }

    int bossBonus() {
        return bossBonus;
    }

    void setBossBonus(int value) {
        bossBonus = Math.max(0, value);
    }

    List<String> bosses() {
        return new ArrayList<>(bosses);
    }

    void setBosses(List<String> ids) {
        bosses.clear();
        for (String id : ids) {
            if (id == null) {
                continue;
            }
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                bosses.add(trimmed);
            }
        }
    }

    List<String> blacklist() {
        return new ArrayList<>(blacklist);
    }

    void setBlacklist(List<String> ids) {
        blacklist.clear();
        for (String id : ids) {
            if (id == null) {
                continue;
            }
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                blacklist.add(trimmed);
            }
        }
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
