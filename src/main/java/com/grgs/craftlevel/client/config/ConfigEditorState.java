package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.scaling.ApplyMode;
import com.grgs.craftlevel.scaling.ScalingMode;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado editável de config.json na UI.
 * Estrutura fixa: debug, lp_scaling (5 campos), scaling (mapa de variáveis).
 * Importa apenas valores nos campos conhecidos.
 */
final class ConfigEditorState {

    record ScalerEntry(
            Double baseValue,
            Double scaling,
            ScalingMode scalingMode,
            ApplyMode applyMode
    ) {
        static ScalerEntry empty() {
            return new ScalerEntry(null, null, null, null);
        }
    }

    private Boolean debug;
    private Integer mobLevelVariation;
    private Integer lvlMax;
    private Double lpBaseValue;
    private Double lpScaling;
    private ScalingMode lpScalingMode;
    private final LinkedHashMap<String, ScalerEntry> scalers = new LinkedHashMap<>();

    static ConfigEditorState loadFrom(Path file) {
        ConfigEditorState state = new ConfigEditorState();
        if (!Files.exists(file)) {
            return state;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            state.importFrom(json);
        } catch (Exception ignored) {
            // JSON inválido ou formato inesperado: sessão vazia
        }
        return state;
    }

    void importFrom(JsonObject json) {
        if (json.has("debug") && json.get("debug").isJsonPrimitive()) {
            try {
                debug = json.get("debug").getAsBoolean();
            } catch (Exception ignored) {
            }
        }

        if (json.has("lp_scaling") && json.get("lp_scaling").isJsonObject()) {
            JsonObject lp = json.getAsJsonObject("lp_scaling");
            mobLevelVariation = readOptionalInt(lp, "mob_level_variation");
            lvlMax = readOptionalInt(lp, "lvl_max");
            lpBaseValue = readOptionalDouble(lp, "base_value");
            lpScaling = readOptionalDouble(lp, "scaling");
            lpScalingMode = readOptionalEnum(lp, "scaling_mode", ScalingMode.class);
        }

        if (json.has("scaling") && json.get("scaling").isJsonObject()) {
            json.getAsJsonObject("scaling").asMap().forEach((name, value) -> {
                if (!value.isJsonObject()) {
                    return;
                }
                scalers.put(name, readScaler(value.getAsJsonObject()));
            });
        }
    }

    JsonObject toJson() {
        JsonObject root = new JsonObject();

        if (debug != null) {
            root.addProperty("debug", debug);
        }

        JsonObject lp = new JsonObject();
        if (mobLevelVariation != null) {
            lp.addProperty("mob_level_variation", mobLevelVariation);
        }
        if (lvlMax != null) {
            lp.addProperty("lvl_max", lvlMax);
        }
        if (lpBaseValue != null) {
            lp.addProperty("base_value", lpBaseValue);
        }
        if (lpScaling != null) {
            lp.addProperty("scaling", lpScaling);
        }
        if (lpScalingMode != null) {
            lp.addProperty("scaling_mode", lpScalingMode.name().toLowerCase());
        }
        if (lp.size() > 0) {
            root.add("lp_scaling", lp);
        }

        if (scalers.size() > 0) {
            JsonObject scaling = new JsonObject();
            for (Map.Entry<String, ScalerEntry> entry : scalers.entrySet()) {
                scaling.add(entry.getKey(), writeScaler(entry.getValue()));
            }
            root.add("scaling", scaling);
        }

        return root;
    }

    Boolean debug() {
        return debug;
    }

    void setDebug(boolean value) {
        debug = value;
    }

    Integer mobLevelVariation() {
        return mobLevelVariation;
    }

    void setMobLevelVariation(Integer value) {
        mobLevelVariation = value;
    }

    Integer lvlMax() {
        return lvlMax;
    }

    void setLvlMax(Integer value) {
        lvlMax = value;
    }

    Double lpBaseValue() {
        return lpBaseValue;
    }

    Double lpScaling() {
        return lpScaling;
    }

    ScalingMode lpScalingMode() {
        return lpScalingMode;
    }

    void setLpScaling(Double baseValue, Double scaling, ScalingMode scalingMode) {
        lpBaseValue = baseValue;
        lpScaling = scaling;
        lpScalingMode = scalingMode;
    }

    LinkedHashMap<String, ScalerEntry> scalers() {
        return scalers;
    }

    ScalerEntry scaler(String name) {
        return scalers.get(name);
    }

    void putScaler(String name, ScalerEntry entry) {
        scalers.put(name, entry);
    }

    void removeScaler(String name) {
        scalers.remove(name);
    }

    void renameScaler(String oldName, String newName, ScalerEntry entry) {
        scalers.remove(oldName);
        scalers.put(newName, entry);
    }

    private static ScalerEntry readScaler(JsonObject json) {
        return new ScalerEntry(
                readOptionalDouble(json, "base_value"),
                readOptionalDouble(json, "scaling"),
                readOptionalEnum(json, "scaling_mode", ScalingMode.class),
                readOptionalEnum(json, "apply_mode", ApplyMode.class)
        );
    }

    private static JsonObject writeScaler(ScalerEntry entry) {
        JsonObject json = new JsonObject();
        if (entry.baseValue() != null) {
            json.addProperty("base_value", entry.baseValue());
        }
        if (entry.scaling() != null) {
            json.addProperty("scaling", entry.scaling());
        }
        if (entry.scalingMode() != null) {
            json.addProperty("scaling_mode", entry.scalingMode().name().toLowerCase());
        }
        if (entry.applyMode() != null) {
            json.addProperty("apply_mode", entry.applyMode().name().toLowerCase());
        }
        return json;
    }

    private static Integer readOptionalInt(JsonObject json, String key) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private static Double readOptionalDouble(JsonObject json, String key) {
        try {
            return json.get(key).getAsDouble();
        } catch (Exception e) {
            return null;
        }
    }

    private static <T extends Enum<T>> T readOptionalEnum(JsonObject json, String key, Class<T> type) {
        try {
            return Enum.valueOf(type, json.get(key).getAsString().trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
