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
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Darkness Light: player emite luz nível 15 (bloco Light seguindo o player).
 * Duração: 30 / 45 / 60s. CD: 60s.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class DarknessLightSkill {

    public static final int LIGHT_LEVEL = 15;
    public static final int COOLDOWN_TICKS = 60 * 20;

    private static final class Session {
        long untilGameTime;
        ResourceKey<Level> dimension;
        BlockPos lightPos;

        Session(long untilGameTime, ResourceKey<Level> dimension) {
            this.untilGameTime = untilGameTime;
            this.dimension = dimension;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private DarknessLightSkill() {}

    /** Lvl 1: 30s, lvl 2: 45s, lvl 3: 60s. */
    public static int durationSeconds(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 30;
            case 2 -> 45;
            default -> 60;
        };
    }

    public static int durationTicks(int level) {
        return durationSeconds(level) * 20;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        clearLight(player);
        long until = player.serverLevel().getGameTime() + durationTicks(level);
        Session session = new Session(until, player.serverLevel().dimension());
        SESSIONS.put(player.getUUID(), session);
        ActiveSkillTracker.put(player, SkillCatalog.DARKNESS_LIGHT, until);
        updateLight(player, session);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS,
                0.55F,
                1.45F
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
        updateLight(player, session);
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
            clearLight(original);
        }
        SESSIONS.remove(event.getOriginal().getUUID());
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        clearLightAt(player.server.getLevel(session.dimension), session.lightPos);
        session.lightPos = null;
        session.dimension = player.serverLevel().dimension();
        updateLight(player, session);
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
                clearLight(online);
                ActiveSkillTracker.remove(online, SkillCatalog.DARKNESS_LIGHT);
            } else {
                Session session = entry.getValue();
                clearLightAt(event.getServer().getLevel(session.dimension), session.lightPos);
            }
            it.remove();
        }
    }

    private static void endSession(ServerPlayer player) {
        clearLight(player);
        SESSIONS.remove(player.getUUID());
        ActiveSkillTracker.remove(player, SkillCatalog.DARKNESS_LIGHT);
    }

    private static void updateLight(ServerPlayer player, Session session) {
        BlockPos target = findLightPos(player);
        if (target == null) {
            return;
        }
        if (target.equals(session.lightPos) && session.dimension == player.serverLevel().dimension()) {
            return;
        }
        clearLightAt(player.server.getLevel(session.dimension), session.lightPos);
        ServerLevel level = player.serverLevel();
        if (!canPlaceLight(level, target)) {
            session.lightPos = null;
            session.dimension = level.dimension();
            return;
        }
        level.setBlock(target, lightState(), 3);
        session.lightPos = target.immutable();
        session.dimension = level.dimension();
    }

    private static void clearLight(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        clearLightAt(player.server.getLevel(session.dimension), session.lightPos);
        session.lightPos = null;
    }

    private static void clearLightAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LIGHT)) {
            level.removeBlock(pos, false);
        }
    }

    private static BlockPos findLightPos(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        if (canPlaceLight(player.serverLevel(), feet)) {
            return feet;
        }
        BlockPos above = feet.above();
        if (canPlaceLight(player.serverLevel(), above)) {
            return above;
        }
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        if (canPlaceLight(player.serverLevel(), eyes)) {
            return eyes;
        }
        return null;
    }

    private static boolean canPlaceLight(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private static BlockState lightState() {
        return Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL);
    }
}
