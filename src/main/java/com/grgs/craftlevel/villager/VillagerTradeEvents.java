package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.events.CraftLevelEvents;
import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.IVillagerEconomy;
import com.grgs.craftlevel.capability.VillagerEconomyProvider;
import com.grgs.craftlevel.economy.BankPaymentService;
import com.grgs.craftlevel.economy.EmeraldPaymentService;
import com.grgs.craftlevel.economy.EconomyService;
import com.grgs.craftlevel.economy.MerchantMenuHelper;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncMerchantOffersPacket;
import com.grgs.craftlevel.network.SyncVillagerBalancePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Economia de esmeraldas e trades por villager. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class VillagerTradeEvents {

    private static final Map<UUID, AbstractVillager> ACTIVE_TRADE_VILLAGERS = new WeakHashMap<>();

    private VillagerTradeEvents() {}

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getTarget() instanceof AbstractVillager villager)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ACTIVE_TRADE_VILLAGERS.put(player.getUUID(), villager);
        EconomyService.ensurePlayerEconomy(player);
        EconomyService.ensureVillagerEconomy(villager);
        CraftLevelEvents.refreshMobLevel(villager);
        VillagerTradeService.applyTrades(villager);
        syncBalance(player, villager);
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getContainer() instanceof MerchantMenu)) {
            return;
        }

        AbstractVillager villager = ACTIVE_TRADE_VILLAGERS.get(player.getUUID());
        if (villager == null) {
            return;
        }

        EconomyService.ensurePlayerEconomy(player);
        EconomyService.ensureVillagerEconomy(villager);
        CraftLevelEvents.refreshMobLevel(villager);
        VillagerTradeService.applyTrades(villager);
        syncBalance(player, villager);
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ACTIVE_TRADE_VILLAGERS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AbstractVillager villager = event.getAbstractVillager();
        if (villager == null) {
            return;
        }

        MerchantOffer offer = event.getMerchantOffer();
        EconomyService.ensurePlayerEconomy(player);
        EconomyService.ensureVillagerEconomy(villager);

        villager.getCapability(VillagerEconomyProvider.VILLAGER_ECONOMY).ifPresent(economy -> {
            int emeraldAmount = VillagerTradeValidator.emeraldValue(offer);
            boolean tradeRecorded = true;

            if (emeraldAmount > 0) {
                if (VillagerTradeValidator.playerBuysFromVillager(offer)) {
                    economy.addBalance(emeraldAmount);
                } else if (VillagerTradeValidator.playerSellsToVillager(offer)) {
                    tradeRecorded = economy.trySpendBalance(emeraldAmount);
                }
            }

            if (!tradeRecorded) {
                return;
            }

            int tradeIndex = VillagerTradeService.findTradeIndex(villager, offer);
            if (tradeIndex >= 0) {
                economy.incrementTradeUses(tradeIndex);
            }
        });

        if (player.getServer() != null) {
            player.getServer().execute(() -> {
                VillagerTradeService.refreshEconomyLocks(villager);
                if (!(player.containerMenu instanceof MerchantMenu openMenu)) {
                    return;
                }
                MerchantOffer selected = MerchantMenuHelper.getSelectedOffer(openMenu);
                if (selected != null) {
                    EmeraldPaymentService.refillPaymentBuffer(player, openMenu, selected);
                    BankPaymentService.refillPaymentBuffer(player, openMenu, selected);
                }
                PacketHandler.sendToPlayer(
                        player,
                        new SyncMerchantOffersPacket(openMenu.containerId, villager.getOffers())
                );
            });
        }

        syncBalance(player, villager);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!CraftLevelEvents.isWorldReady()) {
            return;
        }
        if (!(event.getEntity() instanceof AbstractVillager villager)) {
            return;
        }
        if (villager.tickCount % 20 != 0) {
            return;
        }
        EconomyService.refreshVillagerIfDue(villager);
        VillagerTradeService.ensureMasterLevel(villager);
    }

    public static void syncAfterTrade(ServerPlayer player, AbstractVillager villager) {
        syncBalance(player, villager);
    }

    private static void syncBalance(ServerPlayer player, AbstractVillager villager) {
        int balance = EconomyService.getVillagerBalance(villager);
        PacketHandler.sendToPlayer(player, new SyncVillagerBalancePacket(balance));
    }
}
