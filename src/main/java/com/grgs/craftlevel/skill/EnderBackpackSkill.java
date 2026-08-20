package com.grgs.craftlevel.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.network.NetworkHooks;

/** Ender Backpack: abre o ender chest do player. CD: 30s. Max lvl 1. */
public final class EnderBackpackSkill {

    public static final int COOLDOWN_TICKS = 30 * 20;

    private EnderBackpackSkill() {}

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        NetworkHooks.openScreen(
                player,
                new SimpleMenuProvider(
                        (containerId, inventory, viewer) ->
                                ChestMenu.threeRows(containerId, inventory, viewer.getEnderChestInventory()),
                        Component.translatable("container.enderchest")
                )
        );
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ENDER_CHEST_OPEN,
                SoundSource.PLAYERS,
                0.55F,
                1.1F
        );
    }
}
