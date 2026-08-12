package com.grgs.craftlevel.config;

import com.google.gson.JsonObject;

/** Valores carregados de glory_kills.json. */
public final class GloryKillsParser {

    public static int killsPerShard = 100;
    public static int minLevelGap = 5;

    private GloryKillsParser() {}

    public static void parse(JsonObject json) {
        killsPerShard = 100;
        minLevelGap = 5;

        if (json == null) {
            return;
        }

        killsPerShard = Math.max(1, readInt(json, "kills_per_shard", 100));
        minLevelGap = Math.max(0, readInt(json, "min_level_gap", 5));
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
