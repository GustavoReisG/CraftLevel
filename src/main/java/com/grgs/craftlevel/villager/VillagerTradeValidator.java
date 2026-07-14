package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.capability.IVillagerEconomy;
import com.grgs.craftlevel.economy.EconomyService;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** Valida se o villager tem saldo suficiente antes da troca completar. */
public final class VillagerTradeValidator {

    private VillagerTradeValidator() {}

    public static boolean canCompleteTrade(AbstractVillager villager, MerchantOffer offer) {
        if (offer == null) {
            return true;
        }
        if (offer.isOutOfStock()) {
            return false;
        }

        EconomyService.ensureVillagerEconomy(villager);

        if (!villagerPaysEmeraldOutput(offer)) {
            return true;
        }

        int payout = emeraldPayoutValue(offer.getResult());
        return EconomyService.getVillagerBalance(villager) >= payout;
    }

    public static boolean canCompleteTrade(net.minecraft.server.level.ServerPlayer player, AbstractVillager villager, MerchantOffer offer) {
        return canCompleteTrade(villager, offer);
    }

    static boolean playerBuysFromVillager(MerchantOffer offer) {
        return isEmeraldCurrency(offer.getCostA()) || isEmeraldCurrency(offer.getCostB());
    }

    static boolean playerSellsToVillager(MerchantOffer offer) {
        return villagerPaysEmeraldOutput(offer);
    }

    static boolean villagerPaysEmeraldOutput(MerchantOffer offer) {
        return isEmeraldCurrency(offer.getResult());
    }

    static boolean isEmeraldCurrency(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Items.EMERALD) || stack.is(Items.EMERALD_BLOCK));
    }

    static int emeraldValue(MerchantOffer offer) {
        if (playerBuysFromVillager(offer)) {
            int total = 0;
            total += emeraldPayoutValue(offer.getCostA());
            total += emeraldPayoutValue(offer.getCostB());
            return total;
        }
        if (playerSellsToVillager(offer)) {
            return emeraldPayoutValue(offer.getResult());
        }
        return 0;
    }

    static int emeraldPayoutValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(Items.EMERALD)) {
            return stack.getCount();
        }
        if (stack.is(Items.EMERALD_BLOCK)) {
            return stack.getCount() * 9;
        }
        return 0;
    }

    static void applyEconomyLocks(MerchantOffers offers, IVillagerEconomy economy) {
        if (offers == null || economy == null) {
            return;
        }

        int balance = economy.getBalance();
        for (MerchantOffer offer : offers) {
            if (!villagerPaysEmeraldOutput(offer)) {
                continue;
            }
            if (balance < emeraldPayoutValue(offer.getResult())) {
                offer.setToOutOfStock();
            }
        }
    }
}
