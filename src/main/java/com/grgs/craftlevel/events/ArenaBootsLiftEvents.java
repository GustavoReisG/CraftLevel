package com.grgs.craftlevel.events;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.item.ArenaBootsLift;
import com.grgs.craftlevel.item.ModItems;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ajusta hitbox / eye height em +1px com as botas de arena. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class ArenaBootsLiftEvents {

    private ArenaBootsLiftEvents() {}

    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!ArenaBootsLift.isWearing(player)) {
            return;
        }

        Pose pose = event.getPose();
        if (pose != Pose.STANDING && pose != Pose.CROUCHING) {
            return;
        }

        EntityDimensions old = event.getOldSize();
        event.setNewSize(EntityDimensions.scalable(old.width, old.height + ArenaBootsLift.HEIGHT));
        event.setNewEyeHeight(event.getOldEyeHeight() + ArenaBootsLift.HEIGHT);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.FEET) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        boolean wasArena = event.getFrom().is(ModItems.ARENA_BOOTS.get());
        boolean nowArena = event.getTo().is(ModItems.ARENA_BOOTS.get());
        if (wasArena != nowArena) {
            player.refreshDimensions();
        }
    }
}
