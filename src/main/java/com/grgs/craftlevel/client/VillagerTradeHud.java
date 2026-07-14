package com.grgs.craftlevel.client;

import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.VillagerResortTradesPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Overlay de esmeraldas do villager na tela de trade. */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = com.grgs.craftlevel.CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VillagerTradeHud {

    private static final int PORTRAIT_LEFT = 7;
    private static final int PORTRAIT_RIGHT = 105;
    private static final int PORTRAIT_TOP = 7;
    private static final int RESORT_BTN_OFFSET_Y = -30;
    private static final int RESORT_BTN_SIZE = 18;

    private static int cachedBalance = -1;

    private VillagerTradeHud() {}

    public static void updateBalance(int balance) {
        cachedBalance = balance;
    }

    public static void clearBalance() {
        cachedBalance = -1;
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof MerchantScreen screen)) {
            return;
        }
        Button resortButton = Button.builder(Component.literal("⟳"), btn ->
                        PacketHandler.CHANNEL.sendToServer(new VillagerResortTradesPacket()))
                .bounds(0, 0, RESORT_BTN_SIZE, RESORT_BTN_SIZE)
                .tooltip(Tooltip.create(Component.literal("Resort the tag items. Cost: 10 Emeralds")))
                .build();
        positionResortButton(screen, resortButton);
        event.addListener(resortButton);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof MerchantScreen screen)) {
            return;
        }

        for (var child : screen.children()) {
            if (child instanceof Button button && "⟳".equals(button.getMessage().getString())) {
                positionResortButton(screen, button);
            }
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

    private static void positionResortButton(MerchantScreen screen, Button button) {
        int portraitCenterX = screen.getGuiLeft() + (PORTRAIT_LEFT + PORTRAIT_RIGHT) / 2;
        int x = portraitCenterX - RESORT_BTN_SIZE / 2;
        int y = screen.getGuiTop() + PORTRAIT_TOP + RESORT_BTN_OFFSET_Y;
        button.setX(x);
        button.setY(y);
    }

    @SubscribeEvent
    public static void onScreenClosed(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof MerchantScreen) {
            clearBalance();
        }
    }
}
