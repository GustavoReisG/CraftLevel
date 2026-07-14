package com.grgs.craftlevel.villager;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Cache de expansão de tags/items para evitar varrer o registro a cada slot. */
public final class TagExpansionCache {

    private static final Map<String, List<Item>> CACHE = new HashMap<>();

    private static final List<String> WOOL_COLORS = List.of(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    );

    private TagExpansionCache() {}

    public static void clear() {
        CACHE.clear();
    }

    public static List<Item> expand(String entry) {
        if (entry == null || entry.isBlank()) {
            return List.of();
        }
        return CACHE.computeIfAbsent(entry, TagExpansionCache::resolve);
    }

    public static Set<Item> expandAll(List<String> entries) {
        Set<Item> result = new HashSet<>();
        for (String entry : entries) {
            result.addAll(expand(entry));
        }
        return result;
    }

    private static List<Item> resolve(String entry) {
        if (entry.startsWith("#craftlevel:")) {
            return resolveSynthetic(entry);
        }

        String normalized = entry.startsWith("#") ? entry.substring(1) : entry;
        ResourceLocation id = ResourceLocation.tryParse(normalized);
        if (id == null) {
            return List.of();
        }

        if (entry.startsWith("#")) {
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), id);
            List<Item> items = new ArrayList<>();
            BuiltInRegistries.ITEM.getTag(tag).ifPresent(set ->
                    set.stream().map(Holder::value).forEach(items::add));
            return Collections.unmodifiableList(items);
        }

        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null) {
            return List.of();
        }
        return List.of(item);
    }

    private static List<Item> resolveSynthetic(String entry) {
        if ("#craftlevel:colored_banners".equals(entry)) {
            return coloredItems("banner");
        }
        return List.of();
    }

    private static List<Item> coloredItems(String suffix) {
        List<Item> items = new ArrayList<>();
        for (String color : WOOL_COLORS) {
            addItem(items, "minecraft:" + color + "_" + suffix);
        }
        return Collections.unmodifiableList(items);
    }

    private static void addItem(List<Item> items, String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
        if (item != null) {
            items.add(item);
        }
    }
}
