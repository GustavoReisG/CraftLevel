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
 * Protective Shockwave: onda sem dano que repele hostis (força = nível) e
 * concede invencibilidade por 3s.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class ProtectiveShockwaveSkill {

    public static final int COOLDOWN_TICKS = 90 * 20;
    public static final int INVULN_TICKS = 3 * 20;
    public static final int RADIUS_BLOCKS = 8;
    /** Multiplicador estilo Knockback enchant (0.5 por nível). */
    private static final double KNOCKBACK_PER_LEVEL = 0.5D;

    private static final Map<UUID, Long> INVULN_UNTIL = new ConcurrentHashMap<>();

    private ProtectiveShockwaveSkill() {}

    public static int repulsionLevel(int skillLevel) {
        return Math.max(1, Math.min(3, skillLevel));
    }

    public static int radiusBlocks() {
        return RADIUS_BLOCKS;
    }

    public static int invulnSeconds() {
        return INVULN_TICKS / 20;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        int repulsion = repulsionLevel(level);
        long until = player.serverLevel().getGameTime() + INVULN_TICKS;
        INVULN_UNTIL.put(player.getUUID(), until);
        ActiveSkillTracker.put(player, SkillCatalog.PROTECTIVE_SHOCKWAVE, until);

        ServerLevel world = player.serverLevel();
        world.playSound(
                null,
                player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                0.7F,
                1.35F
        );
        world.sendParticles(
                ParticleTypes.EXPLOSION,
                player.getX(),
                player.getY() + 0.8D,
                player.getZ(),
                6,
                0.4D,
                0.3D,
                0.4D,
                0.0D
        );
        world.sendParticles(
                ParticleTypes.FLASH,
                player.getX(),
                player.getY() + 0.9D,
                player.getZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );

        double radius = RADIUS_BLOCKS;
        double radiusSq = radius * radius;
        double strength = KNOCKBACK_PER_LEVEL * repulsion;
        AABB area = player.getBoundingBox().inflate(radius);

        for (LivingEntity entity : world.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> isValidTarget(player, target) && target.distanceToSqr(player) <= radiusSq
        )) {
            // knockback(strength, x, z): vetor do alvo → origem; impulso no sentido contrário.
            double dx = player.getX() - entity.getX();
            double dz = player.getZ() - entity.getZ();
            if (dx * dx + dz * dz < 1.0E-4D) {
                Vec3 look = player.getLookAngle();
                dx = -look.x;
                dz = -look.z;
            }
            entity.knockback(strength, dx, dz);
            entity.hurtMarked = true;
            entity.hasImpulse = true;
            entity.fallDistance = 0.0F;
        }
    }

    public static boolean isInvulnerable(Player player) {
        Long until = INVULN_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        return player.level().getGameTime() < until;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isInvulnerable(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isInvulnerable(player)) {
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
        Long until = INVULN_UNTIL.get(player.getUUID());
        if (until == null) {
            return;
        }
        if (player.serverLevel().getGameTime() >= until) {
            INVULN_UNTIL.remove(player.getUUID());
            ActiveSkillTracker.remove(player, SkillCatalog.PROTECTIVE_SHOCKWAVE);
        }
    }

    private static boolean isValidTarget(ServerPlayer caster, LivingEntity target) {
        return target != caster && target.isAlive() && target instanceof Enemy;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        INVULN_UNTIL.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        INVULN_UNTIL.remove(event.getOriginal().getUUID());
        INVULN_UNTIL.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = INVULN_UNTIL.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= now) {
                it.remove();
            }
        }
    }
}
