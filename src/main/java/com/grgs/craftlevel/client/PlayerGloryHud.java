package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
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

/** HUD de Glory Kills (ícone + quantidade), configurável em CLIENT → Glory Kills. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class PlayerGloryHud {

    private static final ResourceLocation GLORY_ICON =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/glory_kill.png");
    private static final float TEXT_HEIGHT_FACTOR = 4.0f;

    private static int cachedGlory = -1;

    private PlayerGloryHud() {}

    public static void updateGlory(int gloryKills) {
        cachedGlory = Math.max(0, gloryKills);
        SyncPlayerGloryPacket.ClientGloryState.gloryKills = cachedGlory;
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.CROSSHAIR.id().equals(event.getOverlay().id())) {
            return;
        }
        if (!ClientConfig.hudGloryKillsEnabled || cachedGlory < 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.renderDebug || minecraft.player == null) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        String text = String.valueOf(cachedGlory);
        Component textComponent = HudTextHelper.immortalText(text);
        float hudScale = ClientConfig.hudGloryKillsScale;
        float fontScale = ClientConfig.hudGloryKillsFontScale * hudScale;
        int textWidth = Math.round(minecraft.font.width(textComponent) * fontScale);
        int textHeight = Math.round(TEXT_HEIGHT_FACTOR * fontScale);
        int iconRenderedSize = Math.max(8, Math.round(16 * hudScale));
        int gap = Math.max(2, Math.round(4 * hudScale));
        boolean iconLeft = "left".equals(ClientConfig.hudGloryKillsIconSide);

        int elemWidth = iconRenderedSize + gap + textWidth;
        int elemHeight = Math.max(iconRenderedSize, textHeight);

        int[] anchor = ClientHudLayout.anchorCenter(
                ClientConfig.hudGloryKillsAnchor, screenWidth, screenHeight, elemWidth, elemHeight);
        int baseX = anchor[0] - (elemWidth / 2) + ClientConfig.hudGloryKillsX;
        int baseY = anchor[1] - (elemHeight / 2) + ClientConfig.hudGloryKillsY;

        int iconX = iconLeft ? baseX : baseX + textWidth + gap;
        int textBoxX = iconLeft ? baseX + iconRenderedSize + gap : baseX;
        int iconY = baseY + (elemHeight - iconRenderedSize) / 2;

        GuiGraphics graphics = event.getGuiGraphics();
        RenderSystem.enableBlend();
        graphics.blit(
                GLORY_ICON,
                iconX, iconY,
                iconRenderedSize, iconRenderedSize,
                0, 0,
                32, 32,
                32, 32
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
