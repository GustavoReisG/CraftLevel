package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.network.NetherRushDashPacket;
import com.grgs.craftlevel.network.PacketHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frozen Rush: mesmo dash do Nether Rush, sem explosão. Congela a IA dos mobs
 * num raio de 3 blocos no início, no meio e no fim (lvl 3: +1 pulso).
 * CD e distância iguais ao Nether Rush. Freeze: 3s.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class FrozenRushSkill {

    public static final int FREEZE_SECONDS = 3;
    public static final int FREEZE_TICKS = FREEZE_SECONDS * 20;
    public static final int RADIUS_BLOCKS = 3;
    private static final double STUCK_THRESHOLD = 0.12D;
    private static final int STUCK_GRACE_TICKS = 8;
    private static final int STUCK_CONFIRM_TICKS = 3;
    private static final int SERVER_TIMEOUT_BUFFER = 20;

    private static final class Session {
        final Vec3 start;
        final double dashBlocks;
        final int pulseCount;
        final int maxTicks;
        final boolean hadNoGravity;
        int elapsedTicks;
        int pulsesDone;
        int consecutiveStuck;
        Vec3 lastPos;

        Session(int skillLevel, Vec3 start, boolean hadNoGravity) {
            this.start = start;
            this.dashBlocks = dashBlocks(skillLevel);
            this.pulseCount = pulseCount(skillLevel);
            this.maxTicks = dashTicks(skillLevel) + SERVER_TIMEOUT_BUFFER;
            this.hadNoGravity = hadNoGravity;
            this.lastPos = start;
        }
    }

    private static final class FrozenMob {
        final UUID mobUuid;
        final ResourceKey<Level> dimension;
        final boolean previousNoAi;
        long untilGameTime;

        FrozenMob(UUID mobUuid, ResourceKey<Level> dimension, boolean previousNoAi, long untilGameTime) {
            this.mobUuid = mobUuid;
            this.dimension = dimension;
            this.previousNoAi = previousNoAi;
            this.untilGameTime = untilGameTime;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, FrozenMob> FROZEN = new ConcurrentHashMap<>();

    private FrozenRushSkill() {}

    public static int dashBlocks(int level) {
        return NetherRushSkill.dashBlocks(level);
    }

    /** Lvl 1–2: início + meio + fim. Lvl 3: +1 pulso. */
    public static int pulseCount(int level) {
        return NetherRushSkill.explosionCount(level) + 1;
    }

    public static int dashTicks(int level) {
        return NetherRushSkill.dashTicks(level);
    }

    public static int cooldownTicks(int level) {
        return NetherRushSkill.cooldownTicks(level);
    }

    public static int cooldownSeconds(int level) {
        return NetherRushSkill.cooldownSeconds(level);
    }

    public static int freezeSeconds() {
        return FREEZE_SECONDS;
    }

    public static int radiusBlocks() {
        return RADIUS_BLOCKS;
    }

    public static void activate(ServerPlayer player, int level) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            look = player.getForward();
        }
        look = look.normalize();

        Session session = new Session(level, player.position(), player.isNoGravity());
        player.setNoGravity(true);
        SESSIONS.put(player.getUUID(), session);
        ActiveSkillTracker.put(
                player,
                SkillCatalog.FROZEN_RUSH,
                player.serverLevel().getGameTime() + session.maxTicks + FREEZE_TICKS
        );

        PacketHandler.sendToPlayer(
                player,
                new NetherRushDashPacket(look.x, look.y, look.z, dashTicks(level))
        );

        ServerLevel world = player.serverLevel();
        world.playSound(
                null,
                player.blockPosition(),
                SoundEvents.PLAYER_HURT_FREEZE,
                SoundSource.PLAYERS,
                0.7F,
                1.35F
        );
        pulse(player, session);
    }

    public static boolean isDashing(Player player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }

        Vec3 now = player.position();
        double step = now.distanceTo(session.lastPos);
        double traveled = now.distanceTo(session.start);
        session.elapsedTicks++;

        spawnTrail(player);
        triggerPulses(player, session, traveled);

        if (session.elapsedTicks >= STUCK_GRACE_TICKS && step < STUCK_THRESHOLD) {
            session.consecutiveStuck++;
        } else {
            session.consecutiveStuck = 0;
        }

        boolean stuck = session.consecutiveStuck >= STUCK_CONFIRM_TICKS;
        boolean timeUp = session.elapsedTicks >= session.maxTicks;
        boolean reached = traveled >= session.dashBlocks - 0.2D;
        if (stuck || timeUp || reached) {
            if (session.pulsesDone < session.pulseCount) {
                pulse(player, session);
                session.pulsesDone = session.pulseCount;
            }
            endDash(player);
            return;
        }

        session.lastPos = now;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endDash(player);
        }
        FROZEN.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endDash(player);
        } else {
            SESSIONS.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        SESSIONS.remove(event.getOriginal().getUUID());
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, FrozenMob>> it = FROZEN.entrySet().iterator();
        while (it.hasNext()) {
            FrozenMob freeze = it.next().getValue();
            ServerLevel level = server.getLevel(freeze.dimension);
            Entity entity = level == null ? null : level.getEntity(freeze.mobUuid);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                it.remove();
                continue;
            }
            long now = level.getGameTime();
            if (now >= freeze.untilGameTime) {
                mob.setNoAi(freeze.previousNoAi);
                it.remove();
                continue;
            }
            if (!mob.isNoAi()) {
                mob.setNoAi(true);
            }
            if (mob.getTarget() != null) {
                mob.setTarget(null);
            }
        }
    }

    private static void triggerPulses(ServerPlayer player, Session session, double traveled) {
        if (session.pulsesDone >= session.pulseCount) {
            return;
        }
        double interval = session.dashBlocks / Math.max(1, session.pulseCount - 1);
        if (traveled >= interval * session.pulsesDone - 0.2D) {
            pulse(player, session);
        }
    }

    private static void pulse(ServerPlayer player, Session session) {
        session.pulsesDone++;
        ServerLevel world = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + 0.5D;
        double z = player.getZ();

        world.playSound(
                null,
                player.blockPosition(),
                SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS,
                0.85F,
                0.55F
        );
        world.sendParticles(
                ParticleTypes.SNOWFLAKE,
                x,
                y,
                z,
                24,
                0.7D,
                0.5D,
                0.7D,
                0.02D
        );
        world.sendParticles(
                ParticleTypes.CLOUD,
                x,
                y,
                z,
                8,
                0.4D,
                0.3D,
                0.4D,
                0.01D
        );

        double radiusSq = (double) RADIUS_BLOCKS * RADIUS_BLOCKS;
        AABB area = player.getBoundingBox().inflate(RADIUS_BLOCKS);
        long until = world.getGameTime() + FREEZE_TICKS;
        for (Mob mob : world.getEntitiesOfClass(
                Mob.class,
                area,
                target -> target.isAlive() && target.distanceToSqr(player) <= radiusSq
        )) {
            freezeMob(world, mob, until);
        }
    }

    private static void freezeMob(ServerLevel world, Mob mob, long until) {
        FrozenMob existing = FROZEN.get(mob.getUUID());
        if (existing != null) {
            existing.untilGameTime = Math.max(existing.untilGameTime, until);
            mob.setNoAi(true);
            mob.setTarget(null);
            return;
        }
        FROZEN.put(
                mob.getUUID(),
                new FrozenMob(mob.getUUID(), world.dimension(), mob.isNoAi(), until)
        );
        mob.setNoAi(true);
        mob.setTarget(null);
    }

    private static void spawnTrail(ServerPlayer player) {
        player.serverLevel().sendParticles(
                ParticleTypes.SNOWFLAKE,
                player.getX(),
                player.getY() + 0.6D,
                player.getZ(),
                6,
                0.2D,
                0.25D,
                0.2D,
                0.01D
        );
    }

    private static void endDash(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        player.setNoGravity(session.hadNoGravity);
    }
}
