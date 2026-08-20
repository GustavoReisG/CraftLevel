package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frozen Mind: retira a IA do mob do Mob Focus por 3 / 4 / 5s. CD: 90s.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class FrozenMindSkill {

    public static final int COOLDOWN_TICKS = 90 * 20;

    private static final class Freeze {
        final UUID mobUuid;
        final boolean previousNoAi;
        final long untilGameTime;

        Freeze(UUID mobUuid, boolean previousNoAi, long untilGameTime) {
            this.mobUuid = mobUuid;
            this.previousNoAi = previousNoAi;
            this.untilGameTime = untilGameTime;
        }
    }

    /** player UUID → freeze ativo. */
    private static final Map<UUID, Freeze> FREEZES = new ConcurrentHashMap<>();

    private FrozenMindSkill() {}

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

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    /**
     * @return {@code false} se não houver mob válido no focus (não inicia CD).
     */
    public static boolean activate(ServerPlayer player, int level, int targetEntityId) {
        if (targetEntityId < 0) {
            return false;
        }
        Entity entity = player.serverLevel().getEntity(targetEntityId);
        if (!(entity instanceof Mob mob) || !mob.isAlive()) {
            return false;
        }

        // Libera freeze anterior deste player, se houver.
        clearFreeze(player);

        boolean previousNoAi = mob.isNoAi();
        long until = player.serverLevel().getGameTime() + durationTicks(level);
        mob.setNoAi(true);
        mob.setTarget(null);
        FREEZES.put(player.getUUID(), new Freeze(mob.getUUID(), previousNoAi, until));
        ActiveSkillTracker.put(player, SkillCatalog.FROZEN_MIND, until);

        player.serverLevel().playSound(
                null,
                mob.blockPosition(),
                SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS,
                0.6F,
                0.55F
        );
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        Freeze freeze = FREEZES.get(player.getUUID());
        if (freeze == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        Entity entity = player.serverLevel().getEntity(freeze.mobUuid);
        if (!(entity instanceof Mob mob) || !mob.isAlive() || now >= freeze.untilGameTime) {
            clearFreeze(player);
        } else {
            // Mantém IA desligada e sem alvo durante o efeito.
            if (!mob.isNoAi()) {
                mob.setNoAi(true);
            }
            if (mob.getTarget() != null) {
                mob.setTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearFreeze(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) {
            clearFreeze(original);
        }
        FREEZES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        Iterator<Map.Entry<UUID, Freeze>> it = FREEZES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Freeze> entry = it.next();
            if (entry.getValue().untilGameTime > now) {
                continue;
            }
            ServerPlayer online = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (online != null) {
                restoreMob(online, entry.getValue());
                ActiveSkillTracker.remove(online, SkillCatalog.FROZEN_MIND);
            }
            it.remove();
        }
    }

    private static void clearFreeze(ServerPlayer player) {
        Freeze freeze = FREEZES.remove(player.getUUID());
        if (freeze != null) {
            restoreMob(player, freeze);
        }
        ActiveSkillTracker.remove(player, SkillCatalog.FROZEN_MIND);
    }

    private static void restoreMob(ServerPlayer player, Freeze freeze) {
        Entity entity = player.serverLevel().getEntity(freeze.mobUuid);
        if (entity instanceof Mob mob && mob.isAlive()) {
            mob.setNoAi(freeze.previousNoAi);
        }
    }
}
