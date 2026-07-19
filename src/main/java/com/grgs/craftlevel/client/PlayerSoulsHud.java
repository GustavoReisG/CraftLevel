package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.network.SyncPlayerSoulsPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** HUD de souls (ícone + quantidade), configurável em CLIENT → Souls. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class PlayerSoulsHud {

    private static final ResourceLocation SOUL_ICON =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/soul.png");
    private static final float TEXT_HEIGHT_FACTOR = 4.0f;

    private static int cachedSouls = -1;

    private PlayerSoulsHud() {}

    public static void updateSouls(int souls) {
        cachedSouls = Math.max(0, souls);
        SyncPlayerSoulsPacket.ClientSoulsState.souls = cachedSouls;
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.CROSSHAIR.id().equals(event.getOverlay().id())) {
            return;
        }
        if (!ClientConfig.hudSoulsEnabled || cachedSouls < 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.renderDebug || minecraft.player == null) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        String text = String.valueOf(cachedSouls);
        Component textComponent = HudTextHelper.immortalText(text);
        float hudScale = ClientConfig.hudSoulsScale;
        float fontScale = ClientConfig.hudSoulsFontScale * hudScale;
        int textWidth = Math.round(minecraft.font.width(textComponent) * fontScale);
        int textHeight = Math.round(TEXT_HEIGHT_FACTOR * fontScale);
        int iconRenderedSize = Math.max(8, Math.round(16 * hudScale));
        int gap = Math.max(2, Math.round(4 * hudScale));
        boolean iconLeft = "left".equals(ClientConfig.hudSoulsIconSide);

        int elemWidth = iconRenderedSize + gap + textWidth;
        int elemHeight = Math.max(iconRenderedSize, textHeight);

        int[] anchor = ClientHudLayout.anchorCenter(
                ClientConfig.hudSoulsAnchor, screenWidth, screenHeight, elemWidth, elemHeight);
        int baseX = anchor[0] - (elemWidth / 2) + ClientConfig.hudSoulsX;
        int baseY = anchor[1] - (elemHeight / 2) + ClientConfig.hudSoulsY;

        int iconX = iconLeft ? baseX : baseX + textWidth + gap;
        int textBoxX = iconLeft ? baseX + iconRenderedSize + gap : baseX;
        int iconY = baseY + (elemHeight - iconRenderedSize) / 2;

        GuiGraphics graphics = event.getGuiGraphics();
        RenderSystem.enableBlend();
        graphics.blit(
                SOUL_ICON,
                iconX, iconY,
                iconRenderedSize, iconRenderedSize,
                0, 0,
                16, 16,
                16, 16
        );
        HudTextHelper.drawCenteredInBox(
                graphics,
                minecraft.font,
                textComponent,
                textBoxX,
                baseY,
                textWidth,
                elemHeight,
                fontScale,
                0xFFFFFF,
                true
        );
        RenderSystem.disableBlend();
    }
}
