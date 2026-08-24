package com.grgs.craftlevel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Efeito de escuridão ao trocar Glory Kills no altar. */
public class AltarOfDeathBlockEntity extends BlockEntity {

    public static final double EFFECT_RADIUS = 8.0;
    /** 3 segundos; addEffect refresca a duração, não acumula amplificador. */
    public static final int EXCHANGE_BLINDNESS_TICKS = 60;

    public AltarOfDeathBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALTAR_OF_DEATH.get(), pos, state);
    }

    public static void applyExchangeDarkness(Level level, BlockPos pos) {
        double radiusSq = EFFECT_RADIUS * EFFECT_RADIUS;
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        AABB area = new AABB(pos).inflate(EFFECT_RADIUS);

        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
            if (player.distanceToSqr(cx, cy, cz) <= radiusSq) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.BLINDNESS,
                        EXCHANGE_BLINDNESS_TICKS,
                        0,
                        true,
                        true,
                        true
                ));
            }
        }
    }
}
