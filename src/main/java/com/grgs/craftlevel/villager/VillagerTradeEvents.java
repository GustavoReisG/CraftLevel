package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerVillagerBalanceProvider;
import com.grgs.craftlevel.economy.EmeraldPaymentService;
import com.grgs.craftlevel.economy.EconomyService;
import com.grgs.craftlevel.economy.MerchantMenuHelper;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncVillagerBalancePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Economia de esmeraldas do villager (saldo por entidade + validação de trade). */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class VillagerTradeEvents {

    private static final Map<UUID, Villager> ACTIVE_TRADE_VILLAGERS = new WeakHashMap<>();

    private VillagerTradeEvents() {}

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ACTIVE_TRADE_VILLAGERS.put(player.getUUID(), villager);
        EconomyService.ensurePlayerEconomy(player);
        EconomyService.trackVillagerEntity(player, villager, event.getLevel().getGameTime());
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

        Villager villager = ACTIVE_TRADE_VILLAGERS.get(player.getUUID());
        if (villager == null) {
            return;
        }

        EconomyService.ensurePlayerEconomy(player);
        EconomyService.trackVillagerEntity(player, villager, player.level().getGameTime());
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
        if (!(event.getAbstractVillager() instanceof Villager villager)) {
            return;
        }

        UUID entityId = villager.getUUID();
        MerchantOffer offer = event.getMerchantOffer();
        EconomyService.ensurePlayerEconomy(player);
        EconomyService.trackVillagerEntity(player, villager, player.level().getGameTime());

        int emeraldAmount = VillagerTradeValidator.emeraldValue(offer);
        if (emeraldAmount > 0) {
            if (VillagerTradeValidator.playerBuysFromVillager(offer)) {
                player.getCapability(PlayerVillagerBalanceProvider.VILLAGER_BALANCE)
                        .ifPresent(cap -> cap.addBalance(entityId, emeraldAmount));
            } else if (VillagerTradeValidator.playerSellsToVillager(offer)) {
                player.getCapability(PlayerVillagerBalanceProvider.VILLAGER_BALANCE)
                        .ifPresent(cap -> cap.trySpendBalance(entityId, emeraldAmount));
            }
        }

        if (player.getServer() != null) {
            player.getServer().execute(() -> {
                if (!(player.containerMenu instanceof MerchantMenu openMenu)) {
                    return;
                }
                MerchantOffer selected = MerchantMenuHelper.getSelectedOffer(openMenu);
                if (selected != null) {
                    EmeraldPaymentService.refillPaymentBuffer(player, openMenu, selected);
                }
            });
        }

        syncBalance(player, villager);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        long gameTime = event.getServer().overworld().getGameTime();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            EconomyService.refreshDueEntities(player, event.getServer(), gameTime);
        }
    }

    private static void syncBalance(ServerPlayer player, Villager villager) {
        int balance = EconomyService.getVillagerBalance(player, villager);
        PacketHandler.sendToPlayer(player, new SyncVillagerBalancePacket(balance));
    }
}
