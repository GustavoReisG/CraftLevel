package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.altar.AltarPotionService;
import com.grgs.craftlevel.potion.PotionLevelText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AltarPotionTooltip {

    private AltarPotionTooltip() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (AltarPotionService.isPotionItem(stack) || hasPotionEffects(stack)) {
            for (int i = 0; i < tooltip.size(); i++) {
                tooltip.set(i, PotionLevelText.remapDeep(tooltip.get(i)));
            }
        }

        if (!AltarPotionService.isPotionItem(stack) || !AltarPotionService.hasEffects(stack)) {
            return;
        }

        int durationTicks = AltarPotionService.currentMaxDurationTicks(stack);
        if (durationTicks > 0) {
            tooltip.add(Component.translatable(
                    "gui.craftlevel.altar.potion.limit_duration",
                    AltarPotionService.formatDuration(durationTicks / 20),
                    AltarPotionService.formatDuration(AltarPotionService.maxDurationSeconds(stack))
            ));
        }

        int level = AltarPotionService.currentMaxLevel(stack);
        if (level > 0) {
            tooltip.add(Component.translatable(
                    "gui.craftlevel.altar.potion.limit_level",
                    level,
                    AltarPotionService.maxLevel(stack)
            ));
        }
    }

    private static boolean hasPotionEffects(ItemStack stack) {
        return !stack.isEmpty() && !PotionUtils.getMobEffects(stack).isEmpty();
    }
}
