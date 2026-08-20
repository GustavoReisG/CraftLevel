package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncActiveSkillsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Rastreia efeitos ativos de skills (expira em gameTime) e sincroniza com o cliente. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class ActiveSkillTracker {

    private static final Map<UUID, Map<String, Long>> EFFECTS = new ConcurrentHashMap<>();

    private ActiveSkillTracker() {}

    public static void put(ServerPlayer player, String skillId, long expireAtGameTime) {
        EFFECTS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>())
                .put(skillId, expireAtGameTime);
        sync(player);
    }

    public static void remove(ServerPlayer player, String skillId) {
        Map<String, Long> map = EFFECTS.get(player.getUUID());
        if (map == null) {
            return;
        }
        map.remove(skillId);
        if (map.isEmpty()) {
            EFFECTS.remove(player.getUUID());
        }
        sync(player);
    }

    public static void clear(UUID playerId) {
        EFFECTS.remove(playerId);
    }

    public static void clearAndSync(ServerPlayer player) {
        EFFECTS.remove(player.getUUID());
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        Map<String, Long> map = EFFECTS.get(player.getUUID());
        long now = player.serverLevel().getGameTime();
        Map<String, Long> live = new HashMap<>();
        if (map != null) {
            map.entrySet().removeIf(e -> e.getValue() <= now);
            live.putAll(map);
            if (map.isEmpty()) {
                EFFECTS.remove(player.getUUID());
            }
        }
        PacketHandler.sendToPlayer(player, new SyncActiveSkillsPacket(live));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        Map<String, Long> map = EFFECTS.get(player.getUUID());
        if (map == null || map.isEmpty()) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        boolean changed = map.entrySet().removeIf(e -> e.getValue() <= now);
        if (map.isEmpty()) {
            EFFECTS.remove(player.getUUID());
        }
        if (changed) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        clear(event.getOriginal().getUUID());
        clear(event.getEntity().getUUID());
    }
}
