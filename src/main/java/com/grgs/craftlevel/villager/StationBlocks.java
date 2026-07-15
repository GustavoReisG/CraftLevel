package com.grgs.craftlevel.villager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Verifica se um bloco de estação de profissão existe no registry. */
public final class StationBlocks {

    private StationBlocks() {}

    public static boolean exists(String stationId) {
        ResourceLocation id = ResourceLocation.tryParse(stationId);
        return id != null && exists(id);
    }

    public static boolean exists(ResourceLocation stationId) {
        if (stationId == null || !BuiltInRegistries.BLOCK.containsKey(stationId)) {
            return false;
        }
        Block block = BuiltInRegistries.BLOCK.get(stationId);
        return block != null && block != Blocks.AIR;
    }
}
