package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Supernova: puxa hostis no raio (4 blocos/s), invulnerabilidade 2s, depois explosão
 * estilo creeper normal sem destruir blocos.
 * <ul>
 *   <li>Lvl 1: raio 8, puxada 2s</li>
 *   <li>Lvl 2: raio 12, puxada 2s</li>
 *   <li>Lvl 3: raio 12, puxada 3s</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class SupernovaSkill {

    public static final int COOLDOWN_TICKS = 3 * 60 * 20;
    public static final int INVULN_TICKS = 2 * 20;
    public static final float PULL_BLOCKS_PER_SECOND = 4.0F;
    public static final float EXPLOSION_POWER = 3.0F; // creeper normal
    private static final double STOP_DISTANCE = 1.25D;

    private static final class Session {
        final int skillLevel;
        final long startGameTime;
        final long pullUntil;
        final long invulnUntil;
        boolean exploded;

        Session(int skillLevel, long now, int pullTicks) {
            this.skillLevel = skillLevel;
            this.startGameTime = now;
            this.pullUntil = now + pullTicks;
            this.invulnUntil = now + INVULN_TICKS;
            this.exploded = false;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private SupernovaSkill() {}

    /** Lvl 1: 8, lvl 2+: 12. */
    public static int radiusBlocks(int level) {
        return Math.max(1, level) >= 2 ? 12 : 8;
    }

    /** Lvl 1–2: 2s, lvl 3: 3s. */
    public static int pullSeconds(int level) {
        return Math.max(1, level) >= 3 ? 3 : 2;
    }

    public static int pullTicks(int level) {
        return pullSeconds(level) * 20;
    }

    public static int invulnSeconds() {
        return INVULN_TICKS / 20;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        long now = player.serverLevel().getGameTime();
        int pull = pullTicks(level);
        Session session = new Session(level, now, pull);
        SESSIONS.put(player.getUUID(), session);
        ActiveSkillTracker.put(player, SkillCatalog.SUPERNOVA, session.pullUntil);

        ServerLevel levelWorld = player.serverLevel();
        levelWorld.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                0.8F,
                0.65F
        );
        levelWorld.sendParticles(
                ParticleTypes.PORTAL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                40,
                0.6D,
                0.8D,
                0.6D,
                0.4D
        );
    }

    public static boolean isInvulnerable(Player player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return false;
        }
        return player.level().getGameTime() < session.invulnUntil;
    }

    private static boolean isCasterProtected(Player player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isInvulnerable(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (isInvulnerable(player)) {
            event.setCanceled(true);
            return;
        }
        // Protege o caster da própria supernova (explosão pode ocorrer após os 2s de invuln no lvl 3).
        if (isCasterProtected(player) && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
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
        if (now < session.pullUntil) {
            pullEnemies(player, session);
            if ((now - session.startGameTime) % 4L == 0L) {
                player.serverLevel().sendParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        player.getX(),
                        player.getY() + 0.8D,
                        player.getZ(),
                        8,
                        0.35D,
                        0.4D,
                        0.35D,
                        0.02D
                );
            }
            return;
        }

        if (!session.exploded) {
            session.exploded = true;
            detonate(player);
        }
        SESSIONS.remove(player.getUUID());
        ActiveSkillTracker.remove(player, SkillCatalog.SUPERNOVA);
    }

    private static void pullEnemies(ServerPlayer player, Session session) {
        int radius = radiusBlocks(session.skillLevel);
        double radiusSq = (double) radius * radius;
        double speedPerTick = PULL_BLOCKS_PER_SECOND / 20.0D;
        AABB area = player.getBoundingBox().inflate(radius);
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.35D, 0.0D);

        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> isValidTarget(player, target) && target.distanceToSqr(player) <= radiusSq
        )) {
            Vec3 toCenter = center.subtract(entity.position());
            double dist = toCenter.length();
            if (dist <= STOP_DISTANCE || dist < 1.0E-4D) {
                entity.setDeltaMovement(Vec3.ZERO);
                entity.hurtMarked = true;
                entity.hasImpulse = true;
                continue;
            }

            Vec3 pull = toCenter.normalize().scale(Math.min(speedPerTick, dist - STOP_DISTANCE * 0.5D));
            // Substitui o movimento (puxão contínuo, sem teleporte).
            entity.setDeltaMovement(pull);
            entity.hurtMarked = true;
            entity.hasImpulse = true;
            entity.fallDistance = 0.0F;
        }
    }

    private static void detonate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                1.2F,
                0.85F
        );
        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
        // Potência de creeper normal; NONE = só entidades, sem blocos.
        level.explode(
                player,
                player.getX(),
                player.getY() + 0.5D,
                player.getZ(),
                EXPLOSION_POWER,
                false,
                Level.ExplosionInteraction.NONE
        );
    }

    private static boolean isValidTarget(ServerPlayer caster, LivingEntity target) {
        return target != caster && target.isAlive() && target instanceof Enemy;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
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
        long now = event.getServer().overworld().getGameTime();
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            Session session = entry.getValue();
            if (session.exploded && session.pullUntil < now - 40L) {
                it.remove();
            }
        }
    }
}
