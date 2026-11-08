package com.grgs.craftlevel.altar;

import net.minecraft.world.item.ItemStack;

/** Oferta da aba Trade: item vendido e custo em Glory. */
public record AltarTradeOffer(ItemStack result, int gloryCost) {

    public AltarTradeOffer {
        result = result.copy();
        gloryCost = Math.max(0, gloryCost);
    }

    public ItemStack resultCopy() {
        return result.copy();
    }
}
