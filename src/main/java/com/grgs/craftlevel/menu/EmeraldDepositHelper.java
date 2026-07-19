package com.grgs.craftlevel.menu;

import com.grgs.craftlevel.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Converte stacks de esmeralda/bloco em packs da wallet. */
final class EmeraldDepositHelper {

    static final int PACKS_PER_BLOCK = 9;
    static final int PACKS_PER_COMPRESSED_BLOCK = 81;

    private EmeraldDepositHelper() {}

    static boolean isDepositable(ItemStack stack) {
        return stack.is(Items.EMERALD)
                || stack.is(Items.EMERALD_BLOCK)
                || stack.is(ModItems.COMPRESSED_EMERALD_BLOCK.get());
    }

    static int packValue(ItemStack stack) {
        if (stack.is(Items.EMERALD)) {
            return stack.getCount();
        }
        if (stack.is(Items.EMERALD_BLOCK)) {
            return stack.getCount() * PACKS_PER_BLOCK;
        }
        if (stack.is(ModItems.COMPRESSED_EMERALD_BLOCK.get())) {
            return stack.getCount() * PACKS_PER_COMPRESSED_BLOCK;
        }
        return 0;
    }
}
