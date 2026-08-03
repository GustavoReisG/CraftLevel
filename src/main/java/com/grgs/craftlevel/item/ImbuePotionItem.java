package com.grgs.craftlevel.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Poção de embuir: um item, variantes via NBT de poção (como {@code minecraft:potion}).
 * Cor do líquido vem de {@link PotionUtils#getColor} + {@code tintindex} no model.
 */
public class ImbuePotionItem extends Item {

    public ImbuePotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        return PotionUtils.setPotion(super.getDefaultInstance(), Potions.WATER);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(
                PotionUtils.getPotion(stack).getName(getDescriptionId() + ".effect.")
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        PotionUtils.addPotionTooltip(stack, tooltip, 1.0F);
    }

    /** Preenche a creative tab com uma stack por poção do registry (exceto empty). */
    public static void fillCreative(net.minecraft.world.item.CreativeModeTab.Output output, Item item) {
        for (Potion potion : BuiltInRegistries.POTION) {
            if (potion == Potions.EMPTY) {
                continue;
            }
            output.accept(PotionUtils.setPotion(new ItemStack(item), potion));
        }
    }
}
