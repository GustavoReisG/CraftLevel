package com.grgs.craftlevel.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Arenas, biomas e hordas sorteáveis, gravados em {@code config/craftlevel/arena.json}. */
public final class ArenaSettings {

    private final List<HordeSet> hordes = new ArrayList<>();
    private final List<Schematic> schematics = new ArrayList<>();

    public List<HordeSet> hordes() {
        return hordes;
    }

    public List<Schematic> schematics() {
        return schematics;
    }

    public static ArenaSettings load(Path file, List<String> discovered) {
        ArenaSettings settings = new ArenaSettings();
        if (!Files.exists(file)) {
            copyDefault(file);
        }
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("hordes")) {
                    for (JsonElement element : root.getAsJsonArray("hordes")) {
                        if (element.isJsonObject()) {
                            settings.hordes.add(HordeSet.fromJson(element.getAsJsonObject()));
                        }
                    }
                }
                if (root.has("schematics")) {
                    for (JsonElement element : root.getAsJsonArray("schematics")) {
                        settings.schematics.add(Schematic.fromJson(element.getAsJsonObject(), settings.hordes));
                    }
                }
            } catch (Exception ignored) {
                settings.hordes.clear();
                settings.schematics.clear();
            }
        }
        settings.mergeDiscovered(discovered);
        return settings;
    }

    private static void copyDefault(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (InputStream stream = ArenaSettings.class.getClassLoader()
                    .getResourceAsStream("assets/craftlevel/defaults/arena.json")) {
                if (stream == null) {
                    return;
                }
                Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonArray hordesJson = new JsonArray();
        for (HordeSet horde : hordes) {
            hordesJson.add(horde.toJson());
        }
        root.add("hordes", hordesJson);
        JsonArray list = new JsonArray();
        for (Schematic schematic : schematics) {
            list.add(schematic.toJson());
        }
        root.add("schematics", list);
        return root;
    }

    private void mergeDiscovered(List<String> discovered) {
        Map<String, Schematic> byFile = new LinkedHashMap<>();
        for (Schematic schematic : schematics) {
            if (schematic.file != null && !schematic.file.isBlank()) {
                byFile.put(schematic.file, schematic);
            }
        }
        List<String> files = new ArrayList<>();
        if (discovered != null) {
            for (String file : discovered) {
                if (file != null && !file.isBlank() && !files.contains(file)) {
                    files.add(file);
                }
            }
        }
        if (files.isEmpty()) {
            files.addAll(byFile.keySet());
        }
        if (files.stream().noneMatch(file -> file.toLowerCase(Locale.ROOT).startsWith("wood_arena"))) {
            files.add(0, "wood_arena.schem");
        }

        schematics.clear();
        for (String file : files) {
            Schematic schematic = byFile.remove(file);
            if (schematic == null) {
                schematic = new Schematic();
                schematic.file = file;
                schematic.biome = defaultBiome(file);
            }
            schematics.add(schematic);
        }
    }

    public static String defaultBiome(String file) {
        String name = file == null ? "" : file.toLowerCase(Locale.ROOT);
        if (name.startsWith("wood_arena")) {
            return "minecraft:jungle";
        }
        return "minecraft:plains";
    }

    public static String displayName(String file) {
        if (file == null) {
            return "";
        }
        if (file.endsWith(".schem")) {
            return file.substring(0, file.length() - ".schem".length());
        }
        return file;
    }

    public static final class Schematic {
        public String file = "";
        public String biome = "minecraft:plains";
        /** Nomes das hordas globais que esta arena pode sortear. */
        public final List<String> hordeNames = new ArrayList<>();

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("file", file);
            json.addProperty("biome", biome == null ? "" : biome);
            JsonArray list = new JsonArray();
            for (String name : hordeNames) {
                list.add(name);
            }
            json.add("hordes", list);
            return json;
        }

        static Schematic fromJson(JsonObject json, List<HordeSet> library) {
            Schematic schematic = new Schematic();
            schematic.file = json.has("file") ? json.get("file").getAsString() : "";
            schematic.biome = json.has("biome") ? json.get("biome").getAsString() : defaultBiome(schematic.file);
            if (json.has("hordes")) {
                for (JsonElement element : json.getAsJsonArray("hordes")) {
                    if (element.isJsonPrimitive()) {
                        schematic.hordeNames.add(element.getAsString());
                    } else if (element.isJsonObject()) {
                        HordeSet set = HordeSet.fromJson(element.getAsJsonObject());
                        if (library.stream().noneMatch(existing -> existing.name.equals(set.name))) {
                            library.add(set);
                        }
                        if (set.name != null && !set.name.isBlank()) {
                            schematic.hordeNames.add(set.name);
                        }
                    }
                }
            }
            return schematic;
        }
    }

    /** Conjunto nomeado de levas. Uma arena sorteia um destes. */
    public static final class HordeSet {
        public String name = "";
        public final List<Wave> waves = new ArrayList<>();

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("name", name == null ? "" : name);
            JsonArray list = new JsonArray();
            for (Wave wave : waves) {
                list.add(wave.toJson());
            }
            json.add("waves", list);
            return json;
        }

        static HordeSet fromJson(JsonObject json) {
            HordeSet set = new HordeSet();
            set.name = json.has("name") ? json.get("name").getAsString() : "";
            if (json.has("waves")) {
                for (JsonElement element : json.getAsJsonArray("waves")) {
                    set.waves.add(Wave.fromJson(element.getAsJsonObject()));
                }
            }
            return set;
        }
    }

    public static final class Wave {
        public final List<Mob> mobs = new ArrayList<>();

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            JsonArray list = new JsonArray();
            for (Mob mob : mobs) {
                list.add(mob.toJson());
            }
            json.add("mobs", list);
            return json;
        }

        static Wave fromJson(JsonObject json) {
            Wave wave = new Wave();
            if (json.has("mobs")) {
                for (JsonElement element : json.getAsJsonArray("mobs")) {
                    wave.mobs.add(Mob.fromJson(element.getAsJsonObject()));
                }
            }
            return wave;
        }
    }

    public static final class Mob {
        public String id = "";
        public String count = "1";
        public String helmet = "";
        public String chestplate = "";
        public String leggings = "";
        public String boots = "";
        public String hand = "";
        public String effect = "";

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("mob", id == null ? "" : id);
            json.addProperty("count", count == null ? "" : count);
            json.addProperty("helmet", helmet == null ? "" : helmet);
            json.addProperty("chestplate", chestplate == null ? "" : chestplate);
            json.addProperty("leggings", leggings == null ? "" : leggings);
            json.addProperty("boots", boots == null ? "" : boots);
            json.addProperty("hand", hand == null ? "" : hand);
            json.addProperty("effect", effect == null ? "" : effect);
            return json;
        }

        static Mob fromJson(JsonObject json) {
            Mob mob = new Mob();
            mob.id = text(json, "mob");
            mob.count = text(json, "count");
            if (mob.count.isBlank()) {
                mob.count = "1";
            }
            mob.helmet = text(json, "helmet");
            mob.chestplate = text(json, "chestplate");
            mob.leggings = text(json, "leggings");
            mob.boots = text(json, "boots");
            mob.hand = text(json, "hand");
            mob.effect = text(json, "effect");
            return mob;
        }

        private static String text(JsonObject json, String key) {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
        }
    }

    public void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(toJson()));
    }
}
