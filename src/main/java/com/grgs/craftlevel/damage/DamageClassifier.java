package com.grgs.craftlevel.damage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * Extrai o id do {@link DamageType} a partir de uma {@link DamageSource}.
 * <p>
 * Esse id é usado como chave no wiring {@code hurt} do config
 * (ex: {@code minecraft:player_attack} → escalador {@code melee_power}).
 * <p>
 * As constantes abaixo são referência dos ids vanilla mais comuns — não limitam
 * quais tipos podem ser configurados no JSON.
 */
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

    /** Retorna {@code namespace:path} do damage type ou {@code unknown} se não resolvível. */
    public static String getDamageTypeId(DamageSource source) {
        ResourceKey<DamageType> key = source.typeHolder().unwrapKey().orElse(null);
        return key != null ? key.location().toString() : "unknown";
    }
}
