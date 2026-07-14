package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.capability.IVillagerEconomy;
import com.grgs.craftlevel.economy.EconomyService;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Aplica trades customizados substituindo ofertas vanilla. */
public final class VillagerTradeService {

    private static final Map<UUID, String> APPLIED_PROFESSION = new ConcurrentHashMap<>();

    private VillagerTradeService() {}

    public static void onReload() {
        APPLIED_PROFESSION.clear();
        TagExpansionCache.clear();
        VillagerTradeRollCache.clearAll();
    }

    public static void applyTrades(AbstractVillager villager) {
        IVillagerEconomy economy = EconomyService.getVillagerEconomy(villager);
        if (economy == null) {
            return;
        }

        String professionId = EconomyService.professionId(villager);
        List<VillagerTradeDef> trades = VillagerTradeCatalog.tradesFor(professionId);
        MerchantOffers offers = TradeOfferBuilder.build(trades, economy, villager.getUUID(), villager.getRandom());
        replaceOffers(villager, offers);
        APPLIED_PROFESSION.put(villager.getUUID(), professionId);
    }

    public static void resortTrades(AbstractVillager villager) {
        IVillagerEconomy economy = EconomyService.getVillagerEconomy(villager);
        if (economy == null) {
            return;
        }

        String professionId = EconomyService.professionId(villager);
        List<VillagerTradeDef> trades = VillagerTradeCatalog.tradesFor(professionId);
        List<TradeRollState> rolls = TradeOfferBuilder.rollAll(trades, villager.getRandom());
        VillagerTradeRollCache.set(villager.getUUID(), rolls);
        MerchantOffers offers = TradeOfferBuilder.build(trades, economy, villager.getUUID(), villager.getRandom());
        replaceOffers(villager, offers);
    }

    public static int findTradeIndex(AbstractVillager villager, MerchantOffer selected) {
        if (selected == null) {
            return -1;
        }
        return villager.getOffers().indexOf(selected);
    }

    public static void clearApplied(UUID entityId) {
        APPLIED_PROFESSION.remove(entityId);
    }

    private static void replaceOffers(AbstractVillager villager, MerchantOffers offers) {
        MerchantOffers current = villager.getOffers();
        current.clear();
        current.addAll(offers);
    }
}
