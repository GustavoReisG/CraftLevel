package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.grgs.craftlevel.config.GloryKillsParser;

/** Estado editável de glory_kills.json. */
final class GloryKillsEditorState {

    private int killsPerShard = 100;
    private int minLevelGap = 5;

    static GloryKillsEditorState loadFrom(java.nio.file.Path file) {
        GloryKillsEditorState state = new GloryKillsEditorState();
        try {
            if (!java.nio.file.Files.exists(file)) {
                return state;
            }
            String raw = java.nio.file.Files.readString(file);
            JsonObject json = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            GloryKillsParser.parse(json);
            state.killsPerShard = GloryKillsParser.killsPerShard;
            state.minLevelGap = GloryKillsParser.minLevelGap;
        } catch (Exception ignored) {
        }
        return state;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("kills_per_shard", killsPerShard);
        json.addProperty("min_level_gap", minLevelGap);
        return json;
    }

    int killsPerShard() {
        return killsPerShard;
    }

    void setKillsPerShard(int value) {
        killsPerShard = Math.max(1, value);
    }

    int minLevelGap() {
        return minLevelGap;
    }

    void setMinLevelGap(int value) {
        minLevelGap = Math.max(0, value);
    }
}
