package com.grgs.craftlevel.config;

import com.grgs.craftlevel.villager.VillagerTradeDef;
import com.grgs.craftlevel.villager.VillagerTradeSlotDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Migra variantes coloridas (wool, etc.) para tags no parse.
 * Duplicatas idênticas no JSON são preservadas (rolls extras do mesmo trade).
 */
public final class VillagerJsonMigrator {

    private static final List<String> WOOL_COLORS = List.of(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    );

    private VillagerJsonMigrator() {}

    public static List<VillagerTradeDef> collapseParsedTrades(List<VillagerTradeDef> trades) {
        return collapseTrades(trades);
    }

    private static List<VillagerTradeDef> collapseTrades(List<VillagerTradeDef> trades) {
        List<VillagerTradeDef> result = new ArrayList<>(trades.size());
        Set<String> collapsedColorSignatures = new HashSet<>();

        for (VillagerTradeDef trade : trades) {
            VillagerTradeDef collapsed = collapseTrade(trade);
            boolean colorMigrated = wasColorMigrated(trade, collapsed);

            if (colorMigrated) {
                String signature = collapseSignature(collapsed);
                // Várias cores iguais (ex.: white_wool + orange_wool) → um trade com tag
                if (!collapsedColorSignatures.add(signature)) {
                    continue;
                }
            }

            result.add(collapsed);
        }
        return result;
    }

    private static boolean wasColorMigrated(VillagerTradeDef original, VillagerTradeDef collapsed) {
        return !Objects.equals(original.input1().item(), collapsed.input1().item())
                || !Objects.equals(original.input2().item(), collapsed.input2().item())
                || !Objects.equals(original.output().item(), collapsed.output().item());
    }

    private static VillagerTradeDef collapseTrade(VillagerTradeDef trade) {
        return new VillagerTradeDef(
                trade.limit(),
                collapseSlot(trade.input1()),
                collapseSlot(trade.input2()),
                collapseSlot(trade.output())
        );
    }

    private static VillagerTradeSlotDef collapseSlot(VillagerTradeSlotDef slot) {
        String tag = tagForItem(slot.item());
        if (tag == null) {
            return slot;
        }
        return new VillagerTradeSlotDef(
                tag, slot.min(), slot.max(), slot.whitelist(), slot.blacklist(), slot.nbt()
        );
    }

    private static String collapseSignature(VillagerTradeDef trade) {
        return slotSig(trade.input1()) + "|" + slotSig(trade.input2()) + "|" + slotSig(trade.output()) + "|" + trade.limit();
    }

    private static String slotSig(VillagerTradeSlotDef slot) {
        String itemKey = tagForItem(slot.item()) != null ? tagForItem(slot.item()) : slot.item();
        return itemKey + ":" + slot.min() + ":" + slot.max() + ":"
                + String.join(",", slot.whitelist()) + ":"
                + String.join(",", slot.blacklist()) + ":"
                + (slot.nbt() != null ? slot.nbt() : "");
    }

    static String tagForItem(String item) {
        if (item == null || item.isBlank() || item.startsWith("#")) {
            return null;
        }
        if (item.equals("minecraft:wool")) {
            return "#minecraft:wool";
        }
        if (item.equals("minecraft:terracotta")) {
            return "#minecraft:terracotta";
        }
        if (item.equals("minecraft:stained_glass")) {
            return "#minecraft:stained_glass";
        }
        for (String color : WOOL_COLORS) {
            if (item.equals("minecraft:" + color + "_wool")) {
                return "#minecraft:wool";
            }
            if (item.equals("minecraft:" + color + "_carpet")) {
                return "#minecraft:wool_carpets";
            }
            if (item.equals("minecraft:" + color + "_stained_glass")) {
                return "#minecraft:stained_glass";
            }
            if (item.equals("minecraft:" + color + "_terracotta")) {
                return "#minecraft:terracotta";
            }
            if (item.equals("minecraft:" + color + "_glazed_terracotta")) {
                return "#minecraft:glazed_terracotta";
            }
            if (item.equals("minecraft:" + color + "_dye")) {
                return "#minecraft:dyes";
            }
            if (item.equals("minecraft:" + color + "_bed")) {
                return "#minecraft:beds";
            }
            if (item.equals("minecraft:" + color + "_banner")) {
                return "#craftlevel:colored_banners";
            }
        }
        return null;
    }
}
