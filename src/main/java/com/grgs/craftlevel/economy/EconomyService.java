package com.grgs.craftlevel.economy;

import com.grgs.craftlevel.capability.IPlayerEmeraldBank;
import com.grgs.craftlevel.capability.IPlayerEmeraldWallet;
import com.grgs.craftlevel.capability.IVillagerEconomy;
import com.grgs.craftlevel.capability.PlayerEmeraldBankProvider;
import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.capability.VillagerEconomyProvider;
import com.grgs.craftlevel.config.EconomyParser;
import com.grgs.craftlevel.villager.VillagerTradeRollCache;
import com.grgs.craftlevel.villager.VillagerTradeService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Inicializa e consulta saldos de economia (wallet no player, villager no villager). */
public final class EconomyService {

    private EconomyService() {}

    public static void ensurePlayerEconomy(Player player) {
        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(wallet -> {
            if (!wallet.isInitialized()) {
                wallet.setBalance(EconomyParser.playerStartingBalance);
                wallet.setInitialized(true);
            }
        });
        player.getCapability(PlayerEmeraldBankProvider.EMERALD_BANK).ifPresent(bank -> {
            if (!bank.isInitialized()) {
                bank.setBalance(0L);
                bank.setInitialized(true);
            }
        });
    }

    public static void ensureVillagerEconomy(AbstractVillager villager) {
        String professionId = professionId(villager);
        int starting = EconomyParser.startingBalanceForProfession(professionId);
        long gameTime = villager.level().getGameTime();

        villager.getCapability(VillagerEconomyProvider.VILLAGER_ECONOMY).ifPresent(economy -> {
            String storedProfession = economy.getProfessionId();
            boolean wasInitialized = economy.isInitialized();
            economy.ensureInitialized(professionId, starting, gameTime);
            if (wasInitialized && !storedProfession.equals(professionId)) {
                VillagerTradeService.clearApplied(villager.getUUID());
            }
        });
    }

    public static void refreshVillagerIfDue(AbstractVillager villager) {
        villager.getCapability(VillagerEconomyProvider.VILLAGER_ECONOMY).ifPresent(economy -> {
            if (!economy.isInitialized()) {
                return;
            }

            String professionId = professionId(villager);
            long gameTime = villager.level().getGameTime();
            int refreshTicks = EconomyParser.refreshTicksForProfession(professionId);
            if (gameTime - economy.getLastRefreshTick() < refreshTicks) {
                return;
            }

            economy.resetBalance(EconomyParser.startingBalanceForProfession(professionId));
            economy.clearTradeUses();
            VillagerTradeRollCache.clear(villager.getUUID());
            economy.setLastRefreshTick(gameTime);
            VillagerTradeService.clearApplied(villager.getUUID());
            VillagerTradeService.applyTrades(villager);
        });
    }

    public static int getVillagerBalance(AbstractVillager villager) {
        ensureVillagerEconomy(villager);
        return villager.getCapability(VillagerEconomyProvider.VILLAGER_ECONOMY)
                .map(IVillagerEconomy::getBalance)
                .orElse(EconomyParser.startingBalanceForProfession(professionId(villager)));
    }

    public static IVillagerEconomy getVillagerEconomy(AbstractVillager villager) {
        ensureVillagerEconomy(villager);
        return villager.getCapability(VillagerEconomyProvider.VILLAGER_ECONOMY).orElse(null);
    }

    public static void onEconomyConfigReload(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            AABB bounds = new AABB(
                    -3.0E7, level.getMinBuildHeight(), -3.0E7,
                    3.0E7, level.getMaxBuildHeight(), 3.0E7
            );
            for (AbstractVillager villager : level.getEntitiesOfClass(AbstractVillager.class, bounds)) {
                String professionId = professionId(villager);
                villager.getCapability(VillagerEconomyProvider.VILLAGER_ECONOMY)
                        .ifPresent(economy -> {
                            if (economy.isInitialized()) {
                                economy.applyEconomyConfig(professionId);
                                VillagerTradeService.applyTrades(villager);
                            }
                        });
            }
        }
    }

    public static long getWalletBalance(Player player) {
        return player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET)
                .map(IPlayerEmeraldWallet::getBalance)
                .orElse(0L);
    }

    public static long getBankBalance(Player player) {
        return player.getCapability(PlayerEmeraldBankProvider.EMERALD_BANK)
                .map(IPlayerEmeraldBank::getBalance)
                .orElse(0L);
    }

    public static String professionId(AbstractVillager villager) {
        if (villager instanceof WanderingTrader) {
            return "minecraft:wandering_trader";
        }
        if (villager instanceof Villager typed) {
            return typed.level().registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION)
                    .getKey(typed.getVillagerData().getProfession())
                    .toString();
        }
        return "minecraft:none";
    }

    public static String professionId(Villager villager) {
        return professionId((AbstractVillager) villager);
    }
}
