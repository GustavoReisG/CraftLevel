package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.entity.projectile.SoulLanceProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Soul Lance: tridente sem gravidade. Dano 2×/3× tridente + raios. CD 90s.
 */
public final class SoulLanceSkill {

    public static final int COOLDOWN_TICKS = 90 * 20;

    private SoulLanceSkill() {}

    /** L1–2: 2×, L3: 3×. */
    public static float damageMultiplier(int level) {
        return Math.max(1, level) >= 3 ? 3.0F : 2.0F;
    }

    /** L1: 1, L2+: 2. */
    public static int lightningCount(int level) {
        return Math.max(1, level) >= 2 ? 2 : 1;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            look = player.getForward();
        }
        SoulLanceProjectile lance = new SoulLanceProjectile(
                player.serverLevel(),
                player,
                look,
                damageMultiplier(level),
                lightningCount(level)
        );
        player.serverLevel().addFreshEntity(lance);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                0.85F,
                1.1F
        );
    }
}
