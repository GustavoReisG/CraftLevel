package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.glory.GloryKillsService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blood Rush (passiva): ao matar o alvo do Mob Focus, +% velocidade e ataque por 10s.
 * CD 2 min (lvl 1–2) / 90s (lvl 3). HUD mostra o ícone enquanto o CD roda.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class BloodRushSkill {

    public static final int BUFF_DURATION_SECONDS = 10;
    public static final int BUFF_DURATION_TICKS = BUFF_DURATION_SECONDS * 20;

    private static final UUID SPEED_MODIFIER_ID =
            UUID.fromString("d1a4e8f2-6c3b-4f9a-8e2d-7b5c0a1f4e6d");
    private static final String SPEED_MODIFIER_NAME = "craftlevel_blood_rush";

    private static final Map<UUID, Long> BUFF_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> BUFF_BONUS = new ConcurrentHashMap<>();

    private BloodRushSkill() {}

    /** Lvl 1: 20%, lvl 2+: 25%. */
    public static int bonusPercent(int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.max(1, level) >= 2 ? 25 : 20;
    }

    public static float bonusFraction(int level) {
        return bonusPercent(level) / 100.0F;
    }

    /** Lvl 1–2: 120s, lvl 3+: 90s. */
    public static int cooldownTicks(int level) {
        return Math.max(1, level) >= 3 ? 90 * 20 : 120 * 20;
    }

    public static int cooldownSeconds(int level) {
        return cooldownTicks(level) / 20;
    }

    public static int skillLevel(Player player) {
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getSkillLevel(SkillCatalog.BLOOD_RUSH))
                .orElse(0);
    }

    public static boolean isReady(ServerPlayer player) {
        if (skillLevel(player) <= 0) {
            return false;
        }
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getCooldownReadyAt(SkillCatalog.BLOOD_RUSH)
                        <= player.serverLevel().getGameTime())
                .orElse(false);
    }

    public static boolean isBuffActive(Player player) {
        Long until = BUFF_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        return player.level().getGameTime() < until;
    }

    public static float damageMultiplier(Player player) {
        if (!isBuffActive(player)) {
            return 1.0F;
        }
        return 1.0F + BUFF_BONUS.getOrDefault(player.getUUID(), 0.0F);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob)) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (!MobFocusTracker.isFocused(player, victim)) {
            return;
        }
        MobFocusTracker.clear(player);
        if (!isReady(player)) {
            return;
        }
        trigger(player);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer attacker)) {
            return;
        }
        if (attacker == event.getEntity()) {
            return;
        }
        float mult = damageMultiplier(attacker);
        if (mult > 1.0F) {
            event.setAmount(event.getAmount() * mult);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        refreshBuff(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            long readyAt = glory.getCooldownReadyAt(SkillCatalog.BLOOD_RUSH);
            long now = player.serverLevel().getGameTime();
            if (readyAt > now && glory.getSkillLevel(SkillCatalog.BLOOD_RUSH) > 0) {
                ActiveSkillTracker.put(player, SkillCatalog.BLOOD_RUSH, readyAt);
            }
        });
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearBuff(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) {
            clearBuff(original);
        }
    }

    private static void trigger(ServerPlayer player) {
        int level = skillLevel(player);
        if (level <= 0) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        long buffUntil = now + BUFF_DURATION_TICKS;
        long readyAt = now + cooldownTicks(level);
        float bonus = bonusFraction(level);

        BUFF_UNTIL.put(player.getUUID(), buffUntil);
        BUFF_BONUS.put(player.getUUID(), bonus);
        applySpeed(player, bonus);

        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory ->
                glory.setCooldownReadyAt(SkillCatalog.BLOOD_RUSH, readyAt));
        // HUD: ícone enquanto o CD roda (não só durante o buff).
        ActiveSkillTracker.put(player, SkillCatalog.BLOOD_RUSH, readyAt);
        GloryKillsService.sync(player);

        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS,
                0.65F,
                1.55F
        );
    }

    private static void refreshBuff(ServerPlayer player) {
        Long until = BUFF_UNTIL.get(player.getUUID());
        if (until == null) {
            clearSpeed(player);
            return;
        }
        if (player.serverLevel().getGameTime() >= until) {
            clearBuff(player);
            return;
        }
        float bonus = BUFF_BONUS.getOrDefault(player.getUUID(), 0.0F);
        applySpeed(player, bonus);
    }

    private static void applySpeed(ServerPlayer player, float bonus) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (speed.getModifier(SPEED_MODIFIER_ID) != null) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
        if (bonus <= 0.0F) {
            return;
        }
        speed.addTransientModifier(new AttributeModifier(
                SPEED_MODIFIER_ID,
                SPEED_MODIFIER_NAME,
                bonus,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    private static void clearSpeed(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    private static void clearBuff(ServerPlayer player) {
        BUFF_UNTIL.remove(player.getUUID());
        BUFF_BONUS.remove(player.getUUID());
        clearSpeed(player);
    }
}
