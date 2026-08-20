package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Miner Frenzy: +velocidade de mineração por duração.
 * Lvl 1: +30% / 30s — Lvl 2: +40% / 30s — Lvl 3: +40% / 45s. CD: 60s.
 * Haste vanilla sincroniza o cliente; BreakSpeed cobre o restante do bônus.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class MinerFrenzySkill {

    public static final int COOLDOWN_TICKS = 60 * 20;

    private static final class Session {
        final int skillLevel;
        final long untilGameTime;

        Session(int skillLevel, long untilGameTime) {
            this.skillLevel = skillLevel;
            this.untilGameTime = untilGameTime;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private MinerFrenzySkill() {}

    /** Lvl 1: 30%, lvl 2+: 40%. */
    public static int bonusPercent(int level) {
        return Math.max(1, level) >= 2 ? 40 : 30;
    }

    public static float speedMultiplier(int level) {
        return 1.0F + bonusPercent(level) / 100.0F;
    }

    /**
     * Haste vanilla: +20% por (amplifier + 1). Lvl 1 usa Haste I (+20%) e o
     * BreakSpeed completa até +30%. Lvl 2+ usa Haste II (+40%).
     */
    public static int hasteAmplifier(int level) {
        return Math.max(1, level) >= 2 ? 1 : 0;
    }

    public static float extraBreakMultiplier(int level) {
        float fromHaste = 1.0F + (hasteAmplifier(level) + 1) * 0.2F;
        return speedMultiplier(level) / fromHaste;
    }

    /** Lvl 1–2: 30s, lvl 3: 45s. */
    public static int durationSeconds(int level) {
        return Math.max(1, level) >= 3 ? 45 : 30;
    }

    public static int durationTicks(int level) {
        return durationSeconds(level) * 20;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        int skillLevel = Math.max(1, level);
        long until = player.serverLevel().getGameTime() + durationTicks(skillLevel);
        SESSIONS.put(player.getUUID(), new Session(skillLevel, until));
        ActiveSkillTracker.put(player, SkillCatalog.MINER_FRENZY, until);
        applyHaste(player, skillLevel);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.UI_STONECUTTER_TAKE_RESULT,
                SoundSource.PLAYERS,
                0.8F,
                1.25F
        );
    }

    public static boolean isActive(Player player) {
        return activeLevel(player) > 0;
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        int level = activeLevel(event.getEntity());
        if (level <= 0) {
            return;
        }
        float extra = extraBreakMultiplier(level);
        if (extra <= 1.0001F) {
            return;
        }
        event.setNewSpeed(event.getNewSpeed() * extra);
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
        if (player.serverLevel().getGameTime() >= session.untilGameTime) {
            endSession(player);
            return;
        }
        applyHaste(player, session.skillLevel);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endSession(player);
        } else {
            SESSIONS.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        SESSIONS.remove(event.getOriginal().getUUID());
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            ServerPlayer online = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (online == null) {
                continue;
            }
            if (online.serverLevel().getGameTime() < entry.getValue().untilGameTime) {
                continue;
            }
            removeHaste(online);
            ActiveSkillTracker.remove(online, SkillCatalog.MINER_FRENZY);
            it.remove();
        }
    }

    private static void endSession(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
        removeHaste(player);
        ActiveSkillTracker.remove(player, SkillCatalog.MINER_FRENZY);
    }

    private static void applyHaste(ServerPlayer player, int level) {
        player.addEffect(new MobEffectInstance(
                MobEffects.DIG_SPEED,
                40,
                hasteAmplifier(level),
                false,
                false,
                true
        ));
    }

    private static void removeHaste(ServerPlayer player) {
        player.removeEffect(MobEffects.DIG_SPEED);
    }

    private static int activeLevel(Player player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || player.level().getGameTime() >= session.untilGameTime) {
            return 0;
        }
        return session.skillLevel;
    }
}
