package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

/** Portable Crafting: abre a crafting table sem bloco. CD: 30s. Max lvl 1. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class PortableCraftingSkill {

    public static final int COOLDOWN_TICKS = 30 * 20;

    private PortableCraftingSkill() {}

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        NetworkHooks.openScreen(
                player,
                new SimpleMenuProvider(
                        (containerId, inventory, viewer) -> new CraftingMenu(containerId, inventory),
                        Component.translatable("container.crafting")
                )
        );
        long readyAt = player.serverLevel().getGameTime() + COOLDOWN_TICKS;
        ActiveSkillTracker.put(player, SkillCatalog.PORTABLE_CRAFTING, readyAt);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.WOODEN_BUTTON_CLICK_ON,
                SoundSource.PLAYERS,
                0.55F,
                1.15F
        );
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            long readyAt = glory.getCooldownReadyAt(SkillCatalog.PORTABLE_CRAFTING);
            long now = player.serverLevel().getGameTime();
            if (readyAt > now && glory.getSkillLevel(SkillCatalog.PORTABLE_CRAFTING) > 0) {
                ActiveSkillTracker.put(player, SkillCatalog.PORTABLE_CRAFTING, readyAt);
            }
        });
    }
}
