package com.grgs.craftlevel.entity;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.entity.projectile.InfernoBoltProjectile;
import com.grgs.craftlevel.entity.projectile.SoulLanceProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registro de EntityTypes do mod. */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CraftLevel.MODID);

    public static final RegistryObject<EntityType<InfernoBoltProjectile>> INFERNO_BOLT =
            ENTITY_TYPES.register(
                    "inferno_bolt",
                    () -> EntityType.Builder.<InfernoBoltProjectile>of(
                                    InfernoBoltProjectile::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .fireImmune()
                            .build("inferno_bolt")
            );

    public static final RegistryObject<EntityType<SoulLanceProjectile>> SOUL_LANCE =
            ENTITY_TYPES.register(
                    "soul_lance",
                    () -> EntityType.Builder.<SoulLanceProjectile>of(
                                    SoulLanceProjectile::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .build("soul_lance")
            );

    private ModEntities() {}
}
