package com.grgs.craftlevel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Aplica Blindness em players próximos enquanto o altar estiver aceso. */
public class AltarOfDeathBlockEntity extends BlockEntity {

    public static final double EFFECT_RADIUS = 8.0;
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int BLINDNESS_DURATION_TICKS = 60;

    public AltarOfDeathBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALTAR_OF_DEATH.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AltarOfDeathBlockEntity be) {
        if (!state.getValue(AltarOfDeathBlock.LIT)) {
            return;
        }
        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        double radiusSq = EFFECT_RADIUS * EFFECT_RADIUS;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        AABB area = new AABB(pos).inflate(EFFECT_RADIUS);

        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
            if (player.distanceToSqr(cx, cy, cz) <= radiusSq) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.BLINDNESS,
                        BLINDNESS_DURATION_TICKS,
                        0,
                        true,
                        true,
                        true
                ));
            }
        }
    }
}
