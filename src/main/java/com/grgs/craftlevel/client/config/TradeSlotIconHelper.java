package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.villager.ItemSpec;
import com.grgs.craftlevel.villager.TagExpansionCache;
import com.grgs.craftlevel.villager.TradeSlotResolver;
import com.grgs.craftlevel.villager.VillagerTradeSlotDef;
import net.minecraft.util.RandomSource;
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

        String source = !slot.whitelist().isEmpty() ? slot.whitelist().get(0) : slot.item();
        ItemSpec.Parsed parsed = ItemSpec.parse(source);
        List<Item> candidates = TagExpansionCache.expand(parsed.id());
        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int count = slot.min() > 0 ? slot.min() : 1;
        ItemStack stack = new ItemStack(candidates.get(0), count);
        String nbt = parsed.hasNbt() ? parsed.nbt() : slot.nbt();
        TradeSlotResolver.applyNbt(stack, nbt, RandomSource.create());
        return stack;
    }
}
