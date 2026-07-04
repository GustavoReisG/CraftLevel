package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Estado editável de mob_overrides.json. Chaves = mob ids (valores); wiring usa estrutura fixa do mod. */
final class MobOverridesEditorState {

    private final LinkedHashMap<String, StatWiringEditorState> mobs = new LinkedHashMap<>();

    static MobOverridesEditorState loadFrom(Path file) {
        MobOverridesEditorState state = new MobOverridesEditorState();
        if (!Files.exists(file)) {
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            json.asMap().forEach((mobId, value) -> {
                if (!value.isJsonObject()) {
                    return;
                }
                StatWiringEditorState wiring = StatWiringEditorState.createEmpty();
                wiring.importFrom(value.getAsJsonObject());
                state.mobs.put(mobId, wiring);
            });
        } catch (Exception ignored) {
            // JSON inválido: sessão vazia
        }
        return state;
    }

    JsonObject toJson() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, StatWiringEditorState> entry : mobs.entrySet()) {
            String mobId = entry.getKey().trim();
            if (mobId.isEmpty()) {
                continue;
            }
            JsonObject mobJson = entry.getValue().toJson();
            if (mobJson.size() > 0) {
                root.add(mobId, mobJson);
            }
        }
        return root;
    }

    LinkedHashMap<String, StatWiringEditorState> mobs() {
        return mobs;
    }

    StatWiringEditorState mob(String mobId) {
        return mobs.computeIfAbsent(mobId, id -> StatWiringEditorState.createEmpty());
    }

    void removeMob(String mobId) {
        mobs.remove(mobId);
    }

    void renameMob(String oldId, String newId) {
        if (oldId.equals(newId)) {
            return;
        }
        StatWiringEditorState wiring = mobs.remove(oldId);
        if (wiring != null) {
            mobs.put(newId, wiring);
        }
    }
}
