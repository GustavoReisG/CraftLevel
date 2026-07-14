package com.grgs.craftlevel.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** Serialização de MerchantOffers para pacotes de rede. */
public final class MerchantOffersNetwork {

    private MerchantOffersNetwork() {}

    public static void write(FriendlyByteBuf buf, MerchantOffers offers) {
        buf.writeVarInt(offers.size());
        for (MerchantOffer offer : offers) {
            writeOffer(buf, offer);
        }
    }

    public static MerchantOffers read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        MerchantOffers offers = new MerchantOffers();
        for (int i = 0; i < count; i++) {
            offers.add(readOffer(buf));
        }
        return offers;
    }

    private static void writeOffer(FriendlyByteBuf buf, MerchantOffer offer) {
        buf.writeItem(offer.getCostA());
        buf.writeItem(offer.getCostB());
        buf.writeItem(offer.getResult());
        buf.writeInt(offer.getUses());
        buf.writeInt(offer.getMaxUses());
    }

    private static MerchantOffer readOffer(FriendlyByteBuf buf) {
        ItemStack costA = buf.readItem();
        ItemStack costB = buf.readItem();
        ItemStack result = buf.readItem();
        int uses = buf.readInt();
        int maxUses = buf.readInt();
        return new MerchantOffer(costA, costB, result, uses, maxUses, 0, 0.05f);
    }
}
