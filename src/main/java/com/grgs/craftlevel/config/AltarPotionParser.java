package com.grgs.craftlevel.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Configuração da aba Potion do altar. */
public final class AltarPotionParser {

    /** Duração máxima padrão (10 minutos). */
    public static final int DEFAULT_MAX_DURATION_SECONDS = 600;
    /** Nível máximo padrão (amplifier + 1). */
    public static final int DEFAULT_MAX_LEVEL = 10;

    /**
     * Limite por efeito listado.
     * {@code time} = duração máxima em segundos; {@code level} = nível máximo da poção.
     */
    public record EffectLimit(int time, int level) {
        public EffectLimit {
            time = Math.max(1, time);
            level = Math.max(1, level);
        }
    }

    public static int gloryCostTime = 10;
    public static int gloryCostStrength = 10;
    public static int maxDurationSeconds = DEFAULT_MAX_DURATION_SECONDS;
    public static int maxLevel = DEFAULT_MAX_LEVEL;
    public static Map<ResourceLocation, EffectLimit> effectLimits = Map.of();

    private AltarPotionParser() {}

    public static void parse(JsonObject root) {
        gloryCostTime = 10;
        gloryCostStrength = 10;
        maxDurationSeconds = DEFAULT_MAX_DURATION_SECONDS;
        maxLevel = DEFAULT_MAX_LEVEL;
        Map<ResourceLocation, EffectLimit> limits = new LinkedHashMap<>();

        if (root != null && root.has("potion") && root.get("potion").isJsonObject()) {
            JsonObject potion = root.getAsJsonObject("potion");
            gloryCostTime = Math.max(1, readInt(potion, "glory_cost_time", 10));
            gloryCostStrength = Math.max(1, readInt(potion, "glory_cost_strength", 10));
            maxDurationSeconds = readMaxDurationSeconds(potion);
            maxLevel = readMaxLevel(potion);

            if (potion.has("effect_limits") && potion.get("effect_limits").isJsonObject()) {
                JsonObject map = potion.getAsJsonObject("effect_limits");
                for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                    if (id == null) {
                        continue;
                    }
                    EffectLimit limit = readEffectLimit(entry.getValue(), maxDurationSeconds);
                    if (limit != null) {
                        limits.put(id, limit);
                    }
                }
            }
        }

        effectLimits = Collections.unmodifiableMap(limits);
        CraftLevel.LOGGER.info(
                "[CraftLevel] Altar potion config: timeCost={}, strengthCost={}, maxDuration={}s, maxLevel={}, effectLimits={}",
                gloryCostTime,
                gloryCostStrength,
                maxDurationSeconds,
                maxLevel,
                effectLimits.size()
        );
    }

    /** Override do efeito, ou null se não estiver na lista. */
    public static EffectLimit limitOf(MobEffect effect) {
        if (effect == null) {
            return null;
        }
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (id == null) {
            return null;
        }
        return effectLimits.get(id);
    }

    /** Duração máxima em segundos. Lista → time da entrada; senão → global. */
    public static int maxDurationSeconds(MobEffect effect) {
        EffectLimit limit = limitOf(effect);
        if (limit != null) {
            return limit.time();
        }
        return maxDurationSeconds;
    }

    /** Nível máximo (amplifier + 1). Lista → level da entrada; senão → global. */
    public static int maxLevel(MobEffect effect) {
        EffectLimit limit = limitOf(effect);
        if (limit != null) {
            return limit.level();
        }
        return maxLevel;
    }

    /**
     * Lê entrada da lista: objeto {@code {time, level}} ou int legado (= level; time = defaultDurationSeconds).
     * {@code time} é duração máxima em segundos.
     */
    public static EffectLimit readEffectLimit(JsonElement value, int defaultDurationSeconds) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        try {
            if (value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                int time = Math.max(1, readInt(obj, "time", defaultDurationSeconds));
                int level = Math.max(1, readInt(obj, "level", 1));
                return new EffectLimit(time, level);
            }
            return new EffectLimit(Math.max(1, defaultDurationSeconds), Math.max(1, value.getAsInt()));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Preferência: {@code max_duration_seconds}.
     * Compat: {@code max_upgrades_time} antigo (contagem) → segundos via ×30.
     */
    public static int readMaxDurationSeconds(JsonObject potion) {
        if (potion.has("max_duration_seconds")) {
            return Math.max(1, readInt(potion, "max_duration_seconds", DEFAULT_MAX_DURATION_SECONDS));
        }
        if (potion.has("max_upgrades_time")) {
            return Math.max(1, readInt(potion, "max_upgrades_time", 5) * 30);
        }
        if (potion.has("max_upgrades")) {
            int total = Math.max(2, readInt(potion, "max_upgrades", 10));
            return Math.max(1, (total / 2) * 30);
        }
        return DEFAULT_MAX_DURATION_SECONDS;
    }

    /**
     * Preferência: {@code max_level}.
     * Compat: {@code max_upgrades_strength} antigo usado como nível máximo.
     */
    public static int readMaxLevel(JsonObject potion) {
        if (potion.has("max_level")) {
            return Math.max(1, readInt(potion, "max_level", DEFAULT_MAX_LEVEL));
        }
        if (potion.has("max_upgrades_strength")) {
            return Math.max(1, readInt(potion, "max_upgrades_strength", DEFAULT_MAX_LEVEL));
        }
        if (potion.has("max_upgrades")) {
            int total = Math.max(2, readInt(potion, "max_upgrades", 10));
            return Math.max(1, total - total / 2);
        }
        return DEFAULT_MAX_LEVEL;
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
