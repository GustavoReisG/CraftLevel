package com.grgs.craftlevel.arena;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/** Cada schematic de arena e o bioma aplicado na área colada. */
public final class ArenaCatalog {

    public record Entry(String id, String resource, ResourceKey<Biome> biome) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry("wood", "/data/craftlevel/arena/wood_arena.schem", biome("minecraft:jungle")),
            new Entry("deepdark", "/data/craftlevel/arena/deep_dark_cave.schem", biome("minecraft:deep_dark"))
    );

    private ArenaCatalog() {}

    public static Entry pick(RandomSource random) {
        return ENTRIES.get(random.nextInt(ENTRIES.size()));
    }

    public static Entry find(String id) {
        for (Entry entry : ENTRIES) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return ENTRIES.get(0);
    }

    private static ResourceKey<Biome> biome(String id) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation(id));
    }
}
