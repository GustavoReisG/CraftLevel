package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.villager.TagExpansionCache;
import com.grgs.craftlevel.villager.VillagerTradeSlotDef;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Ícone de preview para slots de trade na UI de config. */
@OnlyIn(Dist.CLIENT)
final class TradeSlotIconHelper {

    private TradeSlotIconHelper() {}

    static ItemStack preview(VillagerTradeSlotDef slot) {
        if (slot == null || slot.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<Item> candidates = candidates(slot);
        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int count = slot.min() > 0 ? slot.min() : 1;
        return new ItemStack(candidates.get(0), count);
    }

    private static List<Item> candidates(VillagerTradeSlotDef slot) {
        if (!slot.whitelist().isEmpty()) {
            return new java.util.ArrayList<>(TagExpansionCache.expandAll(slot.whitelist()));
        }

        String item = slot.item();
        if (item == null || item.isBlank()) {
            return List.of();
        }

        List<Item> expanded = TagExpansionCache.expand(item);
        if (!expanded.isEmpty()) {
            return expanded;
        }

        ResourceLocation id = ResourceLocation.tryParse(item);
        if (id == null) {
            return List.of();
        }
        Item direct = BuiltInRegistries.ITEM.get(id);
        return direct != null ? List.of(direct) : List.of();
    }
}
