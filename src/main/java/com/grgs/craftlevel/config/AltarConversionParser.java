package com.grgs.craftlevel.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Regras de conversão Glory → Skill Points do altar. */
public final class AltarConversionParser {

    public record ConversionRule(
            ResourceLocation itemId,
            ResourceLocation tagId,
            int count,
            int gloryCost,
            int skillPoints
    ) {
        public boolean matches(ItemStack stack) {
            if (stack.isEmpty() || stack.getCount() < count) {
                return false;
            }
            if (tagId != null) {
                TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagId);
                return stack.is(tag);
            }
            return ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(itemId);
        }
    }

    public static List<ConversionRule> rules = List.of();

    private AltarConversionParser() {}

    public static void parse(JsonObject json) {
        List<ConversionRule> loaded = new ArrayList<>();
        if (json != null && json.has("conversions") && json.get("conversions").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("conversions")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                ConversionRule rule = readRule(element.getAsJsonObject());
                if (rule != null) {
                    loaded.add(rule);
                }
            }
        }
        rules = List.copyOf(loaded);
        CraftLevel.LOGGER.info("[CraftLevel] Altar conversion rules loaded: {}", rules.size());
    }

    public static Optional<ConversionRule> findMatch(ItemStack stack) {
        for (ConversionRule rule : rules) {
            if (rule.matches(stack)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    private static ConversionRule readRule(JsonObject obj) {
        int count = Math.max(1, readInt(obj, "count", 1));
        int gloryCost = Math.max(1, readInt(obj, "glory_cost", 100));
        int skillPoints = Math.max(1, readInt(obj, "skill_points", 1));

        if (obj.has("tag")) {
            ResourceLocation tagId = ResourceLocation.tryParse(obj.get("tag").getAsString());
            if (tagId == null) {
                return null;
            }
            return new ConversionRule(null, tagId, count, gloryCost, skillPoints);
        }

        if (!obj.has("item")) {
            return null;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(obj.get("item").getAsString());
        if (itemId == null || !ForgeRegistries.ITEMS.containsKey(itemId)) {
            CraftLevel.LOGGER.warn("[CraftLevel] Unknown altar conversion item: {}", obj.get("item"));
            return null;
        }
        return new ConversionRule(itemId, null, count, gloryCost, skillPoints);
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
