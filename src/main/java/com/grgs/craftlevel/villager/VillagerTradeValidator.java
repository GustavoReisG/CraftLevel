package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.capability.VillagerEconomyProvider;
import com.grgs.craftlevel.economy.EconomyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/** Valida se o villager tem saldo suficiente antes da troca completar. */
public final class VillagerTradeValidator {

    private VillagerTradeValidator() {}

    public static boolean canCompleteTrade(AbstractVillager villager, MerchantOffer offer) {
        if (offer == null) {
            return true;
        }

        EconomyService.ensureVillagerEconomy(villager);

        int emeraldAmount = emeraldValue(offer);
        if (emeraldAmount <= 0 || !playerSellsToVillager(offer)) {
            return true;
        }

        return villager.getCapability(VillagerEconomyProvider.VILLAGER_ECONOMY)
                .map(cap -> cap.getBalance() >= emeraldAmount)
                .orElse(false);
    }

    public static boolean canCompleteTrade(ServerPlayer player, AbstractVillager villager, MerchantOffer offer) {
        return canCompleteTrade(villager, offer);
    }

    static boolean playerBuysFromVillager(MerchantOffer offer) {
        return offer.getCostA().is(Items.EMERALD) || offer.getCostB().is(Items.EMERALD);
    }

    static boolean playerSellsToVillager(MerchantOffer offer) {
        return offer.getResult().is(Items.EMERALD);
    }

    static int emeraldValue(MerchantOffer offer) {
        if (playerBuysFromVillager(offer)) {
            int total = 0;
            if (offer.getCostA().is(Items.EMERALD)) {
                total += offer.getCostA().getCount();
            }
            if (offer.getCostB().is(Items.EMERALD)) {
                total += offer.getCostB().getCount();
            }
            return total;
        }
        if (playerSellsToVillager(offer)) {
            return offer.getResult().getCount();
        }
        return 0;
    }
}
