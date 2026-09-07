package com.grgs.craftlevel.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Limites de nível de encantamento do altar (independentes do vanilla). */
public final class AltarImprovementParser {

    public static int gloryCost = 20;
    public static int genericEnchantmentLimit = 50;
    public static Map<ResourceLocation, Integer> enchantmentLimits = Map.of();

    private AltarImprovementParser() {}

    public static void parse(JsonObject root) {
        gloryCost = 20;
        genericEnchantmentLimit = 50;
        Map<ResourceLocation, Integer> limits = new LinkedHashMap<>();

        if (root != null && root.has("improvement") && root.get("improvement").isJsonObject()) {
            JsonObject improvement = root.getAsJsonObject("improvement");
            gloryCost = Math.max(1, readInt(improvement, "glory_cost", 20));
            genericEnchantmentLimit = Math.max(1, readInt(improvement, "generic_enchantment_limit", 50));

            if (improvement.has("enchantment_limits") && improvement.get("enchantment_limits").isJsonObject()) {
                JsonObject map = improvement.getAsJsonObject("enchantment_limits");
                for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                    if (id == null) {
                        continue;
                    }
                    try {
                        int level = Math.max(1, entry.getValue().getAsInt());
                        limits.put(id, level);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        enchantmentLimits = Collections.unmodifiableMap(limits);
        CraftLevel.LOGGER.info(
                "[CraftLevel] Altar improvement config: glory={}, genericMax={}, overrides={}",
                gloryCost,
                genericEnchantmentLimit,
                enchantmentLimits.size()
        );
    }

    /** Nível máximo permitido pelo altar para o encantamento. */
    public static int maxLevel(Enchantment enchantment) {
        if (enchantment == null) {
            return 1;
        }
        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (id != null) {
            Integer override = enchantmentLimits.get(id);
            if (override != null) {
                return override;
            }
        }
        return genericEnchantmentLimit;
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
