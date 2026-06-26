package com.grgs.craftlevel.damage;

import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Resolve multiplicadores de hurt/heal com base no level e nos JSONs de stats. */
public final class EventScalingService {

    private EventScalingService() {}

    public static double getHurtMultiplier(LivingEntity attacker, String damageTypeId) {
        int level = getLevel(attacker);
        if (level <= 0) {
            return 1.0;
        }

        if (attacker instanceof Player) {
            return ConfigParser.getPlayerHurtMultiplier(damageTypeId, level);
        }
        if (attacker instanceof Mob mob) {
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            return ConfigParser.getMobHurtMultiplier(mobId, damageTypeId, level);
        }
        return 1.0;
    }

    public static double getHealMultiplier(LivingEntity entity, String healKey) {
        int level = getLevel(entity);
        if (level <= 0) {
            return 1.0;
        }

        if (entity instanceof Player) {
            return ConfigParser.getPlayerHealMultiplier(healKey, level);
        }
        if (entity instanceof Mob mob) {
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            return ConfigParser.getMobHealMultiplier(mobId, healKey, level);
        }
        return 1.0;
    }

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
