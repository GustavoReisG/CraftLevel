package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncSoulRenderMarkPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
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
 * Soul Render: próximo hit melee marca o mob (outline vermelho estilo flecha espectral).
 * Ataques do player no marcado causam +5/10/15% de dano. Marca até morte ou unload.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class SoulRenderSkill {

    public static final int COOLDOWN_TICKS = 90 * 20;
    private static final String MARK_TEAM = "cl_soul_render";

    private static final class State {
        boolean armed;
        int skillLevel;
        UUID markedUuid;

        State(int skillLevel) {
            this.armed = true;
            this.skillLevel = skillLevel;
        }
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private SoulRenderSkill() {}

    /** Lvl 1: 5%, lvl 2: 10%, lvl 3: 15%. */
    public static int bonusPercent(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 5;
            case 2 -> 10;
            default -> 15;
        };
    }

    public static float damageMultiplier(int level) {
        return 1.0F + bonusPercent(level) / 100.0F;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        clearMark(player, true);
        STATES.put(player.getUUID(), new State(Math.max(1, level)));
        syncMark(player, -1);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.TRIDENT_RETURN,
                SoundSource.PLAYERS,
                0.7F,
                1.35F
        );
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (attacker == event.getEntity()) {
            return;
        }

        State state = STATES.get(attacker.getUUID());
        if (state == null) {
            return;
        }

        if (state.armed && isMelee(source) && event.getEntity() instanceof Mob mob) {
            applyMark(attacker, state, mob);
        }

        if (state.markedUuid != null && state.markedUuid.equals(event.getEntity().getUUID())) {
            float mult = damageMultiplier(state.skillLevel);
            if (mult > 1.0F) {
                event.setAmount(event.getAmount() * mult);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Mob mob) {
            clearMarkVisual(mob);
        }
        clearMarksOnEntity(event.getEntity().getUUID(), event.getEntity().getServer());
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        clearMarkVisual(mob);
        clearMarksOnEntity(mob.getUUID(), mob.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearMark(player, false);
        }
        STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) {
            clearMark(original, false);
        }
        STATES.remove(event.getOriginal().getUUID());
        STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resync(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resync(player);
        }
    }

    private static void applyMark(ServerPlayer player, State state, Mob mob) {
        if (state.markedUuid != null && !state.markedUuid.equals(mob.getUUID())) {
            clearMarkVisual(findEntity(player.server, state.markedUuid));
        }
        state.armed = false;
        state.markedUuid = mob.getUUID();
        applyMarkVisual(mob);
        syncMark(player, mob.getId());
        player.serverLevel().playSound(
                null,
                mob.blockPosition(),
                SoundEvents.ARROW_HIT_PLAYER,
                SoundSource.PLAYERS,
                0.8F,
                0.6F
        );
    }

    private static void clearMark(ServerPlayer player, boolean sync) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (state.markedUuid != null) {
            clearMarkVisual(findEntity(player.server, state.markedUuid));
            state.markedUuid = null;
        }
        if (sync) {
            syncMark(player, -1);
        }
    }

    private static void clearMarksOnEntity(UUID entityUuid, MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (Map.Entry<UUID, State> entry : STATES.entrySet()) {
            State state = entry.getValue();
            if (state.markedUuid == null || !state.markedUuid.equals(entityUuid)) {
                continue;
            }
            state.markedUuid = null;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                syncMark(player, -1);
            }
        }
    }

    private static void applyMarkVisual(Mob mob) {
        MinecraftServer server = mob.getServer();
        if (server == null) {
            return;
        }
        Scoreboard board = server.getScoreboard();
        PlayerTeam team = board.getPlayerTeam(MARK_TEAM);
        if (team == null) {
            team = board.addPlayerTeam(MARK_TEAM);
            team.setColor(ChatFormatting.RED);
            team.setAllowFriendlyFire(true);
            team.setSeeFriendlyInvisibles(false);
        }
        board.addPlayerToTeam(mob.getScoreboardName(), team);
        mob.setGlowingTag(true);
    }

    private static void clearMarkVisual(Entity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        mob.setGlowingTag(false);
        MinecraftServer server = mob.getServer();
        if (server == null) {
            return;
        }
        Scoreboard board = server.getScoreboard();
        PlayerTeam team = board.getPlayersTeam(mob.getScoreboardName());
        if (team != null && MARK_TEAM.equals(team.getName())) {
            board.removePlayerFromTeam(mob.getScoreboardName(), team);
        }
    }

    private static void resync(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.markedUuid == null) {
            syncMark(player, -1);
            return;
        }
        Entity marked = findEntity(player.server, state.markedUuid);
        if (marked instanceof Mob mob && mob.isAlive()) {
            applyMarkVisual(mob);
            syncMark(player, mob.getId());
        } else {
            state.markedUuid = null;
            syncMark(player, -1);
        }
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static void syncMark(ServerPlayer player, int entityId) {
        PacketHandler.sendToPlayer(player, new SyncSoulRenderMarkPacket(entityId));
    }

    /** Só melee direto do player (não projéteis). */
    private static boolean isMelee(DamageSource source) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        return attacker instanceof Player && direct == attacker;
    }
}
