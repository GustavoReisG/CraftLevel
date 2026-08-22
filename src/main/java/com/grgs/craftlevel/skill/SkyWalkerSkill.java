package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sky Walker: por 30s, transforma um 3x3 de blocos atravessáveis abaixo dos pés em
 * {@link Blocks#BARRIER}. Ao andar, recalcula o 3x3 e restaura o que saiu da área.
 * CD 90s. Logout / morte / dimensão / teleporte restaura tudo.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class SkyWalkerSkill {

    public static final int DURATION_SECONDS = 30;
    public static final int DURATION_TICKS = DURATION_SECONDS * 20;
    public static final int COOLDOWN_TICKS = 90 * 20;

    private static final class Session {
        long untilGameTime;
        ResourceKey<Level> dimension;
        /** Y da camada atual da plataforma (só sobe quando o player sobe 1 bloco de fato). */
        int platformY;
        final Map<BlockPos, BlockState> placed = new HashMap<>();

        Session(long untilGameTime, ResourceKey<Level> dimension, int platformY) {
            this.untilGameTime = untilGameTime;
            this.dimension = dimension;
            this.platformY = platformY;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private SkyWalkerSkill() {}

    public static int durationSeconds() {
        return DURATION_SECONDS;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        endSession(player);
        long until = player.serverLevel().getGameTime() + DURATION_TICKS;
        int platformY = player.blockPosition().below().getY();
        Session session = new Session(until, player.serverLevel().dimension(), platformY);
        SESSIONS.put(player.getUUID(), session);
        ActiveSkillTracker.put(player, SkillCatalog.SKY_WALKER, until);
        updatePlatform(player, session);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.WOOL_PLACE,
                SoundSource.PLAYERS,
                0.7F,
                1.25F
        );
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
        if (session.dimension != player.serverLevel().dimension()) {
            endSession(player);
            return;
        }
        updatePlatform(player, session);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endSession(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) {
            endSession(original);
        }
        SESSIONS.remove(event.getOriginal().getUUID());
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endSession(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endSession(player);
        }
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (SESSIONS.containsKey(player.getUUID())) {
                endSession(player);
            }
        }
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
            if (entry.getValue().untilGameTime > now) {
                continue;
            }
            ServerPlayer online = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (online != null) {
                restoreAll(online.server.getLevel(entry.getValue().dimension), entry.getValue());
                ActiveSkillTracker.remove(online, SkillCatalog.SKY_WALKER);
            } else {
                restoreAll(event.getServer().getLevel(entry.getValue().dimension), entry.getValue());
            }
            it.remove();
        }
    }

    private static void endSession(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        restoreAll(player.server.getLevel(session.dimension), session);
        ActiveSkillTracker.remove(player, SkillCatalog.SKY_WALKER);
    }

    private static void updatePlatform(ServerPlayer player, Session session) {
        ServerLevel level = player.serverLevel();
        updatePlatformY(player, session);
        Set<BlockPos> desired = platformPositions(player, session);
        // Restaura o que saiu do 3x3 atual.
        Iterator<Map.Entry<BlockPos, BlockState>> it = session.placed.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, BlockState> entry = it.next();
            if (desired.contains(entry.getKey())) {
                continue;
            }
            restoreAt(level, entry.getKey(), entry.getValue());
            it.remove();
        }
        // Coloca barreiras nos blocos atravessáveis do novo 3x3.
        for (BlockPos pos : desired) {
            if (session.placed.containsKey(pos)) {
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (!isTraversable(current)) {
                continue;
            }
            session.placed.put(pos.immutable(), current);
            level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 3);
        }
        session.dimension = level.dimension();
    }

    /**
     * Sobe a camada só quando o bloco abaixo dos pés fica acima da plataforma atual.
     * Agachar desce 1 bloco (em relação à altura dos pés).
     */
    private static void updatePlatformY(ServerPlayer player, Session session) {
        int feetY = player.blockPosition().getY();
        int underFeetY = feetY - 1;
        if (player.isShiftKeyDown()) {
            int sneakY = feetY - 2;
            if (sneakY < session.platformY) {
                session.platformY = sneakY;
            }
            return;
        }
        if (underFeetY > session.platformY) {
            session.platformY = underFeetY;
        }
    }

    private static Set<BlockPos> platformPositions(ServerPlayer player, Session session) {
        BlockPos feet = player.blockPosition();
        BlockPos center = new BlockPos(feet.getX(), session.platformY, feet.getZ());
        Set<BlockPos> positions = new HashSet<>(9);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                positions.add(center.offset(dx, 0, dz));
            }
        }
        return positions;
    }

    /** Ar, fluidos, plantas e demais blocos que não bloqueiam movimento. */
    private static boolean isTraversable(BlockState state) {
        if (state.is(Blocks.BARRIER)) {
            return false;
        }
        return !state.blocksMotion();
    }

    private static void restoreAll(ServerLevel level, Session session) {
        if (level == null || session.placed.isEmpty()) {
            session.placed.clear();
            return;
        }
        for (Map.Entry<BlockPos, BlockState> entry : session.placed.entrySet()) {
            restoreAt(level, entry.getKey(), entry.getValue());
        }
        session.placed.clear();
    }

    private static void restoreAt(ServerLevel level, BlockPos pos, BlockState original) {
        if (level == null || pos == null || original == null) {
            return;
        }
        level.setBlock(pos, original, 3);
    }
}
