package com.grgs.craftlevel.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Elevação de 1px (1/16 bloco) ao vestir as botas de arena. */
public final class ArenaBootsLift {

    /** 1 pixel de textura = 1/16 de bloco. */
    public static final float HEIGHT = 1.0F / 16.0F;

    private ArenaBootsLift() {}

    public static boolean isWearing(LivingEntity entity) {
        // EntityEvent.Size dispara na construção do player, antes do inventário existir.
        if (entity instanceof Player player && player.getInventory() == null) {
            return false;
        }
        return entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ARENA_BOOTS.get());
    }
}
