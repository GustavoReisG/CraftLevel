package com.grgs.craftlevel.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.grgs.craftlevel.skill.SkillCatalog;
import com.grgs.craftlevel.skill.SkillDefinition;

import java.util.HashMap;
import java.util.Map;

/** Valores carregados de skills.json (custo SP e min player lvl por nível). */
public final class SkillsParser {

    public record LevelConfig(int skillPointCost, int minPlayerLevel) {}

    private static final Map<String, Map<Integer, LevelConfig>> CONFIG = new HashMap<>();

    private SkillsParser() {}

    public static void parse(JsonObject json) {
        CONFIG.clear();
        if (json == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            Map<Integer, LevelConfig> levels = new HashMap<>();
            JsonArray array = entry.getValue().getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject row = element.getAsJsonObject();
                int level = Math.max(1, readInt(row, "level", 1));
                int cost = Math.max(1, readInt(row, "skill_point_cost", 1));
                int minLevel = Math.max(1, readInt(row, "min_player_level", 1));
                levels.put(level, new LevelConfig(cost, minLevel));
            }
            CONFIG.put(entry.getKey(), levels);
        }
    }

    public static LevelConfig forLevel(String skillId, int level) {
        Map<Integer, LevelConfig> levels = CONFIG.get(skillId);
        if (levels != null && levels.containsKey(level)) {
            return levels.get(level);
        }
        return defaultFor(skillId, level);
    }

    public static LevelConfig defaultFor(String skillId, int level) {
        if (SkillCatalog.SENSE_OF_PREY.equals(skillId)) {
            int min = switch (Math.max(1, level)) {
                case 1 -> 20;
                case 2 -> 25;
                case 3 -> 30;
                case 4 -> 35;
                default -> 40;
            };
            return new LevelConfig(1, min);
        }
        if (SkillCatalog.GHOST_SATURATION.equals(skillId)) {
            return new LevelConfig(1, 30);
        }
        if (SkillCatalog.SOUL_ANCHOR.equals(skillId)) {
            int min = switch (Math.max(1, level)) {
                case 1 -> 20;
                case 2 -> 25;
                case 3 -> 30;
                case 4 -> 35;
                default -> 40;
            };
            return new LevelConfig(1, min);
        }
        if (SkillCatalog.SPECTRAL_CLIMB.equals(skillId)) {
            int min = switch (Math.max(1, level)) {
                case 1 -> 15;
                case 2 -> 20;
                default -> 25;
            };
            return new LevelConfig(1, min);
        }
        if (SkillCatalog.IMBUE_MASTER.equals(skillId)) {
            int min = switch (Math.max(1, level)) {
                case 1 -> 20;
                case 2 -> 25;
                case 3 -> 30;
                case 4 -> 35;
                default -> 40;
            };
            return new LevelConfig(1, min);
        }
        return new LevelConfig(1, 1 + Math.max(0, level - 1) * 5);
    }

    public static JsonObject toDefaultJson() {
        JsonObject root = new JsonObject();
        for (SkillDefinition def : SkillCatalog.all()) {
            JsonArray rows = new JsonArray();
            for (int level = 1; level <= def.maxLevel(); level++) {
                LevelConfig config = defaultFor(def.id(), level);
                JsonObject row = new JsonObject();
                row.addProperty("level", level);
                row.addProperty("skill_point_cost", config.skillPointCost());
                row.addProperty("min_player_level", config.minPlayerLevel());
                rows.add(row);
            }
            root.add(def.id(), rows);
        }
        return root;
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
