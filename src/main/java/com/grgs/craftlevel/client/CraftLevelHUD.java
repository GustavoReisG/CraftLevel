package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** HUD do jogador: barra de LP e círculo de level, renderizados sobre a tela. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public class CraftLevelHUD {

    private static final ResourceLocation LP_BAR   = new ResourceLocation(CraftLevel.MODID, "textures/gui/lp_bar.png");
    private static final ResourceLocation LP_EMPTY = new ResourceLocation(CraftLevel.MODID, "textures/gui/empty.png");
    private static final ResourceLocation LP_FULL  = new ResourceLocation(CraftLevel.MODID, "textures/gui/lp_full.png");
    private static final ResourceLocation LEVEL_CIRCLE = new ResourceLocation(CraftLevel.MODID, "textures/gui/lvl.png");

    private static final int TEX_CIRCLE_WIDTH  = 292;
    private static final int TEX_CIRCLE_HEIGHT = 292;

    // Cache sincronizado via SyncPlayerLevelPacket — usado pelo HUD
    public static int cachedLp = 0;
    public static int cachedLpNeeded = 100;
    public static float cachedProgress = 0f;
    public static int cachedLevel = 1;

    /** Atualiza cache local quando o servidor envia level/LP atualizados. */
    public static void updateCache(int lp, int lpNeeded, int level) {
        cachedLp = lp;
        cachedLpNeeded = lpNeeded;
        cachedLevel = level;
        cachedProgress = lpNeeded > 0 ? (float) lp / lpNeeded : 0f;
        cachedProgress = Math.min(1.0f, Math.max(0.0f, cachedProgress));
    }

    @SubscribeEvent
    public static void onRenderHUD(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.CROSSHAIR.id().equals(event.getOverlay().id())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.options.renderDebug) return;

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float scale = ClientConfig.hudLpBarScale;
        int[] barSize = HudBarRenderer.barDimensions(scale);
        int renderWidth = barSize[0];
        int renderHeight = barSize[1];

        int[] lpAnchor = ClientHudLayout.anchorCenter(
                ClientConfig.hudLpBarAnchor, screenWidth, screenHeight, renderWidth, renderHeight);
        int x = lpAnchor[0] - (renderWidth / 2) + ClientConfig.hudLpBarX;
        int y = lpAnchor[1] - (renderHeight / 2) + ClientConfig.hudLpBarY;

        HudBarRenderer.draw(event.getGuiGraphics(), LP_EMPTY, LP_FULL, LP_BAR, x, y, scale, cachedProgress);

        String lpText = cachedLp + " / " + cachedLpNeeded;
        HudTextHelper.drawCenteredInBar(
                event.getGuiGraphics(),
                mc.font,
                HudTextHelper.immortalText(lpText),
                x, y, renderWidth, renderHeight,
                ClientConfig.hudLpBarFontScale,
                0xFFFFFF,
                true
        );

        float circleScale = ClientConfig.hudLevelCircleScale;

        int circleWidth  = (int)(TEX_CIRCLE_WIDTH  * circleScale);
        int circleHeight = (int)(TEX_CIRCLE_HEIGHT * circleScale);

        int[] circleAnchor = ClientHudLayout.anchorCenter(
                ClientConfig.hudLevelCircleAnchor, screenWidth, screenHeight, circleWidth, circleHeight);
        int cx = circleAnchor[0] - (circleWidth  / 2) + ClientConfig.hudLevelCircleX;
        int cy = circleAnchor[1] - (circleHeight / 2) + ClientConfig.hudLevelCircleY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        event.getGuiGraphics().blit(
            LEVEL_CIRCLE,
            cx, cy,
            circleWidth, circleHeight,
            0, 0,
            TEX_CIRCLE_WIDTH, TEX_CIRCLE_HEIGHT,
            TEX_CIRCLE_WIDTH, TEX_CIRCLE_HEIGHT
        );

        RenderSystem.disableBlend();

        float fontScale = ClientConfig.hudLevelCircleFontScale * circleScale / 0.1f;
        HudTextHelper.drawCenteredInBox(
                event.getGuiGraphics(),
                mc.font,
                HudTextHelper.immortalText(String.valueOf(cachedLevel)),
                cx, cy, circleWidth, circleHeight,
                fontScale,
                0xFFFFFF,
                false
        );
    }
}
