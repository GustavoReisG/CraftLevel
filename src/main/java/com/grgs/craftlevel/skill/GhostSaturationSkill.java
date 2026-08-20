package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Ghost Saturation: mantém saturação (e comida) no máximo. Duração e CD por nível. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class GhostSaturationSkill {

    private record ActiveEffect(long untilGameTime, int foodLevel, float saturation, float exhaustion) {}

    private static final Map<UUID, ActiveEffect> ACTIVE = new ConcurrentHashMap<>();

    private GhostSaturationSkill() {}

    /** Lvl 1: 3s, lvl 2: 4s, lvl 3: 5s. */
    public static int durationSeconds(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 3;
            case 2 -> 4;
            default -> 5;
        };
    }

    public static int durationTicks(int level) {
        return durationSeconds(level) * 20;
    }

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

    public static void activate(ServerPlayer player, int level) {
        FoodData food = player.getFoodData();
        // Se já estiver ativo, não sobrescreve o snapshot original.
        ActiveEffect existing = ACTIVE.get(player.getUUID());
        int savedFood = existing != null ? existing.foodLevel() : food.getFoodLevel();
        float savedSat = existing != null ? existing.saturation() : food.getSaturationLevel();
        float savedExh = existing != null ? existing.exhaustion() : food.getExhaustionLevel();

        long until = player.serverLevel().getGameTime() + durationTicks(level);
        ACTIVE.put(player.getUUID(), new ActiveEffect(until, savedFood, savedSat, savedExh));
        ActiveSkillTracker.put(player, SkillCatalog.GHOST_SATURATION, until);
        applyMaxSaturation(player);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.GENERIC_EAT,
                SoundSource.PLAYERS,
                0.8F,
                1.2F
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        Player player = event.player;
        ActiveEffect effect = ACTIVE.get(player.getUUID());
        if (effect == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now >= effect.untilGameTime()) {
            ACTIVE.remove(player.getUUID());
            restore(player, effect);
            if (player instanceof ServerPlayer serverPlayer) {
                ActiveSkillTracker.remove(serverPlayer, SkillCatalog.GHOST_SATURATION);
            }
            return;
        }
        applyMaxSaturation(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ActiveEffect effect = ACTIVE.remove(player.getUUID());
        if (effect != null) {
            restore(player, effect);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        ACTIVE.remove(event.getOriginal().getUUID());
        ACTIVE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        Iterator<Map.Entry<UUID, ActiveEffect>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveEffect> entry = it.next();
            if (entry.getValue().untilGameTime() > now) {
                continue;
            }
            ServerPlayer online = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (online != null) {
                restore(online, entry.getValue());
            }
            it.remove();
        }
    }

    private static void applyMaxSaturation(Player player) {
        FoodData food = player.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(20.0F);
        food.setExhaustion(0.0F);
    }

    private static void restore(Player player, ActiveEffect effect) {
        FoodData food = player.getFoodData();
        int foodLevel = Math.max(0, Math.min(20, effect.foodLevel()));
        float saturation = Math.max(0.0F, Math.min((float) foodLevel, effect.saturation()));
        food.setFoodLevel(foodLevel);
        food.setSaturation(saturation);
        food.setExhaustion(Math.max(0.0F, effect.exhaustion()));
    }
}
