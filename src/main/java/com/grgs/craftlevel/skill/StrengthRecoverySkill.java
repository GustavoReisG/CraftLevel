package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.glory.GloryKillsService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Strength Recovery (passiva): vida baixa concede absorção (% da vida máx.).
 * Lvl 1: &lt;20% vida → +20% absorção. Lvl 2: &lt;25%. Lvl 3: +25% absorção.
 * CD 5 min — ícone no HUD enquanto o CD roda.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class StrengthRecoverySkill {

    public static final int COOLDOWN_TICKS = 5 * 60 * 20;

    private StrengthRecoverySkill() {}

    /** Lvl 1: 20%, lvl 2+: 25%. */
    public static int healthThresholdPercent(int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.max(1, level) >= 2 ? 25 : 20;
    }

    public static float healthThresholdFraction(int level) {
        return healthThresholdPercent(level) / 100.0F;
    }

    /** Lvl 1–2: 20%, lvl 3+: 25%. */
    public static int absorptionPercent(int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.max(1, level) >= 3 ? 25 : 20;
    }

    public static float absorptionFraction(int level) {
        return absorptionPercent(level) / 100.0F;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static int skillLevel(Player player) {
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getSkillLevel(SkillCatalog.STRENGTH_RECOVERY))
                .orElse(0);
    }

    public static boolean isReady(ServerPlayer player) {
        if (skillLevel(player) <= 0) {
            return false;
        }
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getCooldownReadyAt(SkillCatalog.STRENGTH_RECOVERY)
                        <= player.serverLevel().getGameTime())
                .orElse(false);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!isReady(player)) {
            return;
        }
        int level = skillLevel(player);
        if (!isLowHealth(player, level)) {
            return;
        }
        trigger(player, level);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            long readyAt = glory.getCooldownReadyAt(SkillCatalog.STRENGTH_RECOVERY);
            long now = player.serverLevel().getGameTime();
            if (readyAt > now && glory.getSkillLevel(SkillCatalog.STRENGTH_RECOVERY) > 0) {
                ActiveSkillTracker.put(player, SkillCatalog.STRENGTH_RECOVERY, readyAt);
            }
        });
    }

    private static boolean isLowHealth(Player player, int level) {
        float max = player.getMaxHealth();
        if (max <= 0.0F) {
            return false;
        }
        return player.getHealth() / max < healthThresholdFraction(level);
    }

    private static void trigger(ServerPlayer player, int level) {
        float max = player.getMaxHealth();
        float grant = max * absorptionFraction(level);
        if (grant <= 0.0F) {
            return;
        }

        player.setAbsorptionAmount(player.getAbsorptionAmount() + grant);

        long now = player.serverLevel().getGameTime();
        long readyAt = now + COOLDOWN_TICKS;
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory ->
                glory.setCooldownReadyAt(SkillCatalog.STRENGTH_RECOVERY, readyAt));
        ActiveSkillTracker.put(player, SkillCatalog.STRENGTH_RECOVERY, readyAt);
        GloryKillsService.sync(player);

        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                0.55F,
                1.45F
        );
    }
}
