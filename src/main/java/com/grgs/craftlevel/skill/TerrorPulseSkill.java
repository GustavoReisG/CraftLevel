package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Terror Pulse: hostis no raio 3 fogem do player por 10s.
 * CD L1 2 min / L2 90s.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class TerrorPulseSkill {

    public static final int DURATION_TICKS = 10 * 20;
    public static final int RADIUS_BLOCKS = 3;

    private static final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();

    private TerrorPulseSkill() {}

    public static int cooldownTicks(int level) {
        return Math.max(1, level) >= 2 ? 90 * 20 : 2 * 60 * 20;
    }

    public static int cooldownSeconds(int level) {
        return cooldownTicks(level) / 20;
    }

    public static int durationSeconds() {
        return DURATION_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        long until = player.serverLevel().getGameTime() + DURATION_TICKS;
        ACTIVE_UNTIL.put(player.getUUID(), until);
        ActiveSkillTracker.put(player, SkillCatalog.TERROR_PULSE, until);

        ServerLevel world = player.serverLevel();
        world.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                0.45F,
                1.4F
        );
        world.sendParticles(
                ParticleTypes.SCULK_SOUL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                28,
                0.8D,
                0.5D,
                0.8D,
                0.02D
        );
        frightenNearby(player);
    }

    public static boolean isActive(Player player) {
        Long until = ACTIVE_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        Long until = ACTIVE_UNTIL.get(player.getUUID());
        if (until == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (now >= until) {
            ACTIVE_UNTIL.remove(player.getUUID());
            ActiveSkillTracker.remove(player, SkillCatalog.TERROR_PULSE);
            return;
        }
        if ((now % 5L) == 0L) {
            frightenNearby(player);
        }
    }

    private static void frightenNearby(ServerPlayer player) {
        double radius = RADIUS_BLOCKS;
        double radiusSq = radius * radius;
        AABB area = player.getBoundingBox().inflate(radius);
        ServerLevel world = player.serverLevel();

        for (LivingEntity entity : world.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> isValidTarget(player, target) && target.distanceToSqr(player) <= radiusSq
        )) {
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
            }
            Vec3 away = entity.position().subtract(player.position());
            if (away.lengthSqr() < 1.0E-4D) {
                away = player.getLookAngle();
            }
            away = away.normalize().scale(0.45D);
            entity.setDeltaMovement(entity.getDeltaMovement().add(away.x, 0.05D, away.z));
            entity.hurtMarked = true;
            entity.hasImpulse = true;
            if (entity instanceof Mob mob && mob.getNavigation() != null) {
                Vec3 fleeTo = entity.position().add(away.normalize().scale(6.0D));
                mob.getNavigation().moveTo(fleeTo.x, fleeTo.y, fleeTo.z, 1.25D);
            }
        }
    }

    private static boolean isValidTarget(ServerPlayer caster, LivingEntity target) {
        return target != caster && target.isAlive() && target instanceof Enemy;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE_UNTIL.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        ACTIVE_UNTIL.remove(event.getOriginal().getUUID());
        ACTIVE_UNTIL.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = ACTIVE_UNTIL.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (entry.getValue() > now) {
                continue;
            }
            ServerPlayer online = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (online != null) {
                ActiveSkillTracker.remove(online, SkillCatalog.TERROR_PULSE);
            }
            it.remove();
        }
    }
}
