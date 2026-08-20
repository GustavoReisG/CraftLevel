package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.glory.GloryKillsService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Last Chance (passiva, 1 lvl): hit mortal deixa o player com 1 HP.
 * CD 10 min — HUD só aparece durante o CD; ao sumir, volta a valer.
 * Cobre void, /kill e evita overlap com totem (salva antes da morte).
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class LastChanceSkill {

    public static final int COOLDOWN_TICKS = 10 * 60 * 20;
    private static final int INVULN_TICKS = 40;

    private LastChanceSkill() {}

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static int skillLevel(Player player) {
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getSkillLevel(SkillCatalog.LAST_CHANCE))
                .orElse(0);
    }

    public static boolean isReady(ServerPlayer player) {
        if (skillLevel(player) <= 0) {
            return false;
        }
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getCooldownReadyAt(SkillCatalog.LAST_CHANCE)
                        <= player.serverLevel().getGameTime())
                .orElse(false);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isReady(player)) {
            return;
        }
        float lethalThreshold = player.getHealth() + player.getAbsorptionAmount();
        if (event.getAmount() < lethalThreshold) {
            return;
        }
        if (!trigger(player)) {
            return;
        }
        // Dano vira (vida atual - 1): sobra 1 HP; zera absorption para não distorcer.
        event.setAmount(Math.max(0.0F, player.getHealth() - 1.0F));
        player.setAbsorptionAmount(0.0F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isReady(player)) {
            return;
        }
        if (!trigger(player)) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(1.0F);
        player.setAbsorptionAmount(0.0F);
        player.hurtTime = 0;
        player.invulnerableTime = INVULN_TICKS;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Reexibe no HUD se ainda estiver em CD após relog.
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            long readyAt = glory.getCooldownReadyAt(SkillCatalog.LAST_CHANCE);
            long now = player.serverLevel().getGameTime();
            if (readyAt > now && glory.getSkillLevel(SkillCatalog.LAST_CHANCE) > 0) {
                ActiveSkillTracker.put(player, SkillCatalog.LAST_CHANCE, readyAt);
            }
        });
    }

    /**
     * @return {@code true} se ativou o save e iniciou o CD.
     */
    private static boolean trigger(ServerPlayer player) {
        if (!isReady(player)) {
            return false;
        }
        long now = player.serverLevel().getGameTime();
        long readyAt = now + COOLDOWN_TICKS;
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory ->
                glory.setCooldownReadyAt(SkillCatalog.LAST_CHANCE, readyAt));
        ActiveSkillTracker.put(player, SkillCatalog.LAST_CHANCE, readyAt);
        player.setHealth(1.0F);
        player.setAbsorptionAmount(0.0F);
        player.invulnerableTime = Math.max(player.invulnerableTime, INVULN_TICKS);
        GloryKillsService.sync(player);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS,
                0.7F,
                1.35F
        );
        return true;
    }
}
