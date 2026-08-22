package com.grgs.craftlevel.skill;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Blink Step: teleporta na mira 5/8 blocos, sempre pousando no chão quando possível.
 * CD 90s. Sem destino válido → não inicia CD.
 */
public final class BlinkStepSkill {

    public static final int COOLDOWN_TICKS = 90 * 20;
    private static final int MAX_HEIGHT_DELTA = 2;

    private BlinkStepSkill() {}

    public static int distanceBlocks(int level) {
        return Math.max(1, level) >= 2 ? 8 : 5;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    /**
     * @return {@code false} se não houver destino válido (não inicia CD).
     */
    public static boolean activate(ServerPlayer player, int level) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            look = player.getForward();
        }
        look = look.normalize();
        int distance = distanceBlocks(level);
        Vec3 origin = player.position();
        Vec3 ideal = origin.add(look.scale(distance));

        BlockPos dest = findLanding(player.serverLevel(), origin, look, distance, ideal);
        if (dest == null) {
            return false;
        }

        double x = dest.getX() + 0.5D;
        double y = dest.getY();
        double z = dest.getZ() + 0.5D;

        ServerLevel world = player.serverLevel();
        world.sendParticles(
                ParticleTypes.PORTAL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                24,
                0.3D,
                0.6D,
                0.3D,
                0.15D
        );
        player.teleportTo(x, y, z);
        player.fallDistance = 0.0F;
        world.sendParticles(ParticleTypes.PORTAL, x, y + 1.0D, z, 24, 0.3D, 0.6D, 0.3D, 0.15D);
        world.playSound(
                null,
                BlockPos.containing(x, y, z),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.9F,
                1.2F
        );
        return true;
    }

    private static BlockPos findLanding(
            ServerLevel level,
            Vec3 origin,
            Vec3 look,
            int distance,
            Vec3 ideal
    ) {
        BlockPos idealFeet = BlockPos.containing(ideal.x, ideal.y, ideal.z);
        BlockPos snapped = snapToGround(level, idealFeet, Mth.floor(origin.y));
        if (snapped != null && Math.abs(snapped.getY() - Mth.floor(origin.y)) <= MAX_HEIGHT_DELTA) {
            return snapped;
        }

        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (int step = distance; step >= 1; step--) {
            Vec3 point = origin.add(look.scale(step));
            BlockPos candidate = snapToGround(level, BlockPos.containing(point.x, point.y, point.z), Mth.floor(origin.y));
            if (candidate == null) {
                continue;
            }
            double d = candidate.distToCenterSqr(ideal.x, ideal.y, ideal.z);
            if (d < bestDistSq) {
                bestDistSq = d;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Procura pés válidos (air + air acima + sólido abaixo) perto da coluna,
     * priorizando altura próxima de {@code preferY}.
     */
    private static BlockPos snapToGround(ServerLevel level, BlockPos around, int preferY) {
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int dy = -MAX_HEIGHT_DELTA - 2; dy <= MAX_HEIGHT_DELTA + 4; dy++) {
            BlockPos feet = new BlockPos(around.getX(), preferY + dy, around.getZ());
            if (!canStand(level, feet)) {
                continue;
            }
            int score = Math.abs(feet.getY() - preferY);
            if (score < bestScore) {
                bestScore = score;
                best = feet;
            }
        }
        // Também tenta a Y do around.
        for (int dy = -4; dy <= 6; dy++) {
            BlockPos feet = around.offset(0, dy, 0);
            if (!canStand(level, feet)) {
                continue;
            }
            int score = Math.abs(feet.getY() - preferY) + 1;
            if (score < bestScore) {
                bestScore = score;
                best = feet;
            }
        }
        return best;
    }

    private static boolean canStand(ServerLevel level, BlockPos feet) {
        BlockState below = level.getBlockState(feet.below());
        if (!below.blocksMotion() && below.getCollisionShape(level, feet.below()).isEmpty()) {
            return false;
        }
        return isFree(level, feet) && isFree(level, feet.above());
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty();
    }
}
