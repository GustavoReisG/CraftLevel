package com.grgs.craftlevel.economy;

import com.grgs.craftlevel.capability.IPlayerEmeraldWallet;
import com.grgs.craftlevel.capability.IPlayerVillagerBalance;
import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.capability.PlayerVillagerBalanceProvider;
import com.grgs.craftlevel.config.EconomyParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Inicializa e consulta saldos de economia no player. */
public final class EconomyService {

    private static final int REFRESH_CHECK_INTERVAL = 20;
    private static final int CLEANUP_INTERVAL = 6000;

    private EconomyService() {}

    public static void ensurePlayerEconomy(Player player) {
        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(wallet -> {
            if (!wallet.isInitialized()) {
                wallet.setBalance(EconomyParser.playerStartingBalance);
                wallet.setInitialized(true);
            }
        });

        player.getCapability(PlayerVillagerBalanceProvider.VILLAGER_BALANCE).ifPresent(balance -> {
            if (!balance.isInitialized()) {
                balance.setInitialized(true);
            }
        });
    }

    public static void trackVillagerEntity(Player player, Villager villager, long gameTime) {
        UUID entityId = villager.getUUID();
        String professionId = professionId(villager);
        int starting = EconomyParser.startingBalanceForProfession(professionId);

        player.getCapability(PlayerVillagerBalanceProvider.VILLAGER_BALANCE).ifPresent(balance -> {
            balance.ensureEntity(entityId, professionId, starting, gameTime);
        });
    }

    public static void refreshDueEntities(ServerPlayer player, MinecraftServer server, long gameTime) {
        if (gameTime % REFRESH_CHECK_INTERVAL != 0) {
            return;
        }

        player.getCapability(PlayerVillagerBalanceProvider.VILLAGER_BALANCE).ifPresent(balance -> {
            List<UUID> tracked = new ArrayList<>();
            balance.trackedEntities().forEach(tracked::add);

            for (UUID entityId : tracked) {
                if (gameTime % CLEANUP_INTERVAL == 0 && findEntity(server, entityId) == null) {
                    balance.pruneEntity(entityId);
                    continue;
                }

                String professionId = balance.getProfession(entityId);
                long lastRefresh = balance.getLastRefreshTick(entityId);
                int refreshTicks = EconomyParser.refreshTicksForProfession(professionId);
                if (gameTime - lastRefresh >= refreshTicks) {
                    balance.resetEntity(entityId, EconomyParser.startingBalanceForProfession(professionId));
                    balance.setLastRefreshTick(entityId, gameTime);
                }
            }
        });
    }

    public static int getVillagerBalance(Player player, Villager villager) {
        UUID entityId = villager.getUUID();
        String professionId = professionId(villager);
        int starting = EconomyParser.startingBalanceForProfession(professionId);

        return player.getCapability(PlayerVillagerBalanceProvider.VILLAGER_BALANCE)
                .map(cap -> {
                    cap.ensureEntity(entityId, professionId, starting, player.level().getGameTime());
                    return cap.getBalance(entityId);
                })
                .orElse(starting);
    }

    public static void onEconomyConfigReload(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.getCapability(PlayerVillagerBalanceProvider.VILLAGER_BALANCE)
                    .ifPresent(IPlayerVillagerBalance::applyEconomyConfig);
        }
    }

    public static long getWalletBalance(Player player) {
        return player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET)
                .map(IPlayerEmeraldWallet::getBalance)
                .orElse(0L);
    }

    public static String professionId(Villager villager) {
        return villager.level().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION)
                .getKey(villager.getVillagerData().getProfession())
                .toString();
    }

    private static Entity findEntity(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
