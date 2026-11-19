package com.grgs.craftlevel.damage;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Damage types custom do mod (datapack registry). Nome: Skill + Damage. */
public final class ModDamageTypes {

    public static final ResourceKey<DamageType> SUPERNOVA_DAMAGE = key("supernova_damage");
    public static final ResourceKey<DamageType> NETHER_RUSH_DAMAGE = key("nether_rush_damage");
    public static final ResourceKey<DamageType> NETHER_ELEVATION_DAMAGE = key("nether_elevation_damage");
    public static final ResourceKey<DamageType> INFERNO_BOLT_DAMAGE = key("inferno_bolt_damage");
    public static final ResourceKey<DamageType> SOUL_LANCE_DAMAGE = key("soul_lance_damage");
    public static final ResourceKey<DamageType> OVERCHARGE_DAMAGE = key("overcharge_damage");
    public static final ResourceKey<DamageType> RETURN_DAMAGE = key("return_damage");

    /** @deprecated use {@link #INFERNO_BOLT_DAMAGE} */
    @Deprecated
    public static final ResourceKey<DamageType> INFERNO_BOLT = INFERNO_BOLT_DAMAGE;
    /** @deprecated use {@link #SOUL_LANCE_DAMAGE} */
    @Deprecated
    public static final ResourceKey<DamageType> SOUL_LANCE = SOUL_LANCE_DAMAGE;

    private ModDamageTypes() {}

    public static DamageSource supernovaDamage(Level level, Entity attacker) {
        return create(level, SUPERNOVA_DAMAGE, attacker);
    }

    public static DamageSource netherRushDamage(Level level, Entity attacker) {
        return create(level, NETHER_RUSH_DAMAGE, attacker);
    }

    public static DamageSource netherElevationDamage(Level level, Entity attacker) {
        return create(level, NETHER_ELEVATION_DAMAGE, attacker);
    }

    public static DamageSource infernoBoltDamage(Level level, Entity attacker) {
        return create(level, INFERNO_BOLT_DAMAGE, attacker);
    }

    public static DamageSource soulLanceDamage(Level level, Entity attacker) {
        return create(level, SOUL_LANCE_DAMAGE, attacker);
    }

    public static DamageSource overchargeDamage(Level level, Entity attacker) {
        return create(level, OVERCHARGE_DAMAGE, attacker);
    }

    public static DamageSource returnDamage(Level level, Entity attacker) {
        return create(level, RETURN_DAMAGE, attacker);
    }

    /** @deprecated use {@link #infernoBoltDamage} */
    @Deprecated
    public static DamageSource infernoBolt(Level level, Entity attacker) {
        return infernoBoltDamage(level, attacker);
    }

    /** @deprecated use {@link #soulLanceDamage} */
    @Deprecated
    public static DamageSource soulLance(Level level, Entity attacker) {
        return soulLanceDamage(level, attacker);
    }

    public static DamageSource create(Level level, ResourceKey<DamageType> type, Entity attacker) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type);
        return new DamageSource(holder, attacker);
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(CraftLevel.MODID, path));
    }
}
