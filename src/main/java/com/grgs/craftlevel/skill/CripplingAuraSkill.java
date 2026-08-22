package com.grgs.craftlevel.skill;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.common.Mod;

/**
 * Crippling Aura: névoa no chão (raio 4). Hostis recebem Slowness + Weakness 10s.
 * CD 60s. 5 níveis de amplifier.
 */
public final class CripplingAuraSkill {

    public static final int COOLDOWN_TICKS = 60 * 20;
    public static final int RADIUS_BLOCKS = 4;
    public static final int DURATION_SECONDS = 10;

    private CripplingAuraSkill() {}

    /** Amplifier 0-based: L1 S0/W0 … L5 S2/W2. */
    public static int slownessAmplifier(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 0;
            case 2, 3 -> 1;
            default -> 2;
        };
    }

    public static int weaknessAmplifier(int level) {
        return switch (Math.max(1, level)) {
            case 1, 2 -> 0;
            case 3, 4 -> 1;
            default -> 2;
        };
    }

    /** Níveis de exibição (1-based) para a descrição. */
    public static int slownessLevel(int level) {
        return slownessAmplifier(level) + 1;
    }

    public static int weaknessLevel(int level) {
        return weaknessAmplifier(level) + 1;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        ServerLevel world = player.serverLevel();
        int durationTicks = DURATION_SECONDS * 20;
        int slow = slownessAmplifier(level);
        int weak = weaknessAmplifier(level);
        double radius = RADIUS_BLOCKS;
        double radiusSq = radius * radius;
        AABB area = player.getBoundingBox().inflate(radius);

        for (LivingEntity entity : world.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> isValidTarget(player, target) && target.distanceToSqr(player) <= radiusSq
        )) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, slow, false, true, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, weak, false, true, true));
        }

        long until = world.getGameTime() + durationTicks;
        ActiveSkillTracker.put(player, SkillCatalog.CRIPPLING_AURA, until);

        world.playSound(
                null,
                player.blockPosition(),
                SoundEvents.EVOKER_PREPARE_ATTACK,
                SoundSource.PLAYERS,
                0.7F,
                0.75F
        );
        for (int i = 0; i < 40; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0D;
            double dist = world.random.nextDouble() * radius;
            double px = player.getX() + Math.cos(angle) * dist;
            double pz = player.getZ() + Math.sin(angle) * dist;
            world.sendParticles(
                    ParticleTypes.CLOUD,
                    px,
                    player.getY() + 0.15D,
                    pz,
                    1,
                    0.05D,
                    0.02D,
                    0.05D,
                    0.0D
            );
            if (i % 3 == 0) {
                world.sendParticles(
                        ParticleTypes.SMOKE,
                        px,
                        player.getY() + 0.2D,
                        pz,
                        1,
                        0.05D,
                        0.05D,
                        0.05D,
                        0.0D
                );
            }
        }
    }

    private static boolean isValidTarget(ServerPlayer caster, LivingEntity target) {
        return target != caster && target.isAlive() && target instanceof Enemy;
    }
}
