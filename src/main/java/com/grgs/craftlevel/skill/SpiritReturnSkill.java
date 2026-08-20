package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Spirit Return: marca o bloco atual com aura luminosa (5 min).
 * Agachar teleporta de volta. Some ao morrer ou ao marcar de novo.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class SpiritReturnSkill {

    public static final int AURA_DURATION_TICKS = 5 * 60 * 20;

    private record Mark(
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            long expireAtGameTime
    ) {}

    private static final Map<UUID, Mark> MARKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> WAS_SNEAKING = new ConcurrentHashMap<>();

    private SpiritReturnSkill() {}

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

    public static int auraDurationSeconds() {
        return AURA_DURATION_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        ServerLevel levelWorld = player.serverLevel();
        long expire = levelWorld.getGameTime() + AURA_DURATION_TICKS;
        MARKS.put(
                player.getUUID(),
                new Mark(
                        levelWorld.dimension(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getYRot(),
                        player.getXRot(),
                        expire
                )
        );
        WAS_SNEAKING.put(player.getUUID(), player.isShiftKeyDown());

        levelWorld.playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                1.0F,
                1.2F
        );
        spawnAuraBurst(levelWorld, player.getX(), player.getY(), player.getZ());
        ActiveSkillTracker.put(player, SkillCatalog.SPIRIT_RETURN, expire);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        Mark mark = MARKS.get(player.getUUID());
        if (mark == null) {
            WAS_SNEAKING.remove(player.getUUID());
            return;
        }

        ServerLevel markLevel = player.server.getLevel(mark.dimension());
        long now = markLevel != null ? markLevel.getGameTime() : player.serverLevel().getGameTime();
        if (now >= mark.expireAtGameTime()) {
            clearMark(player);
            return;
        }

        if (markLevel != null && player.tickCount % 4 == 0) {
            spawnAuraIdle(markLevel, mark.x(), mark.y(), mark.z());
        }

        boolean sneaking = player.isShiftKeyDown();
        boolean was = WAS_SNEAKING.getOrDefault(player.getUUID(), false);
        WAS_SNEAKING.put(player.getUUID(), sneaking);
        if (sneaking && !was) {
            tryReturn(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearMark(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearMark(player);
        } else {
            clearMark(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        clearMark(event.getOriginal().getUUID());
        clearMark(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long overworldNow = event.getServer().overworld().getGameTime();
        Iterator<Map.Entry<UUID, Mark>> it = MARKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Mark> entry = it.next();
            Mark mark = entry.getValue();
            ServerLevel markLevel = event.getServer().getLevel(mark.dimension());
            long now = markLevel != null ? markLevel.getGameTime() : overworldNow;
            if (now >= mark.expireAtGameTime()) {
                WAS_SNEAKING.remove(entry.getKey());
                it.remove();
            }
        }
    }

    private static void tryReturn(ServerPlayer player) {
        Mark mark = MARKS.remove(player.getUUID());
        WAS_SNEAKING.remove(player.getUUID());
        if (mark == null) {
            return;
        }

        ServerLevel targetLevel = player.server.getLevel(mark.dimension());
        if (targetLevel == null) {
            return;
        }

        Vec3 dest = safeTeleportPos(targetLevel, mark.x(), mark.y(), mark.z());
        playReturnFx(player.serverLevel(), player.getX(), player.getY(), player.getZ());

        if (player.serverLevel().dimension() == mark.dimension()) {
            player.teleportTo(dest.x, dest.y, dest.z);
            player.setYRot(mark.yRot());
            player.setXRot(mark.xRot());
        } else {
            player.changeDimension(targetLevel, new ITeleporter() {
                @Override
                public PortalInfo getPortalInfo(
                        net.minecraft.world.entity.Entity entity,
                        ServerLevel destLevel,
                        Function<ServerLevel, PortalInfo> defaultPortalInfo
                ) {
                    return new PortalInfo(dest, Vec3.ZERO, mark.yRot(), mark.xRot());
                }
            });
        }

        playReturnFx(targetLevel, dest.x, dest.y, dest.z);
        targetLevel.playSound(
                null,
                BlockPos.containing(dest),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.9F,
                1.15F
        );
        ActiveSkillTracker.remove(player, SkillCatalog.SPIRIT_RETURN);
    }

    private static Vec3 safeTeleportPos(ServerLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                net.minecraft.util.Mth.floor(x),
                net.minecraft.util.Mth.floor(y),
                net.minecraft.util.Mth.floor(z)
        );
        for (int i = 0; i < 8; i++) {
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                return new Vec3(x, pos.getY(), z);
            }
            pos.move(0, 1, 0);
        }
        return new Vec3(x, y, z);
    }

    private static void clearMark(UUID uuid) {
        MARKS.remove(uuid);
        WAS_SNEAKING.remove(uuid);
    }

    private static void clearMark(ServerPlayer player) {
        clearMark(player.getUUID());
        ActiveSkillTracker.remove(player, SkillCatalog.SPIRIT_RETURN);
    }

    private static void spawnAuraBurst(ServerLevel level, double x, double y, double z) {
        for (int i = 0; i < 24; i++) {
            double oy = i * 0.12;
            level.sendParticles(ParticleTypes.END_ROD, x, y + oy, z, 1, 0.12, 0.02, 0.12, 0.01);
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.2, z, 12, 0.25, 0.4, 0.25, 0.01);
    }

    private static void spawnAuraIdle(ServerLevel level, double x, double y, double z) {
        // Coluna luminosa estilo rastilho de flecha espectral / end rod.
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.1, z, 2, 0.08, 0.55, 0.08, 0.0);
        if (level.getRandom().nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.GLOW, x, y + 1.0, z, 1, 0.15, 0.35, 0.15, 0.0);
        }
    }

    private static void playReturnFx(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 40, 0.4, 0.8, 0.4, 0.2);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.5, z, 16, 0.3, 0.6, 0.3, 0.02);
    }
}
