package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mind Fury: hostis no raio enlouquecem e atacam outros hostis (não o player).
 * Raio 3, duração 5s, CD 90s. Lvl 1: máx 3 mobs, lvl 2: máx 4.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class MindFurySkill {

    public static final int RADIUS_BLOCKS = 3;
    public static final int DURATION_SECONDS = 5;
    public static final int DURATION_TICKS = DURATION_SECONDS * 20;
    public static final int COOLDOWN_TICKS = 90 * 20;
    private static final double RETARGET_RANGE = 16.0D;

    private static final class Session {
        final long untilGameTime;
        final Set<UUID> affected = new HashSet<>();

        Session(long untilGameTime) {
            this.untilGameTime = untilGameTime;
        }
    }

    /** mob UUID → sessão ativa (pode haver várias sessões de players diferentes). */
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> FRENZIED_UNTIL_OWNER = new ConcurrentHashMap<>();

    private MindFurySkill() {}

    /** Lvl 1: 3, lvl 2+: 4. */
    public static int maxMobs(int level) {
        return Math.max(1, level) >= 2 ? 4 : 3;
    }

    public static int radiusBlocks() {
        return RADIUS_BLOCKS;
    }

    public static int durationSeconds() {
        return DURATION_SECONDS;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        ServerLevel world = player.serverLevel();
        long until = world.getGameTime() + DURATION_TICKS;
        Session session = new Session(until);

        double radiusSq = (double) RADIUS_BLOCKS * RADIUS_BLOCKS;
        AABB area = player.getBoundingBox().inflate(RADIUS_BLOCKS);
        List<Mob> candidates = world.getEntitiesOfClass(
                Mob.class,
                area,
                mob -> isHostile(mob) && mob.isAlive() && mob.distanceToSqr(player) <= radiusSq
        );
        candidates.sort(Comparator.comparingDouble(m -> m.distanceToSqr(player)));

        int limit = maxMobs(level);
        int taken = 0;
        for (Mob mob : candidates) {
            if (taken >= limit) {
                break;
            }
            session.affected.add(mob.getUUID());
            FRENZIED_UNTIL_OWNER.put(mob.getUUID(), player.getUUID());
            assignHostileTarget(mob, player);
            taken++;
        }

        if (!session.affected.isEmpty()) {
            SESSIONS.put(player.getUUID(), session);
            ActiveSkillTracker.put(player, SkillCatalog.MIND_FURY, until);
        }

        world.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.PLAYERS,
                0.55F,
                1.55F
        );
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        if (!FRENZIED_UNTIL_OWNER.containsKey(mob.getUUID())) {
            return;
        }
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget instanceof Player || newTarget == null || !isHostile(newTarget)) {
            LivingEntity replacement = findHostileTarget(mob, null);
            if (replacement != null) {
                event.setNewTarget(replacement);
            } else if (newTarget instanceof Player) {
                event.setCanceled(true);
            }
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
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (now >= session.untilGameTime) {
            endSession(player);
            return;
        }
        // Reaplica alvos a cada tick para manter a fúria.
        Iterator<UUID> it = session.affected.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            var entity = player.serverLevel().getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                FRENZIED_UNTIL_OWNER.remove(id);
                it.remove();
                continue;
            }
            LivingEntity current = mob.getTarget();
            if (current instanceof Player || current == null || !current.isAlive() || !isHostile(current)) {
                assignHostileTarget(mob, player);
            }
        }
        if (session.affected.isEmpty()) {
            endSession(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endSession(player);
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
        long now = event.getServer().overworld().getGameTime();
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
            if (entry.getValue().untilGameTime <= now) {
                expired.add(entry.getKey());
            }
        }
        for (UUID playerId : expired) {
            ServerPlayer online = event.getServer().getPlayerList().getPlayer(playerId);
            if (online != null) {
                endSession(online);
            } else {
                Session session = SESSIONS.remove(playerId);
                if (session != null) {
                    for (UUID mobId : session.affected) {
                        FRENZIED_UNTIL_OWNER.remove(mobId);
                    }
                }
            }
        }
    }

    private static void endSession(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            for (UUID mobId : session.affected) {
                FRENZIED_UNTIL_OWNER.remove(mobId);
                var entity = player.serverLevel().getEntity(mobId);
                if (entity instanceof Mob mob && mob.getTarget() instanceof Player) {
                    mob.setTarget(null);
                }
            }
        }
        ActiveSkillTracker.remove(player, SkillCatalog.MIND_FURY);
    }

    private static void assignHostileTarget(Mob mob, Player caster) {
        LivingEntity target = findHostileTarget(mob, caster);
        if (target != null) {
            mob.setTarget(target);
        } else if (mob.getTarget() instanceof Player) {
            mob.setTarget(null);
        }
    }

    private static LivingEntity findHostileTarget(Mob mob, Player caster) {
        AABB area = mob.getBoundingBox().inflate(RETARGET_RANGE);
        double rangeSq = RETARGET_RANGE * RETARGET_RANGE;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Mob other : mob.level().getEntitiesOfClass(Mob.class, area, candidate ->
                candidate != mob
                        && candidate.isAlive()
                        && isHostile(candidate)
                        && candidate.distanceToSqr(mob) <= rangeSq)) {
            double dist = other.distanceToSqr(mob);
            if (dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }

    private static boolean isHostile(LivingEntity entity) {
        return entity instanceof Mob && entity instanceof Enemy;
    }
}
