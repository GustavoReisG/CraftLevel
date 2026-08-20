package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.glory.GloryKillsService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carnage: janela de 5 min; kills de mobs agressivos geram cargas (+5% dano, máx. 10).
 * Cada carga dura 1 min. CD começa só após expirar a última carga (ou a janela sem cargas).
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class CarnageSkill {

    public static final int HUNT_DURATION_TICKS = 5 * 60 * 20;
    public static final int STACK_DURATION_TICKS = 60 * 20;
    public static final int MAX_STACKS = 10;
    public static final float BONUS_PER_STACK = 0.05F;

    private static final class Session {
        final int skillLevel;
        long huntUntil;
        final List<Long> stackExpireAt = new ArrayList<>();
        boolean cooldownStarted;

        Session(int skillLevel, long huntUntil) {
            this.skillLevel = skillLevel;
            this.huntUntil = huntUntil;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private CarnageSkill() {}

    /** Lvl 1: 3 min, lvl 2: 2.5 min, lvl 3: 2 min. */
    public static int cooldownTicks(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 3 * 60 * 20;
            case 2 -> 150 * 20;
            default -> 2 * 60 * 20;
        };
    }

    public static int cooldownSeconds(int level) {
        return cooldownTicks(level) / 20;
    }

    public static int huntDurationSeconds() {
        return HUNT_DURATION_TICKS / 20;
    }

    public static int maxStacks() {
        return MAX_STACKS;
    }

    public static int bonusPercentPerStack() {
        return Math.round(BONUS_PER_STACK * 100);
    }

    /**
     * @return {@code false} se já houver sessão ativa (janela/cargas).
     */
    public static boolean activate(ServerPlayer player, int level) {
        Session existing = SESSIONS.get(player.getUUID());
        if (existing != null) {
            pruneStacks(existing, player.serverLevel().getGameTime());
            if (existing.huntUntil > player.serverLevel().getGameTime() || !existing.stackExpireAt.isEmpty()) {
                player.displayClientMessage(Component.translatable("gui.craftlevel.skill.carnage.active"), true);
                return false;
            }
            SESSIONS.remove(player.getUUID());
        }

        long now = player.serverLevel().getGameTime();
        SESSIONS.put(player.getUUID(), new Session(level, now + HUNT_DURATION_TICKS));
        ActiveSkillTracker.put(player, SkillCatalog.CARNAGE, now + HUNT_DURATION_TICKS);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                0.55F,
                1.25F
        );
        return true;
    }

    public static int currentStacks(Player player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return 0;
        }
        pruneStacks(session, player.level().getGameTime());
        return session.stackExpireAt.size();
    }

    public static float damageMultiplier(Player player) {
        int stacks = currentStacks(player);
        if (stacks <= 0) {
            return 1.0F;
        }
        return 1.0F + stacks * BONUS_PER_STACK;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
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
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer dead) {
            clearSession(dead.getUUID());
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        Session session = SESSIONS.get(killer.getUUID());
        if (session == null) {
            return;
        }
        long now = killer.serverLevel().getGameTime();
        pruneStacks(session, now);
        if (now >= session.huntUntil) {
            maybeStartCooldown(killer, session, now);
            return;
        }
        if (session.stackExpireAt.size() >= MAX_STACKS) {
            return;
        }
        session.stackExpireAt.add(now + STACK_DURATION_TICKS);
        refreshTracker(killer, session);
        killer.serverLevel().playSound(
                null,
                killer.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.4F,
                0.7F + session.stackExpireAt.size() * 0.05F
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        pruneStacks(session, now);
        refreshTracker(player, session);
        maybeStartCooldown(player, session, now);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearSession(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        clearSession(event.getOriginal().getUUID());
        clearSession(event.getEntity().getUUID());
    }

    private static void pruneStacks(Session session, long now) {
        session.stackExpireAt.removeIf(expireAt -> expireAt <= now);
    }

    private static void maybeStartCooldown(ServerPlayer player, Session session, long now) {
        if (session.cooldownStarted) {
            return;
        }
        if (now < session.huntUntil) {
            return;
        }
        if (!session.stackExpireAt.isEmpty()) {
            return;
        }
        session.cooldownStarted = true;
        int level = session.skillLevel;
        SESSIONS.remove(player.getUUID());
        ActiveSkillTracker.remove(player, SkillCatalog.CARNAGE);
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            glory.setCooldownReadyAt(SkillCatalog.CARNAGE, now + cooldownTicks(level));
            GloryKillsService.sync(player);
        });
    }

    private static void refreshTracker(ServerPlayer player, Session session) {
        long end = session.huntUntil;
        for (long stackEnd : session.stackExpireAt) {
            end = Math.max(end, stackEnd);
        }
        ActiveSkillTracker.put(player, SkillCatalog.CARNAGE, end);
    }

    private static void clearSession(UUID uuid) {
        SESSIONS.remove(uuid);
    }
}
