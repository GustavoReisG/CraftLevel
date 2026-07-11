package com.grgs.craftlevel.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Overlay de esmeraldas do villager na tela de trade. */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = com.grgs.craftlevel.CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VillagerTradeHud {

    private static int cachedBalance = -1;

    private VillagerTradeHud() {}

    public static void updateBalance(int balance) {
        cachedBalance = balance;
    }

    public static void clearBalance() {
        cachedBalance = -1;
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof MerchantScreen screen)) {
            return;
        }
        if (cachedBalance < 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int right = screen.width - 8;
        int top = 8;

        String text = String.valueOf(cachedBalance);
        int textWidth = minecraft.font.width(text);
        int iconSize = 16;
        int gap = 4;
        int iconX = right - iconSize;
        int textX = iconX - gap - textWidth;

        graphics.drawString(minecraft.font, text, textX, top + 4, 0xFFFFFF, true);
        WalletIconRenderer.draw(graphics, iconX, top, iconSize);
    }

    @SubscribeEvent
    public static void onScreenClosed(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof MerchantScreen) {
            clearBalance();
        }
    }
}
