package com.grgs.craftlevel.block;

import com.grgs.craftlevel.menu.AltarOfDeathMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Efeito de escuridão ao trocar Glory Kills no altar. */
public class AltarOfDeathBlockEntity extends BlockEntity implements MenuProvider {

    public static final double EFFECT_RADIUS = 8.0;
    /** 3 segundos; addEffect refresca a duração, não acumula amplificador. */
    public static final int EXCHANGE_BLINDNESS_TICKS = 60;

    public AltarOfDeathBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALTAR_OF_DEATH.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.craftlevel.altar_of_death");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AltarOfDeathMenu(containerId, inventory, worldPosition);
    }

    public static void applyExchangeDarkness(Level level, BlockPos pos) {
        double radiusSq = EFFECT_RADIUS * EFFECT_RADIUS;
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        var area = new net.minecraft.world.phys.AABB(pos).inflate(EFFECT_RADIUS);

        for (Player nearby : level.getEntitiesOfClass(Player.class, area)) {
            if (nearby.distanceToSqr(cx, cy, cz) <= radiusSq) {
                nearby.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS,
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
