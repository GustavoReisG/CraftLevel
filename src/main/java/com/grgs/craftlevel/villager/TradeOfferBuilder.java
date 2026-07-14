package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.capability.IVillagerEconomy;
import com.grgs.craftlevel.villager.VillagerTradeRollCache;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Constrói MerchantOffers a partir de definições JSON + rolls do villager. */
public final class TradeOfferBuilder {

    public static final int INFINITE_USES = Integer.MAX_VALUE;

    private TradeOfferBuilder() {}

    public static MerchantOffers build(
            List<VillagerTradeDef> trades,
            IVillagerEconomy economy,
            UUID entityId,
            RandomSource random
    ) {
        MerchantOffers offers = new MerchantOffers();
        List<TradeRollState> rolls = ensureRolls(trades, entityId, random);

        for (int i = 0; i < trades.size(); i++) {
            VillagerTradeDef trade = trades.get(i);
            if (!isValidTrade(trade)) {
                continue;
            }

            TradeRollState roll = rolls.get(i);
            int uses = economy.getTradeUses(i);
            int maxUses = trade.limit() <= 0 ? INFINITE_USES : trade.limit();

            ItemStack costA = roll.input1().toStack();
            ItemStack costB = roll.input2().toStack();
            ItemStack result = roll.output().toStack();

            if (!isValidRoll(trade, costA, costB, result)) {
                continue;
            }

            MerchantOffer offer = new MerchantOffer(
                    costA,
                    costB,
                    result,
                    uses,
                    maxUses,
                    0,
                    0.05f
            );
            offers.add(offer);
        }
        return offers;
    }

    public static List<TradeRollState> rollAll(List<VillagerTradeDef> trades, RandomSource random) {
        List<TradeRollState> rolls = new ArrayList<>(trades.size());
        for (VillagerTradeDef trade : trades) {
            rolls.add(rollTrade(trade, random));
        }
        return rolls;
    }

    private static List<TradeRollState> ensureRolls(
            List<VillagerTradeDef> trades,
            UUID entityId,
            RandomSource random
    ) {
        List<TradeRollState> existing = VillagerTradeRollCache.get(entityId);
        if (existing != null && existing.size() == trades.size()) {
            return existing;
        }

        List<TradeRollState> rolls = rollAll(trades, random);
        VillagerTradeRollCache.set(entityId, rolls);
        return rolls;
    }

    private static TradeRollState rollTrade(VillagerTradeDef trade, RandomSource random) {
        return new TradeRollState(
                TradeSlotResolver.resolveCached(trade.input1(), random),
                TradeSlotResolver.resolveCached(trade.input2(), random),
                TradeSlotResolver.resolveCached(trade.output(), random)
        );
    }

    private static boolean isValidTrade(VillagerTradeDef trade) {
        return TradeSlotResolver.isResolvable(trade.input1())
                && TradeSlotResolver.isResolvable(trade.input2())
                && TradeSlotResolver.isResolvable(trade.output());
    }

    private static boolean isValidRoll(VillagerTradeDef trade, ItemStack costA, ItemStack costB, ItemStack result) {
        if (!slotRollValid(trade.input1(), costA)) {
            return false;
        }
        if (!slotRollValid(trade.input2(), costB)) {
            return false;
        }
        if (!slotRollValid(trade.output(), result)) {
            return false;
        }
        return !costA.isEmpty() || !costB.isEmpty() || !result.isEmpty();
    }

    private static boolean slotRollValid(VillagerTradeSlotDef slot, ItemStack stack) {
        if (slot == null || slot.isEmpty()) {
            return true;
        }
        return !stack.isEmpty();
    }
}
