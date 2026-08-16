package com.grgs.craftlevel.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.grgs.craftlevel.config.SkillsParser;
import com.grgs.craftlevel.skill.SkillCatalog;
import com.grgs.craftlevel.skill.SkillDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Estado editável de skills.json. */
final class SkillsEditorState {

    record LevelRow(int level, int skillPointCost, int minPlayerLevel) {
        LevelRow withCost(int cost) {
            return new LevelRow(level, Math.max(1, cost), minPlayerLevel);
        }

        LevelRow withMinLevel(int minLevel) {
            return new LevelRow(level, skillPointCost, Math.max(1, minLevel));
        }
    }

    private final Map<String, List<LevelRow>> skills = new LinkedHashMap<>();

    static SkillsEditorState loadFrom(java.nio.file.Path file) {
        SkillsEditorState state = defaults();
        try {
            if (!java.nio.file.Files.exists(file)) {
                return state;
            }
            String raw = java.nio.file.Files.readString(file);
            JsonObject json = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            SkillsParser.parse(json);
            for (SkillDefinition def : SkillCatalog.all()) {
                List<LevelRow> rows = new ArrayList<>();
                for (int level = 1; level <= def.maxLevel(); level++) {
                    SkillsParser.LevelConfig config = SkillsParser.forLevel(def.id(), level);
                    rows.add(new LevelRow(level, config.skillPointCost(), config.minPlayerLevel()));
                }
                state.skills.put(def.id(), rows);
            }
        } catch (Exception ignored) {
        }
        return state;
    }

    static SkillsEditorState defaults() {
        SkillsEditorState state = new SkillsEditorState();
        for (SkillDefinition def : SkillCatalog.all()) {
            List<LevelRow> rows = new ArrayList<>();
            for (int level = 1; level <= def.maxLevel(); level++) {
                SkillsParser.LevelConfig config = SkillsParser.defaultFor(def.id(), level);
                rows.add(new LevelRow(level, config.skillPointCost(), config.minPlayerLevel()));
            }
            state.skills.put(def.id(), rows);
        }
        return state;
    }

    List<LevelRow> rows(String skillId) {
        return skills.computeIfAbsent(skillId, id -> new ArrayList<>());
    }

    void setRow(String skillId, int index, LevelRow row) {
        List<LevelRow> rows = rows(skillId);
        if (index >= 0 && index < rows.size()) {
            rows.set(index, row);
        }
    }

    JsonObject toJson() {
        JsonObject root = new JsonObject();
        skills.forEach((id, rows) -> {
            JsonArray array = new JsonArray();
            for (LevelRow row : rows) {
                JsonObject obj = new JsonObject();
                obj.addProperty("level", row.level());
                obj.addProperty("skill_point_cost", row.skillPointCost());
                obj.addProperty("min_player_level", row.minPlayerLevel());
                array.add(obj);
            }
            root.add(id, array);
        });
        return root;
    }
}
