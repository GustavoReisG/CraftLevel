package com.grgs.craftlevel.config;

import com.google.gson.JsonObject;

/** Valores carregados de souls.json. */
public final class SoulsParser {

    public enum LostEmeraldsType {
        QUANTITY,
        PERCENTAGE;

        public static LostEmeraldsType fromString(String raw) {
            if (raw == null) {
                return PERCENTAGE;
            }
            return switch (raw.trim().toLowerCase()) {
                case "quantity" -> QUANTITY;
                default -> PERCENTAGE;
            };
        }

        public String id() {
            return name().toLowerCase();
        }
    }

    public enum LostItemsMode {
        RANDOM,
        ARMOR,
        EQUIPMENT,
        ARMOR_EQUIPMENT,
        HOTBAR,
        INVENTORY,
        ALL_ITEMS,
        NONE;

        public static LostItemsMode fromString(String raw) {
            if (raw == null) {
                return ALL_ITEMS;
            }
            return switch (raw.trim().toLowerCase()) {
                case "random" -> RANDOM;
                case "armor" -> ARMOR;
                case "equipment" -> EQUIPMENT;
                case "armor_equipment", "armor+equipment", "armor_and_equipment" -> ARMOR_EQUIPMENT;
                case "hotbar" -> HOTBAR;
                case "inventory" -> INVENTORY;
                case "none" -> NONE;
                default -> ALL_ITEMS;
            };
        }

        public String id() {
            return switch (this) {
                case ARMOR_EQUIPMENT -> "armor_equipment";
                case ALL_ITEMS -> "all_items";
                default -> name().toLowerCase();
            };
        }

        public String displayName() {
            return switch (this) {
                case RANDOM -> "Random";
                case ARMOR -> "Armor";
                case EQUIPMENT -> "Equipment";
                case ARMOR_EQUIPMENT -> "Armor + Equipment";
                case HOTBAR -> "Hotbar";
                case INVENTORY -> "Inventory";
                case ALL_ITEMS -> "All Items";
                case NONE -> "None";
            };
        }

        public LostItemsMode next() {
            LostItemsMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public static int maxSouls = 3;
    public static int timeRestoreSeconds = 1800;
    public static LostEmeraldsType lostEmeraldsType = LostEmeraldsType.PERCENTAGE;
    public static double lostEmeraldsQuantity = 10.0;
    public static LostItemsMode lostItems = LostItemsMode.INVENTORY;
    public static boolean gravestone = true;

    private SoulsParser() {}

    public static void parse(JsonObject json) {
        maxSouls = 3;
        timeRestoreSeconds = 1800;
        lostEmeraldsType = LostEmeraldsType.PERCENTAGE;
        lostEmeraldsQuantity = 10.0;
        lostItems = LostItemsMode.INVENTORY;
        gravestone = true;

        if (json == null) {
            return;
        }

        maxSouls = Math.max(0, readInt(json, "max_souls", 3));
        timeRestoreSeconds = Math.max(1, readInt(json, "time_restore", 1800));
        lostEmeraldsType = LostEmeraldsType.fromString(readString(json, "lost_emeralds_type", "percentage"));
        lostEmeraldsQuantity = Math.max(0.0, readDouble(json, "lost_emeralds_quantity", 10.0));
        lostItems = LostItemsMode.fromString(readString(json, "lost_items", "inventory"));
        if (json.has("gravestone")) {
            try {
                gravestone = json.get("gravestone").getAsBoolean();
            } catch (Exception ignored) {
                gravestone = true;
            }
        }
    }

    public static int restoreTicksRequired() {
        return timeRestoreSeconds * 20;
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double readDouble(JsonObject json, String key, double fallback) {
        try {
            return json.get(key).getAsDouble();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String readString(JsonObject json, String key, String fallback) {
        try {
            return json.get(key).getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }
}
