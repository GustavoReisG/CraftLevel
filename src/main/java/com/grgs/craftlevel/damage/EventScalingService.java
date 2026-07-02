package com.grgs.craftlevel.damage;

import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.ConfigParser;
import com.grgs.craftlevel.scaling.ScalingDefinition;
import com.grgs.craftlevel.scaling.ScalingEngine;
import com.grgs.craftlevel.scaling.ScalingRegistry;
import com.grgs.craftlevel.util.DebugLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Aplica scaling de hurt/heal em tempo de combate.
 * <p>
 * Fluxo: damage type → wiring (player_stats / mob_stats) → nome do escalador
 * → {@link ScalingRegistry} → valor calculado → {@link ScalingEngine#applyEventAmount}.
 */
public final class EventScalingService {

    private EventScalingService() {}

    /**
     * Escala dano causado por uma entidade viva com base no level do atacante.
     *
     * @param damageTypeId id do DamageType (ex: minecraft:player_attack)
     */
    public static float applyHurtScaling(LivingEntity attacker, String damageTypeId, float amount) {
        int level = getLevel(attacker);
        if (level <= 0) {
            return amount;
        }

        String scalerName;
        String context;

        if (attacker instanceof Player) {
            scalerName = ConfigParser.resolvePlayerHurtScaler(damageTypeId);
            context = "player hurt:" + damageTypeId;
        } else if (attacker instanceof Mob mob) {
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            scalerName = ConfigParser.resolveMobHurtScaler(mobId, damageTypeId);
            context = "mob hurt:" + mobId + "/" + damageTypeId;
        } else {
            return amount;
        }

        return applyScaler(amount, level, scalerName, context, damageTypeId);
    }

    /** Escala cura recebida pela entidade (chave heal do config, geralmente "default"). */
    public static float applyHealScaling(LivingEntity entity, String healKey, float amount) {
        int level = getLevel(entity);
        if (level <= 0) {
            return amount;
        }

        String scalerName;
        String context;

        if (entity instanceof Player) {
            scalerName = ConfigParser.resolvePlayerHealScaler(healKey);
            context = "player heal:" + healKey;
        } else if (entity instanceof Mob mob) {
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            scalerName = ConfigParser.resolveMobHealScaler(mobId, healKey);
            context = "mob heal:" + mobId + "/" + healKey;
        } else {
            return amount;
        }

        return applyScaler(amount, level, scalerName, context, healKey);
    }

    private static float applyScaler(float amount, int level, String scalerName, String context, String eventKey) {
        if (scalerName == null) {
            DebugLog.info("Scaling skip: sem escalador para {} ({})", eventKey, context);
            return amount;
        }

        ScalingDefinition def = ScalingRegistry.require(scalerName, context);
        if (def == null) {
            return amount;
        }

        double scaledValue = ScalingEngine.computeScaledValue(def, level);
        float result = ScalingEngine.applyEventAmount(amount, scaledValue, def.applyMode());

        DebugLog.info(
                "Scaling {}: escalador={} level={} valor={} amount {} -> {}",
                context,
                scalerName,
                level,
                scaledValue,
                amount,
                result
        );

        return result;
    }

    /** Lê level da capability de player ou mob. */
    public static int getLevel(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getCapability(PlayerLevelProvider.PLAYER_LEVEL)
                    .map(cap -> cap.getLevel())
                    .orElse(0);
        }
        if (entity instanceof Mob mob) {
            return mob.getCapability(MobLevelProvider.MOB_LEVEL)
                    .map(cap -> cap.getLevel())
                    .orElse(0);
        }
        return 0;
    }
}
