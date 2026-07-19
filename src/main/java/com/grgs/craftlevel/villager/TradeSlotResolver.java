package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.CraftLevel;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Resolve slots de trade com tags, whitelist/blacklist, NBT inline e randomização. */
public final class TradeSlotResolver {

    private TradeSlotResolver() {}

    public static ItemStack resolve(VillagerTradeSlotDef slot, RandomSource random) {
        if (slot == null || slot.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<StackCandidate> candidates = candidates(slot);
        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }

        StackCandidate chosen = candidates.get(random.nextInt(candidates.size()));
        int count = rollCount(slot.min(), slot.max(), random);
        ItemStack stack = new ItemStack(chosen.item(), count);
        String nbt = chosen.hasNbt() ? chosen.nbt() : slot.nbt();
        applyNbt(stack, nbt, random);
        return stack;
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
        if (NbtWildcardResolver.containsWildcard(slot.item())
                || NbtWildcardResolver.containsWildcard(slot.nbt())) {
            return true;
        }
        for (String entry : slot.whitelist()) {
            if (NbtWildcardResolver.containsWildcard(entry)) {
                return true;
            }
        }
        ItemSpec.Parsed parsed = ItemSpec.parse(slot.item());
        String item = parsed.id();
        if (item.startsWith("#") || (item.contains(":") && !item.startsWith("minecraft:"))) {
            return true;
        }
        if (item.startsWith("forge:")) {
            return true;
        }
        if (slot.min() != slot.max()) {
            return true;
        }
        return TagExpansionCache.expand(item).size() > 1;
    }

    /** Aplica SNBT e resolve curingas {@code "*"} com o random do villager. */
    public static void applyNbt(ItemStack stack, String nbt, RandomSource random) {
        if (stack.isEmpty() || nbt == null || nbt.isBlank()) {
            return;
        }
        try {
            CompoundTag tag = TagParser.parseTag(nbt);
            NbtWildcardResolver.resolve(tag, random);
            stack.setTag(tag);
        } catch (CommandSyntaxException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Invalid trade slot NBT: {}", nbt, e);
        }
    }

    private static List<StackCandidate> candidates(VillagerTradeSlotDef slot) {
        if (!slot.whitelist().isEmpty()) {
            List<StackCandidate> items = expandEntries(slot.whitelist());
            removeBlacklist(items, slot.blacklist());
            return items;
        }

        ItemSpec.Parsed parsed = ItemSpec.parse(slot.item());
        List<StackCandidate> items = new ArrayList<>();
        for (Item item : TagExpansionCache.expand(parsed.id())) {
            items.add(new StackCandidate(item, parsed.nbt()));
        }
        if (!slot.blacklist().isEmpty()) {
            removeBlacklist(items, slot.blacklist());
        }
        return items;
    }

    private static List<StackCandidate> expandEntries(List<String> entries) {
        List<StackCandidate> result = new ArrayList<>();
        for (String entry : entries) {
            ItemSpec.Parsed parsed = ItemSpec.parse(entry);
            for (Item item : TagExpansionCache.expand(parsed.id())) {
                result.add(new StackCandidate(item, parsed.nbt()));
            }
        }
        return result;
    }

    private static void removeBlacklist(List<StackCandidate> items, List<String> blacklist) {
        if (blacklist.isEmpty()) {
            return;
        }
        Set<Item> blocked = TagExpansionCache.expandAll(blacklist);
        items.removeIf(candidate -> blocked.contains(candidate.item()));
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

    private record StackCandidate(Item item, String nbt) {
        boolean hasNbt() {
            return nbt != null && !nbt.isBlank();
        }
    }
}
