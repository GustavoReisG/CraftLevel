package com.grgs.craftlevel.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Nether Elevation: aplica Wither V + Levitation II por 5s no mob do Mob Focus. CD: 60s.
 * Dano do Wither é atribuído ao player (scaling de spell_power).
 */
public final class NetherElevationSkill {

    public static final int COOLDOWN_TICKS = 60 * 20;
    public static final int DURATION_SECONDS = 5;
    public static final int DURATION_TICKS = DURATION_SECONDS * 20;
    /** Wither V → amplifier 4. */
    public static final int WITHER_AMPLIFIER = 4;
    /** Levitation II → amplifier 1. */
    public static final int LEVITATION_AMPLIFIER = 1;

    private NetherElevationSkill() {}

    public static int durationSeconds(int level) {
        return DURATION_SECONDS;
    }

    public static int durationTicks(int level) {
        return DURATION_TICKS;
    }

    public static int witherLevel() {
        return WITHER_AMPLIFIER + 1;
    }

    public static int levitationLevel() {
        return LEVITATION_AMPLIFIER + 1;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    /**
     * @return {@code false} se não houver alvo válido no focus (não inicia CD).
     */
    public static boolean activate(ServerPlayer player, int level, int targetEntityId) {
        if (targetEntityId < 0) {
            return false;
        }
        Entity entity = player.serverLevel().getEntity(targetEntityId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || target == player) {
            return false;
        }

        PlayerAttributedEffects.applyWither(player, target, DURATION_TICKS, WITHER_AMPLIFIER);
        target.addEffect(new MobEffectInstance(
                MobEffects.LEVITATION,
                DURATION_TICKS,
                LEVITATION_AMPLIFIER,
                false,
                true,
                true
        ));

        long until = player.serverLevel().getGameTime() + DURATION_TICKS;
        ActiveSkillTracker.put(player, SkillCatalog.NETHER_ELEVATION, until);

        player.serverLevel().playSound(
                null,
                target.blockPosition(),
                SoundEvents.SHULKER_SHOOT,
                SoundSource.PLAYERS,
                0.7F,
                0.85F
        );
        return true;
    }
}
