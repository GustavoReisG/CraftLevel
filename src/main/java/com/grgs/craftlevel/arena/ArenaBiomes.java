package com.grgs.craftlevel.arena;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Preenche o volume do schematic com um bioma. As células do jogo são de 4×4×4. */
public final class ArenaBiomes {

    private ArenaBiomes() {}

    public static void fill(ServerLevel level, ArenaSchematic.Bounds bounds, ResourceKey<Biome> biome) {
        Holder<Biome> holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome);
        int minX = QuartPos.fromBlock(bounds.minX());
        int minY = QuartPos.fromBlock(bounds.minY());
        int minZ = QuartPos.fromBlock(bounds.minZ());
        int maxX = QuartPos.fromBlock(bounds.maxX());
        int maxY = QuartPos.fromBlock(bounds.maxY());
        int maxZ = QuartPos.fromBlock(bounds.maxZ());
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();

        Set<LevelChunk> chunks = new HashSet<>();
        int chunkMinX = bounds.minX() >> 4;
        int chunkMaxX = bounds.maxX() >> 4;
        int chunkMinZ = bounds.minZ() >> 4;
        int chunkMaxZ = bounds.maxZ() >> 4;
        for (int chunkX = chunkMinX; chunkX <= chunkMaxX; chunkX++) {
            for (int chunkZ = chunkMinZ; chunkZ <= chunkMaxZ; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                chunk.fillBiomesFromNoise((x, y, z, ignored) -> {
                    if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                        return holder;
                    }
                    return chunk.getNoiseBiome(x, y, z);
                }, sampler);
                chunk.setUnsaved(true);
                chunks.add(chunk);
            }
        }
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.copyOf(chunks));
    }
}
