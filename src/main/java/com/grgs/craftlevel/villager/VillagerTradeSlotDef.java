package com.grgs.craftlevel.villager;

import java.util.List;

/** Definição imutável de um slot de trade (input1, input2 ou output). */
public record VillagerTradeSlotDef(
        String item,
        int min,
        int max,
        List<String> whitelist,
        List<String> blacklist
) {
    public boolean isEmpty() {
        return item == null || item.isBlank();
    }
}
