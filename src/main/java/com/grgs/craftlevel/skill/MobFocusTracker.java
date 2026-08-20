package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Focus de mob por jogador (sincronizado do cliente). */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class MobFocusTracker {

    private static final Map<UUID, Integer> FOCUSED = new ConcurrentHashMap<>();

    private MobFocusTracker() {}

    public static void set(ServerPlayer player, int entityId) {
        FOCUSED.put(player.getUUID(), entityId);
    }

    public static void clear(ServerPlayer player) {
        FOCUSED.remove(player.getUUID());
    }

    public static void clear(UUID playerId) {
        FOCUSED.remove(playerId);
    }

    public static int get(ServerPlayer player) {
        return FOCUSED.getOrDefault(player.getUUID(), -1);
    }

    public static boolean isFocused(ServerPlayer player, Entity entity) {
        if (entity == null) {
            return false;
        }
        return get(player) == entity.getId();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity().getUUID());
    }
}
