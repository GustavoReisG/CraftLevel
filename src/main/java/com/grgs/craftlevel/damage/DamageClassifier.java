package com.grgs.craftlevel.damage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/** Utilitários para obter o id do DamageType a partir de uma DamageSource. */
public final class DamageClassifier {

    public static final String PLAYER_ATTACK = "minecraft:player_attack";
    public static final String MOB_ATTACK = "minecraft:mob_attack";
    public static final String ARROW = "minecraft:arrow";
    public static final String TRIDENT = "minecraft:trident";
    public static final String MAGIC = "minecraft:magic";
    public static final String INDIRECT_MAGIC = "minecraft:indirect_magic";
    public static final String WITHER = "minecraft:wither";
    public static final String POISON = "minecraft:poison";

    private DamageClassifier() {}

    public static String getDamageTypeId(DamageSource source) {
        ResourceKey<DamageType> key = source.typeHolder().unwrapKey().orElse(null);
        return key != null ? key.location().toString() : "unknown";
    }
}
