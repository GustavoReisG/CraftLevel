package com.grgs.craftlevel.villager;

/** Definição imutável de um trade em villager.json. */
public record VillagerTradeDef(
        int limit,
        VillagerTradeSlotDef input1,
        VillagerTradeSlotDef input2,
        VillagerTradeSlotDef output
) {}
