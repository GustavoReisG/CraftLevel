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

    public static final int DEFAULT_EFFECT_MAX = 255;

    /** Limite por efeito listado: Time (upgrades de duração) e Level (nível máx. / strength). */
    public record EffectLimit(int time, int level) {
        public EffectLimit {
            time = Math.max(1, time);
            level = Math.max(1, level);
        }
    }

    public static int gloryCostTime = 10;
    public static int gloryCostStrength = 10;
    public static int maxUpgradesTime = 5;
    public static int maxUpgradesStrength = 5;
    public static Map<ResourceLocation, EffectLimit> effectLimits = Map.of();

    private AltarPotionParser() {}

    public static void parse(JsonObject root) {
        gloryCostTime = 10;
        gloryCostStrength = 10;
        maxUpgradesTime = 5;
        maxUpgradesStrength = 5;
        Map<ResourceLocation, EffectLimit> limits = new LinkedHashMap<>();

        if (root != null && root.has("potion") && root.get("potion").isJsonObject()) {
            JsonObject potion = root.getAsJsonObject("potion");
            gloryCostTime = Math.max(1, readInt(potion, "glory_cost_time", 10));
            gloryCostStrength = Math.max(1, readInt(potion, "glory_cost_strength", 10));

            if (potion.has("max_upgrades_time") || potion.has("max_upgrades_strength")) {
                maxUpgradesTime = Math.max(1, readInt(potion, "max_upgrades_time", 5));
                maxUpgradesStrength = Math.max(1, readInt(potion, "max_upgrades_strength", 5));
            } else if (potion.has("max_upgrades")) {
                // Compat: max_upgrades antigo → metade / metade (mín. 1 cada).
                int total = Math.max(2, readInt(potion, "max_upgrades", 10));
                maxUpgradesTime = Math.max(1, total / 2);
                maxUpgradesStrength = Math.max(1, total - maxUpgradesTime);
            }

            if (potion.has("effect_limits") && potion.get("effect_limits").isJsonObject()) {
                JsonObject map = potion.getAsJsonObject("effect_limits");
                for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                    if (id == null) {
                        continue;
                    }
                    EffectLimit limit = readEffectLimit(entry.getValue(), maxUpgradesTime);
                    if (limit != null) {
                        limits.put(id, limit);
                    }
                }
            }
        }

        effectLimits = Collections.unmodifiableMap(limits);
        CraftLevel.LOGGER.info(
                "[CraftLevel] Altar potion config: time={}, strength={}, maxTime={}, maxStrength={}, effectLimits={}",
                gloryCostTime,
                gloryCostStrength,
                maxUpgradesTime,
                maxUpgradesStrength,
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

    /** Nível máximo (amplifier + 1). Lista → level da entrada; senão → {@link #DEFAULT_EFFECT_MAX}. */
    public static int maxLevel(MobEffect effect) {
        EffectLimit limit = limitOf(effect);
        if (limit != null) {
            return limit.level();
        }
        return DEFAULT_EFFECT_MAX;
    }

    /**
     * Lê entrada da lista: objeto {@code {time, level}} ou int legado (= level; time = defaultTime).
     */
    public static EffectLimit readEffectLimit(JsonElement value, int defaultTime) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        try {
            if (value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                int time = Math.max(1, readInt(obj, "time", defaultTime));
                int level = Math.max(1, readInt(obj, "level", 1));
                return new EffectLimit(time, level);
            }
            return new EffectLimit(Math.max(1, defaultTime), Math.max(1, value.getAsInt()));
        } catch (Exception e) {
            return null;
        }
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
