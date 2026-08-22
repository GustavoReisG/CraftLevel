package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.entity.projectile.InfernoBoltProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Inferno Bolt: fireball rápida na mira. Explosão simulada (dano creeper via
 * craftlevel:inferno_bolt_damage), sem quebrar blocos. CD 2 min. L5: 2 bolas.
 */
public final class InfernoBoltSkill {

    public static final int COOLDOWN_TICKS = 2 * 60 * 20;

    private InfernoBoltSkill() {}

    /** L1: 2, L2–3: 3, L4–5: 4. */
    public static int blastRadius(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 2;
            case 2, 3 -> 3;
            default -> 4;
        };
    }

    public static int boltCount(int level) {
        return Math.max(1, level) >= 5 ? 2 : 1;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            look = player.getForward();
        }
        look = look.normalize();
        int radius = blastRadius(level);
        int count = boltCount(level);

        spawnBolt(player, look, radius);
        if (count >= 2) {
            Vec3 offset = rotateYaw(look, 8.0D);
            spawnBolt(player, offset, radius);
        }

        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.GHAST_SHOOT,
                SoundSource.PLAYERS,
                0.75F,
                1.15F
        );
    }

    private static void spawnBolt(ServerPlayer player, Vec3 direction, int radius) {
        InfernoBoltProjectile bolt = new InfernoBoltProjectile(player.serverLevel(), player, direction, radius);
        player.serverLevel().addFreshEntity(bolt);
    }

    private static Vec3 rotateYaw(Vec3 dir, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = dir.x * cos - dir.z * sin;
        double z = dir.x * sin + dir.z * cos;
        return new Vec3(x, dir.y, z).normalize();
    }
}
