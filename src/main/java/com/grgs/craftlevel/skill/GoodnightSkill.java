package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Goodnight: dorme no chão, de dia ou de noite, com ou sem mobs.
 * Ao completar o sono: vai para o amanhecer, para a chuva, não marca spawn.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class GoodnightSkill {

    public static final int COOLDOWN_TICKS = 3 * 60 * 20;

    private static final Map<UUID, Boolean> SLEEPING = new ConcurrentHashMap<>();

    private GoodnightSkill() {}

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static boolean isSkillSleeping(Player player) {
        return SLEEPING.containsKey(player.getUUID());
    }

    /**
     * @return {@code false} se o player não puder dormir (já dormindo / morto).
     */
    public static boolean activate(ServerPlayer player, int level) {
        if (!player.isAlive() || player.isSleeping() || player.isSpectator()) {
            return false;
        }

        BlockPos pos = player.blockPosition();
        player.startSleeping(pos);
        SLEEPING.put(player.getUUID(), Boolean.TRUE);

        long readyAt = player.serverLevel().getGameTime() + COOLDOWN_TICKS;
        ActiveSkillTracker.put(player, SkillCatalog.GOODNIGHT, readyAt);

        player.serverLevel().playSound(
                null,
                pos,
                SoundEvents.WOOL_PLACE,
                SoundSource.PLAYERS,
                0.7F,
                0.85F
        );
        return true;
    }

    @SubscribeEvent
    public static void onSleepingLocationCheck(SleepingLocationCheckEvent event) {
        if (event.getEntity() instanceof Player player && isSkillSleeping(player)) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void onSleepingTimeCheck(SleepingTimeCheckEvent event) {
        if (isSkillSleeping(event.getEntity())) {
            event.setResult(Event.Result.ALLOW);
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
        if (!isSkillSleeping(player)) {
            return;
        }
        if (!player.isSleeping()) {
            clear(player);
            return;
        }
        if (player.isSleepingLongEnough()) {
            complete(player);
        }
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isSkillSleeping(player)) {
            if (!player.isSleepingLongEnough()) {
                clear(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            long readyAt = glory.getCooldownReadyAt(SkillCatalog.GOODNIGHT);
            long now = player.serverLevel().getGameTime();
            if (readyAt > now && glory.getSkillLevel(SkillCatalog.GOODNIGHT) > 0) {
                ActiveSkillTracker.put(player, SkillCatalog.GOODNIGHT, readyAt);
            }
        });
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (isSkillSleeping(player) && player.isSleeping()) {
                player.stopSleepInBed(true, true);
            }
            clear(player);
        }
    }

    private static void complete(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        skipToMorning(level);
        stopRain(level);
        SLEEPING.remove(player.getUUID());
        if (player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }
    }

    private static void skipToMorning(ServerLevel level) {
        long dayTime = level.getDayTime();
        long nextMorning = dayTime + 24000L - dayTime % 24000L;
        level.setDayTime(ForgeEventFactory.onSleepFinished(level, nextMorning, dayTime));
    }

    private static void stopRain(ServerLevel level) {
        level.setWeatherParameters(0, 0, false, false);
    }

    private static void clear(ServerPlayer player) {
        SLEEPING.remove(player.getUUID());
    }
}
