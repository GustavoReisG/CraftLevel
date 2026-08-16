package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.OptionalDouble;

/**
 * Spectral Climb: no chão, olhando para uma parede, o pulo sobe até
 * min(máx do nível, altura útil da parede). Sem cooldown.
 * <p>
 * Lvl 1→3: 2 / 3 / 4 blocos. Só ativa se a parede útil &gt; 1.25 blocos.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class SpectralClimbSkill {

    public static final double MIN_USEFUL_WALL = 1.25;
    private static final double LOOK_REACH = 1.35;
    private static final int MAX_COLUMN_SCAN = 8;
    private static final double FORWARD_NUDGE = 0.22;

    private SpectralClimbSkill() {}

    /** Lvl 1: 2 blocos, lvl 2: 3, lvl 3: 4. */
    public static double maxClimbBlocks(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 2.0;
            case 2 -> 3.0;
            default -> 4.0;
        };
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.getAbilities().flying) {
            return;
        }

        int skillLevel = skillLevel(player);
        if (skillLevel <= 0) {
            return;
        }

        OptionalDouble usefulOpt = measureUsefulWallHeight(player);
        if (usefulOpt.isEmpty()) {
            return;
        }

        double targetHeight = Math.min(maxClimbBlocks(skillLevel), usefulOpt.getAsDouble());
        applyClimbBoost(player, targetHeight);

        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.serverLevel().playSound(
                    null,
                    serverPlayer.blockPosition(),
                    SoundEvents.WITHER_SHOOT,
                    SoundSource.PLAYERS,
                    0.55F,
                    1.35F
            );
        }
    }

    private static int skillLevel(Player player) {
        if (player.level().isClientSide()) {
            return SyncPlayerGloryPacket.ClientGloryState.skillLevel(SkillCatalog.SPECTRAL_CLIMB);
        }
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getSkillLevel(SkillCatalog.SPECTRAL_CLIMB))
                .orElse(0);
    }

    /**
     * Altura útil do topo da coluna de colisão olhada até os pés do player.
     * Vazio se não estiver olhando uma face lateral ou se útil ≤ 1.25.
     */
    public static OptionalDouble measureUsefulWallHeight(Player player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6) {
            return OptionalDouble.empty();
        }

        BlockHitResult hit = level.clip(new ClipContext(
                eye,
                eye.add(look.normalize().scale(LOOK_REACH)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return OptionalDouble.empty();
        }
        if (hit.getDirection().getAxis() == Direction.Axis.Y) {
            return OptionalDouble.empty();
        }

        BlockPos columnBase = hit.getBlockPos();
        if (!hasSolidCollision(level, columnBase)) {
            return OptionalDouble.empty();
        }

        double topY = collisionTopY(level, columnBase);
        BlockPos cursor = columnBase.above();
        for (int i = 0; i < MAX_COLUMN_SCAN; i++) {
            if (!hasSolidCollision(level, cursor)) {
                break;
            }
            topY = collisionTopY(level, cursor);
            cursor = cursor.above();
        }

        double useful = topY - player.getY();
        if (useful <= MIN_USEFUL_WALL) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(useful);
    }

    private static void applyClimbBoost(Player player, double targetHeight) {
        double vy = velocityForHeight(targetHeight);
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() > 1.0E-6) {
            horizontal = horizontal.normalize().scale(FORWARD_NUDGE);
        } else {
            horizontal = Vec3.ZERO;
        }

        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x + horizontal.x, vy, motion.z + horizontal.z);
        player.hasImpulse = true;
        player.hurtMarked = true;
    }

    /** Simula o drag vertical vanilla para achar o Y inicial que atinge a altura. */
    static double velocityForHeight(double targetHeight) {
        double low = 0.0;
        double high = 2.5;
        for (int i = 0; i < 28; i++) {
            double mid = (low + high) * 0.5;
            if (simulateMaxHeight(mid) < targetHeight) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    private static double simulateMaxHeight(double initialVy) {
        double y = 0.0;
        double vy = initialVy;
        double maxY = 0.0;
        for (int i = 0; i < 200 && (vy > 0.0 || y > 0.0); i++) {
            y += vy;
            maxY = Math.max(maxY, y);
            vy = (vy - 0.08) * 0.98;
            if (y < -1.0) {
                break;
            }
        }
        return maxY;
    }

    private static boolean hasSolidCollision(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty();
    }

    private static double collisionTopY(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return pos.getY();
        }
        return pos.getY() + shape.max(Direction.Axis.Y);
    }
}
