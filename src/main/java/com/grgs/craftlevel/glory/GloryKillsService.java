package com.grgs.craftlevel.glory;

import com.grgs.craftlevel.capability.IPlayerGlory;
import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.GloryKillsParser;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
import com.grgs.craftlevel.skill.SkillService;
import com.grgs.craftlevel.util.DebugLog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Crédito de Glory Kills e sync para o cliente. */
public final class GloryKillsService {

    private GloryKillsService() {}

    public static void rewardKill(Player player, Mob mob) {
        if (player.level().isClientSide()) {
            return;
        }
        if (!(mob instanceof Enemy) || mob instanceof AbstractVillager) {
            return;
        }

        int playerLevel = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL)
                .map(cap -> Math.max(1, cap.getLevel()))
                .orElse(1);
        int mobLevel = mob.getCapability(MobLevelProvider.MOB_LEVEL)
                .map(cap -> Math.max(0, cap.getLevel()))
                .orElse(0);
        if (mobLevel <= 0) {
            return;
        }
        if (mobLevel < playerLevel - GloryKillsParser.minLevelGap) {
            return;
        }

        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            glory.setGloryKills(glory.getGloryKills() + 1);
            if (player instanceof ServerPlayer serverPlayer) {
                sync(serverPlayer);
            }
            DebugLog.info(
                    "Glory Kill +1 (playerLvl={} mobLvl={} gap={}) → {}",
                    playerLevel,
                    mobLevel,
                    GloryKillsParser.minLevelGap,
                    glory.getGloryKills()
            );
        });
    }

    public static void sync(ServerPlayer player) {
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory ->
                PacketHandler.sendToPlayer(player, createSyncPacket(player, glory)));
    }

    private static SyncPlayerGloryPacket createSyncPacket(ServerPlayer player, IPlayerGlory glory) {
        long now = player.serverLevel().getGameTime();
        Map<String, Integer> remaining = new HashMap<>();
        glory.getCooldownReadyAt().forEach((id, readyAt) -> {
            int ticks = SkillService.remainingCooldownTicks(glory, id, now);
            if (ticks > 0) {
                remaining.put(id, ticks);
            }
        });
        return new SyncPlayerGloryPacket(
                glory.getGloryKills(),
                glory.getSkillPoints(),
                glory.getSkillLevels(),
                remaining,
                glory.getSpellHotbar()
        );
    }
}
