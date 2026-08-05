package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.item.ImbuePotionItem;
import com.grgs.craftlevel.item.ModItems;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cores dinâmicas da poção de embuir:
 * <ul>
 *   <li>{@code tintindex 0} — líquido ({@link PotionUtils#getColor})</li>
 *   <li>{@code tintindex 1} — rolha/tampa ({@link ImbuePotionItem#getCorkColor})</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModItemColors {

    private ModItemColors() {}

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0) {
                        return PotionUtils.getColor(stack);
                    }
                    if (tintIndex == 1) {
                        return ImbuePotionItem.getCorkColor(stack);
                    }
                    return -1;
                },
                ModItems.IMBUE_POTION.get()
        );
    }
}
