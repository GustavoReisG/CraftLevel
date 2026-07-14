package com.grgs.craftlevel.villager;

/** Roll cacheado para um trade (3 slots). */
public record TradeRollState(
        ResolvedTradeSlot input1,
        ResolvedTradeSlot input2,
        ResolvedTradeSlot output
) {}
