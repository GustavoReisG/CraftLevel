package com.grgs.craftlevel.menu;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Converte stacks de esmeralda/bloco em packs da wallet. */
final class EmeraldDepositHelper {

    static final int PACKS_PER_BLOCK = 9;

    private EmeraldDepositHelper() {}

    static boolean isDepositable(ItemStack stack) {
        return stack.is(Items.EMERALD) || stack.is(Items.EMERALD_BLOCK);
    }

    static int packValue(ItemStack stack) {
        if (stack.is(Items.EMERALD)) {
            return stack.getCount();
        }
        if (stack.is(Items.EMERALD_BLOCK)) {
            return stack.getCount() * PACKS_PER_BLOCK;
        }
        return 0;
    }
}
