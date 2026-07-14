package com.grgs.craftlevel.villager;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Resolve slots de trade com tags, whitelist/blacklist e randomização. */
public final class TradeSlotResolver {

    private TradeSlotResolver() {}

    public static ItemStack resolve(VillagerTradeSlotDef slot, RandomSource random) {
        if (slot == null || slot.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<Item> candidates = candidates(slot);
        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Item chosen = candidates.get(random.nextInt(candidates.size()));
        int count = rollCount(slot.min(), slot.max(), random);
        return new ItemStack(chosen, count);
    }

    public static ResolvedTradeSlot resolveCached(VillagerTradeSlotDef slot, RandomSource random) {
        return ResolvedTradeSlot.fromStack(resolve(slot, random));
    }

    public static boolean isResolvable(VillagerTradeSlotDef slot) {
        if (slot == null || slot.isEmpty()) {
            return true;
        }
        return !candidates(slot).isEmpty();
    }

    public static boolean needsRoll(VillagerTradeSlotDef slot) {
        if (slot == null || slot.isEmpty()) {
            return false;
        }
        if (!slot.whitelist().isEmpty()) {
            return true;
        }
        String item = slot.item();
        if (item != null && (item.startsWith("#") || item.contains(":") && !item.startsWith("minecraft:"))) {
            return true;
        }
        if (item != null && item.startsWith("forge:")) {
            return true;
        }
        if (slot.min() != slot.max()) {
            return true;
        }
        List<Item> expanded = TagExpansionCache.expand(item != null ? item : "");
        return expanded.size() > 1;
    }

    private static List<Item> candidates(VillagerTradeSlotDef slot) {
        if (!slot.whitelist().isEmpty()) {
            List<Item> items = new ArrayList<>(TagExpansionCache.expandAll(slot.whitelist()));
            removeBlacklist(items, slot.blacklist());
            return items;
        }

        List<Item> items = new ArrayList<>(TagExpansionCache.expand(slot.item()));
        if (!slot.blacklist().isEmpty()) {
            removeBlacklist(items, slot.blacklist());
        }
        return items;
    }

    private static void removeBlacklist(List<Item> items, List<String> blacklist) {
        if (blacklist.isEmpty()) {
            return;
        }
        Set<Item> blocked = TagExpansionCache.expandAll(blacklist);
        items.removeIf(blocked::contains);
    }

    private static int rollCount(int min, int max, RandomSource random) {
        if (min <= 0 && max <= 0) {
            return 0;
        }
        if (min >= max) {
            return Math.max(min, 1);
        }
        return min + random.nextInt(max - min + 1);
    }
}
