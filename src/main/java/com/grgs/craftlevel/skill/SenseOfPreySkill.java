package com.grgs.craftlevel.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Sense of Prey: Glowing em hostis e outros players no raio. */
public final class SenseOfPreySkill {

    private SenseOfPreySkill() {}

    public static int radiusBlocks(int level) {
        return switch (Math.max(1, level)) {
            case 1, 2 -> 16;
            case 3, 4 -> 24;
            default -> 32;
        };
    }

    public static int durationSeconds(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 30;
            case 2, 3 -> 45;
            default -> 60;
        };
    }

    public static int durationTicks(int level) {
        return durationSeconds(level) * 20;
    }

    public static void activate(ServerPlayer player, int level) {
        int radius = radiusBlocks(level);
        int duration = durationTicks(level);
        double radiusSq = (double) radius * radius;
        AABB area = player.getBoundingBox().inflate(radius);

        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area, target ->
                isValidTarget(player, target) && target.distanceToSqr(player) <= radiusSq)) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true));
        }

        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.SPYGLASS_USE,
                SoundSource.PLAYERS,
                1.0F,
                1.1F
        );
    }

    private static boolean isValidTarget(ServerPlayer caster, LivingEntity target) {
        if (target == caster || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player) {
            return true;
        }
        return target instanceof Enemy;
    }
}
