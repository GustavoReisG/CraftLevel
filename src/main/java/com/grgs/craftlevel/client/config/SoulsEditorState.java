package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.grgs.craftlevel.config.SoulsParser;

/** Estado editável de souls.json. */
final class SoulsEditorState {

    private int maxSouls = 3;
    private int timeRestore = 1800;
    private SoulsParser.LostEmeraldsType lostEmeraldsType = SoulsParser.LostEmeraldsType.PERCENTAGE;
    private double lostEmeraldsQuantity = 10.0;
    private SoulsParser.LostItemsMode lostItems = SoulsParser.LostItemsMode.INVENTORY;
    private boolean gravestone = true;

    static SoulsEditorState loadFrom(java.nio.file.Path file) {
        SoulsEditorState state = new SoulsEditorState();
        try {
            if (!java.nio.file.Files.exists(file)) {
                return state;
            }
            String raw = java.nio.file.Files.readString(file);
            JsonObject json = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            SoulsParser.parse(json);
            state.maxSouls = SoulsParser.maxSouls;
            state.timeRestore = SoulsParser.timeRestoreSeconds;
            state.lostEmeraldsType = SoulsParser.lostEmeraldsType;
            state.lostEmeraldsQuantity = SoulsParser.lostEmeraldsQuantity;
            state.lostItems = SoulsParser.lostItems;
            state.gravestone = SoulsParser.gravestone;
        } catch (Exception ignored) {
        }
        return state;
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("max_souls", maxSouls);
        json.addProperty("time_restore", timeRestore);
        json.addProperty("lost_emeralds_type", lostEmeraldsType.id());
        json.addProperty("lost_emeralds_quantity", lostEmeraldsQuantity);
        json.addProperty("lost_items", lostItems.id());
        json.addProperty("gravestone", gravestone);
        return json;
    }

    int maxSouls() {
        return maxSouls;
    }

    void setMaxSouls(int value) {
        maxSouls = Math.max(0, value);
    }

    int timeRestore() {
        return timeRestore;
    }

    void setTimeRestore(int value) {
        timeRestore = Math.max(1, value);
    }

    SoulsParser.LostEmeraldsType lostEmeraldsType() {
        return lostEmeraldsType;
    }

    void cycleLostEmeraldsType() {
        lostEmeraldsType = lostEmeraldsType == SoulsParser.LostEmeraldsType.PERCENTAGE
                ? SoulsParser.LostEmeraldsType.QUANTITY
                : SoulsParser.LostEmeraldsType.PERCENTAGE;
    }

    double lostEmeraldsQuantity() {
        return lostEmeraldsQuantity;
    }

    void setLostEmeraldsQuantity(double value) {
        lostEmeraldsQuantity = Math.max(0.0, value);
    }

    SoulsParser.LostItemsMode lostItems() {
        return lostItems;
    }

    void cycleLostItems() {
        lostItems = lostItems.next();
    }

    boolean gravestone() {
        return gravestone;
    }

    void cycleGravestone() {
        gravestone = !gravestone;
    }
}
