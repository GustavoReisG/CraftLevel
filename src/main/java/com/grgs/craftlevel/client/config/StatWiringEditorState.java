package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.config.ConfigParser;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado editável de player_stats.json / mob_stats.json / mob override na UI.
 * Estrutura fixa: seções attribute, hurt e heal (hurt/heal exigem entrada default).
 */
final class StatWiringEditorState {

    enum Section {
        ATTRIBUTE,
        HURT,
        HEAL;

        boolean requiresDefault() {
            return this == HURT || this == HEAL;
        }
    }

    private final LinkedHashMap<String, String> attribute = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> hurt = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> heal = new LinkedHashMap<>();

    static StatWiringEditorState loadFrom(Path file) {
        StatWiringEditorState state = new StatWiringEditorState();
        if (!Files.exists(file)) {
            state.ensureRequiredDefaults();
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            state.importFrom(json);
        } catch (Exception ignored) {
            // JSON inválido: sessão vazia
        }
        state.ensureRequiredDefaults();
        return state;
    }

    static StatWiringEditorState createEmpty() {
        StatWiringEditorState state = new StatWiringEditorState();
        state.ensureRequiredDefaults();
        return state;
    }

    void importFrom(JsonObject json) {
        if (json.has("attribute") && json.get("attribute").isJsonObject()) {
            readSection(json.getAsJsonObject("attribute"), attribute);
        }
        if (json.has("hurt") && json.get("hurt").isJsonObject()) {
            readSection(json.getAsJsonObject("hurt"), hurt);
        }
        if (json.has("heal") && json.get("heal").isJsonObject()) {
            readSection(json.getAsJsonObject("heal"), heal);
        }
    }

    JsonObject toJson() {
        JsonObject root = new JsonObject();
        if (attribute.size() > 0) {
            root.add("attribute", writeSection(attribute));
        }
        if (hurt.size() > 0) {
            root.add("hurt", writeSection(hurt));
        }
        if (heal.size() > 0) {
            root.add("heal", writeSection(heal));
        }
        return root;
    }

    LinkedHashMap<String, String> section(Section section) {
        return switch (section) {
            case ATTRIBUTE -> attribute;
            case HURT -> hurt;
            case HEAL -> heal;
        };
    }

    void setSection(Section section, LinkedHashMap<String, String> entries) {
        LinkedHashMap<String, String> target = section(section);
        target.clear();
        target.putAll(entries);
        if (section.requiresDefault()) {
            ensureDefaultEntry(target);
        }
    }

    private void ensureRequiredDefaults() {
        ensureDefaultEntry(hurt);
        ensureDefaultEntry(heal);
    }

    private static void ensureDefaultEntry(LinkedHashMap<String, String> map) {
        if (!map.containsKey(ConfigParser.DEFAULT_KEY)) {
            map.put(ConfigParser.DEFAULT_KEY, "");
        }
    }

    private static void readSection(JsonObject section, LinkedHashMap<String, String> target) {
        section.asMap().forEach((entryKey, value) -> {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                target.put(entryKey, value.getAsString());
            }
        });
    }

    private static JsonObject writeSection(LinkedHashMap<String, String> map) {
        JsonObject section = new JsonObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            section.addProperty(entry.getKey(), entry.getValue());
        }
        return section;
    }
}
