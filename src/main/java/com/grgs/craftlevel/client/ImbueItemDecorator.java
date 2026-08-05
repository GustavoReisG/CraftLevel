package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.item.ItemImbue;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Segunda barra sob o ícone (estilo durabilidade) com a carga de imbue.
 * Fica acima da barra de durabilidade vanilla quando ambas existem.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ImbueItemDecorator implements IItemDecorator {

    private static final ImbueItemDecorator INSTANCE = new ImbueItemDecorator();

    private ImbueItemDecorator() {}

    @SubscribeEvent
    public static void onRegisterDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (ItemImbue.isWeaponOrTool(new ItemStack(item))) {
                event.register(item, INSTANCE);
            }
        }
    }

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        int amount = ItemImbue.getAmount(stack);
        if (amount <= 0) {
            return false;
        }

        int barY = stack.isBarVisible() ? y + 11 : y + 13;
        int width = Math.round(13.0F * Math.min(1.0F, (float) amount / (float) ItemImbue.MAX));
        int color = ItemImbue.getColor(stack) | 0xFF000000;

        graphics.fill(RenderType.guiOverlay(), x + 2, barY, x + 15, barY + 2, 0xFF000000);
        if (width > 0) {
            graphics.fill(RenderType.guiOverlay(), x + 2, barY, x + 2 + width, barY + 1, color);
        }
        return true;
    }
}
