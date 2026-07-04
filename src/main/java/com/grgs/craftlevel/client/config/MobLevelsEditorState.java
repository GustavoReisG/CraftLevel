package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado editável de mob_levels.json na UI.
 * Estrutura fixa por mob: lvl_min, lvl_max. Lista de mobs vem dos valores do JSON.
 */
final class MobLevelsEditorState {

    record MobLevelEntry(Integer lvlMin, Integer lvlMax) {}

    private final LinkedHashMap<String, MobLevelEntry> levels = new LinkedHashMap<>();

    static MobLevelsEditorState loadFrom(Path file) {
        MobLevelsEditorState state = new MobLevelsEditorState();
        if (!Files.exists(file)) {
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            state.importFrom(json);
        } catch (Exception ignored) {
            // JSON inválido: sessão vazia
        }
        return state;
    }

    void importFrom(JsonObject json) {
        json.asMap().forEach((mobId, value) -> {
            if (!value.isJsonObject()) {
                return;
            }
            JsonObject entry = value.getAsJsonObject();
            levels.put(mobId, new MobLevelEntry(
                    readOptionalInt(entry, "lvl_min"),
                    readOptionalInt(entry, "lvl_max")
            ));
        });
    }

    JsonObject toJson() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, MobLevelEntry> entry : levels.entrySet()) {
            String mobId = entry.getKey().trim();
            if (mobId.isEmpty()) {
                continue;
            }
            JsonObject obj = new JsonObject();
            MobLevelEntry values = entry.getValue();
            if (values.lvlMin() != null) {
                obj.addProperty("lvl_min", values.lvlMin());
            }
            if (values.lvlMax() != null) {
                obj.addProperty("lvl_max", values.lvlMax());
            }
            root.add(mobId, obj);
        }
        return root;
    }

    LinkedHashMap<String, MobLevelEntry> levels() {
        return levels;
    }

    void putLevel(String mobId, MobLevelEntry entry) {
        levels.put(mobId, entry);
    }

    void removeLevel(String mobId) {
        levels.remove(mobId);
    }

    void clear() {
        levels.clear();
    }

    private static Integer readOptionalInt(JsonObject json, String key) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return null;
        }
    }
}
