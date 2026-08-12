package com.grgs.craftlevel.block;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registro de blocos do mod. */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CraftLevel.MODID);

    public static final RegistryObject<Block> COMPRESSED_EMERALD_BLOCK = BLOCKS.register(
            "compressed_emerald_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.EMERALD)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL))
    );

    public static final RegistryObject<Block> PLAYER_GRAVESTONE = BLOCKS.register(
            "player_gravestone",
            PlayerGravestoneBlock::new
    );

    public static final RegistryObject<Block> ALTAR_OF_DEATH = BLOCKS.register(
            "altar_of_death",
            AltarOfDeathBlock::new
    );

    private ModBlocks() {}
}
