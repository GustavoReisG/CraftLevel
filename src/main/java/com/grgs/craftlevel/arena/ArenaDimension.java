package com.grgs.craftlevel.arena;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Dimensão vazia onde cada visita cola uma cópia da arena. */
public final class ArenaDimension {

    public static final ResourceKey<Level> LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(CraftLevel.MODID, "arena")
    );

    private ArenaDimension() {}
}
