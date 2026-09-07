package com.grgs.craftlevel.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Estado editável de altar_conversion.json. */
final class AltarConversionEditorState {

    record ConversionRow(String itemId, int gloryCost, int count, int skillPoints) {}

    private final List<ConversionRow> rows = new ArrayList<>();

    static AltarConversionEditorState loadFrom(Path file) {
        AltarConversionEditorState state = new AltarConversionEditorState();
        if (!Files.exists(file)) {
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (!json.has("conversions") || !json.get("conversions").isJsonArray()) {
                return state;
            }
            for (JsonElement element : json.getAsJsonArray("conversions")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                String itemId = null;
                if (obj.has("item")) {
                    itemId = obj.get("item").getAsString();
                } else if (obj.has("tag")) {
                    itemId = "#" + obj.get("tag").getAsString();
                }
                if (itemId == null || itemId.isBlank()) {
                    continue;
                }
                state.rows.add(new ConversionRow(
                        itemId.trim(),
                        Math.max(1, readInt(obj, "glory_cost", 100)),
                        Math.max(1, readInt(obj, "count", 1)),
                        Math.max(1, readInt(obj, "skill_points", 1))
                ));
            }
        } catch (Exception ignored) {
        }
        return state;
    }

    JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonArray conversions = new JsonArray();
        for (ConversionRow row : rows) {
            String id = row.itemId() == null ? "" : row.itemId().trim();
            if (id.isEmpty()) {
                continue;
            }
            JsonObject obj = new JsonObject();
            if (id.startsWith("#")) {
                obj.addProperty("tag", id.substring(1));
            } else {
                obj.addProperty("item", id);
            }
            obj.addProperty("count", Math.max(1, row.count()));
            obj.addProperty("glory_cost", Math.max(1, row.gloryCost()));
            obj.addProperty("skill_points", Math.max(1, row.skillPoints()));
            conversions.add(obj);
        }
        root.add("conversions", conversions);
        return root;
    }

    List<ConversionRow> rows() {
        return rows;
    }

    void setRows(List<ConversionRow> next) {
        rows.clear();
        rows.addAll(next);
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
