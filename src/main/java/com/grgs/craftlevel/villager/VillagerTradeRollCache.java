package com.grgs.craftlevel.villager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cache em memória de rolls por villager (não persiste em NBT). */
public final class VillagerTradeRollCache {

    private static final Map<UUID, List<TradeRollState>> ROLLS = new ConcurrentHashMap<>();

    private VillagerTradeRollCache() {}

    public static List<TradeRollState> get(UUID entityId) {
        return ROLLS.get(entityId);
    }

    public static void set(UUID entityId, List<TradeRollState> rolls) {
        if (rolls == null || rolls.isEmpty()) {
            ROLLS.remove(entityId);
            return;
        }
        ROLLS.put(entityId, rolls);
    }

    public static void clear(UUID entityId) {
        ROLLS.remove(entityId);
    }

    public static void clearAll() {
        ROLLS.clear();
    }
}
