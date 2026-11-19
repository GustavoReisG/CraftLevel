package com.grgs.craftlevel.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.altar.AltarTradeOffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ofertas da aba Trade em altar_of_death.json. */
public final class AltarTradeParser {

    public record TradeEntry(String itemId, int gloryCost, int count) {
        public TradeEntry {
            itemId = itemId == null ? "" : itemId.trim();
            gloryCost = Math.max(0, gloryCost);
            count = Math.max(1, count);
        }
    }

    private static List<TradeEntry> entries = defaultEntries();
    private static List<AltarTradeOffer> offers = resolveOffers(entries);

    private AltarTradeParser() {}

    public static void parse(JsonObject root) {
        List<TradeEntry> next = defaultEntries();
        if (root != null && root.has("trade") && root.get("trade").isJsonObject()) {
            JsonObject trade = root.getAsJsonObject("trade");
            if (trade.has("offers") && trade.get("offers").isJsonArray()) {
                next = readOffers(trade.getAsJsonArray("offers"));
                if (next.isEmpty()) {
                    next = defaultEntries();
                }
            }
        }
        entries = List.copyOf(next);
        offers = resolveOffers(entries);
        CraftLevel.LOGGER.info("[CraftLevel] Altar trade config: {} offers", offers.size());
    }

    public static List<TradeEntry> entries() {
        return entries;
    }

    public static List<AltarTradeOffer> offers() {
        return offers;
    }

    public static List<TradeEntry> defaultEntries() {
        List<TradeEntry> list = new ArrayList<>();
        list.add(new TradeEntry("minecraft:totem_of_undying", 100, 1));
        list.add(new TradeEntry("craftlevel:totem_of_vitality", 200, 1));
        list.add(new TradeEntry("craftlevel:totem_of_overcharge", 200, 1));
        list.add(new TradeEntry("craftlevel:totem_of_blessing", 200, 1));
        list.add(new TradeEntry("craftlevel:rune_of_unbreakable", 500, 1));
        list.add(new TradeEntry("craftlevel:rune_of_veloce", 500, 1));
        list.add(new TradeEntry("craftlevel:rune_of_reach", 500, 1));
        list.add(new TradeEntry("craftlevel:rune_of_fortune", 500, 1));
        list.add(new TradeEntry("craftlevel:rune_of_return", 500, 1));
        list.add(new TradeEntry("craftlevel:rune_of_soulbound", 500, 1));
        return list;
    }

    public static JsonArray writeOffers(List<TradeEntry> source) {
        JsonArray array = new JsonArray();
        for (TradeEntry entry : source) {
            if (entry.itemId().isEmpty()) {
                continue;
            }
            JsonObject obj = new JsonObject();
            obj.addProperty("item", entry.itemId());
            obj.addProperty("glory_cost", Math.max(0, entry.gloryCost()));
            if (entry.count() != 1) {
                obj.addProperty("count", Math.max(1, entry.count()));
            }
            array.add(obj);
        }
        return array;
    }

    private static List<TradeEntry> readOffers(JsonArray array) {
        List<TradeEntry> list = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            String itemId = readString(obj, "item", "");
            if (itemId.isEmpty()) {
                continue;
            }
            int glory = Math.max(0, readInt(obj, "glory_cost", 1));
            int count = Math.max(1, readInt(obj, "count", 1));
            list.add(new TradeEntry(itemId, glory, count));
        }
        return list;
    }

    private static List<AltarTradeOffer> resolveOffers(List<TradeEntry> source) {
        List<AltarTradeOffer> resolved = new ArrayList<>();
        for (TradeEntry entry : source) {
            ItemStack stack = stackFromId(entry.itemId(), entry.count());
            if (stack.isEmpty()) {
                CraftLevel.LOGGER.warn("[CraftLevel] Altar trade: unknown item '{}'", entry.itemId());
                continue;
            }
            resolved.add(new AltarTradeOffer(stack, entry.gloryCost()));
        }
        return Collections.unmodifiableList(resolved);
    }

    private static ItemStack stackFromId(String itemId, int count) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(1, count));
    }

    private static String readString(JsonObject json, String key, String fallback) {
        try {
            return json.get(key).getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
