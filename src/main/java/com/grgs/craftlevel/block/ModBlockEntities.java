package com.grgs.craftlevel.block;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registro de BlockEntityTypes do mod. */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CraftLevel.MODID);

    public static final RegistryObject<BlockEntityType<PlayerGravestoneBlockEntity>> PLAYER_GRAVESTONE =
            BLOCK_ENTITIES.register(
                    "player_gravestone",
                    () -> BlockEntityType.Builder.of(
                            PlayerGravestoneBlockEntity::new,
                            ModBlocks.PLAYER_GRAVESTONE.get()
                    ).build(null)
            );

    private ModBlockEntities() {}
}
