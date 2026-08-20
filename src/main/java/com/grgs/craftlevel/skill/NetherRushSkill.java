package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.network.NetherRushDashPacket;
import com.grgs.craftlevel.network.PacketHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
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
 * Nether Rush: dash na direção da mira (não teleporta). Explosões no caminho
 * e fogo nos hostis. Lvl 1: 6 blocos / 2 explosões / CD 2 min.
 * Lvl 2: mesmo dash, CD 90s. Lvl 3: 9 blocos / 3 explosões.
 * <p>
 * O impulso roda no cliente — o servidor só acompanha a posição para as explosões.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class NetherRushSkill {

    public static final int FIRE_SECONDS = 8;
    public static final float EXPLOSION_POWER = 3.0F;
    public static final double IGNITE_RADIUS = 4.0D;
    public static final double SPEED_PER_TICK = 0.75D;
    private static final double STUCK_THRESHOLD = 0.12D;
    private static final int STUCK_GRACE_TICKS = 8;
    private static final int STUCK_CONFIRM_TICKS = 3;
    private static final int SERVER_TIMEOUT_BUFFER = 20;

    private static final class Session {
        final Vec3 start;
        final double dashBlocks;
        final int explosionCount;
        final int maxTicks;
        final boolean hadNoGravity;
        int elapsedTicks;
        int explosionsDone;
        int consecutiveStuck;
        Vec3 lastPos;

        Session(int skillLevel, Vec3 start, boolean hadNoGravity) {
            this.start = start;
            this.dashBlocks = dashBlocks(skillLevel);
            this.explosionCount = explosionCount(skillLevel);
            this.maxTicks = dashTicks(skillLevel) + SERVER_TIMEOUT_BUFFER;
            this.hadNoGravity = hadNoGravity;
            this.lastPos = start;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private NetherRushSkill() {}

    /** Lvl 1–2: 6, lvl 3: 9. */
    public static int dashBlocks(int level) {
        return Math.max(1, level) >= 3 ? 9 : 6;
    }

    /** Lvl 1–2: 2, lvl 3: 3. */
    public static int explosionCount(int level) {
        return Math.max(1, level) >= 3 ? 3 : 2;
    }

    public static int dashTicks(int level) {
        return Math.max(1, (int) Math.ceil(dashBlocks(level) / SPEED_PER_TICK));
    }

    /** Lvl 1: 2 min, lvl 2+: 90s. */
    public static int cooldownTicks(int level) {
        return Math.max(1, level) >= 2 ? 90 * 20 : 2 * 60 * 20;
    }

    public static int cooldownSeconds(int level) {
        return cooldownTicks(level) / 20;
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
                SkillCatalog.NETHER_RUSH,
                player.serverLevel().getGameTime() + session.maxTicks
        );

        PacketHandler.sendToPlayer(
                player,
                new NetherRushDashPacket(look.x, look.y, look.z, dashTicks(level))
        );

        ServerLevel world = player.serverLevel();
        world.playSound(
                null,
                player.blockPosition(),
                SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS,
                0.9F,
                0.75F
        );
        world.sendParticles(
                ParticleTypes.FLAME,
                player.getX(),
                player.getY() + 0.9D,
                player.getZ(),
                24,
                0.35D,
                0.4D,
                0.35D,
                0.08D
        );
    }

    public static boolean isDashing(Player player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && isDashing(player)
                && event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && isDashing(player)
                && event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
        }
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
        triggerExplosions(player, session, traveled);

        if (session.elapsedTicks >= STUCK_GRACE_TICKS && step < STUCK_THRESHOLD) {
            session.consecutiveStuck++;
        } else {
            session.consecutiveStuck = 0;
        }

        boolean stuck = session.consecutiveStuck >= STUCK_CONFIRM_TICKS;
        boolean timeUp = session.elapsedTicks >= session.maxTicks;
        boolean reached = traveled >= session.dashBlocks - 0.2D;
        if (stuck || timeUp || reached) {
            if (session.explosionsDone < session.explosionCount) {
                detonate(player);
                session.explosionsDone = session.explosionCount;
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

    private static void triggerExplosions(ServerPlayer player, Session session, double traveled) {
        if (session.explosionsDone >= session.explosionCount) {
            return;
        }
        double interval = session.dashBlocks / session.explosionCount;
        if (traveled >= interval * (session.explosionsDone + 1) - 0.2D) {
            detonate(player);
            session.explosionsDone++;
        }
    }

    private static void detonate(ServerPlayer player) {
        ServerLevel world = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + 0.5D;
        double z = player.getZ();

        world.playSound(
                null,
                player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                1.0F,
                0.9F
        );
        world.sendParticles(
                ParticleTypes.EXPLOSION,
                x,
                y,
                z,
                4,
                0.4D,
                0.3D,
                0.4D,
                0.0D
        );
        world.sendParticles(
                ParticleTypes.FLAME,
                x,
                y,
                z,
                18,
                0.5D,
                0.4D,
                0.5D,
                0.06D
        );
        world.explode(
                player,
                x,
                y,
                z,
                EXPLOSION_POWER,
                false,
                Level.ExplosionInteraction.NONE
        );

        double radiusSq = IGNITE_RADIUS * IGNITE_RADIUS;
        AABB area = player.getBoundingBox().inflate(IGNITE_RADIUS);
        for (LivingEntity entity : world.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> isValidTarget(player, target) && target.distanceToSqr(player) <= radiusSq
        )) {
            entity.setSecondsOnFire(FIRE_SECONDS);
        }
    }

    private static void spawnTrail(ServerPlayer player) {
        player.serverLevel().sendParticles(
                ParticleTypes.SMALL_FLAME,
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
        ActiveSkillTracker.remove(player, SkillCatalog.NETHER_RUSH);
    }

    private static boolean isValidTarget(ServerPlayer caster, LivingEntity target) {
        return target != caster && target.isAlive() && target instanceof Enemy;
    }
}
