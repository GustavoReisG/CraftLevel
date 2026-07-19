package com.grgs.craftlevel.villager;

import java.util.List;

/** Definição imutável de um slot de trade (input1, input2 ou output). */
public record VillagerTradeSlotDef(
        String item,
        int min,
        int max,
        List<String> whitelist,
        List<String> blacklist,
        /** SNBT aplicado ao ItemStack na resolução (ex.: livros encantados). */
        String nbt
) {
    public VillagerTradeSlotDef(
            String item,
            int min,
            int max,
            List<String> whitelist,
            List<String> blacklist
    ) {
        this(item, min, max, whitelist, blacklist, "");
    }

    public boolean isEmpty() {
        return item == null || item.isBlank();
    }

    public boolean hasNbt() {
        return nbt != null && !nbt.isBlank();
    }
}
