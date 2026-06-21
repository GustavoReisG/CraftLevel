package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public class CraftLevelHUD {

    private static final ResourceLocation LP_BAR   = new ResourceLocation(CraftLevel.MODID, "textures/gui/lp_bar.png");
    private static final ResourceLocation LP_EMPTY = new ResourceLocation(CraftLevel.MODID, "textures/gui/lp_empty.png");
    private static final ResourceLocation LP_FULL  = new ResourceLocation(CraftLevel.MODID, "textures/gui/lp_full.png");
    private static final ResourceLocation LEVEL_CIRCLE = new ResourceLocation(CraftLevel.MODID, "textures/gui/lvl.png");

    private static final int TEX_CIRCLE_WIDTH  = 608;
    private static final int TEX_CIRCLE_HEIGHT = 590;

    private static final int TEX_WIDTH       = 1568;
    private static final int TEX_HEIGHT      = 182;
    private static final int TEX_FULL_WIDTH  = 1336;
    private static final int TEX_FULL_HEIGHT = 182;

    // Cache dos valores
    public static int cachedLp = 0;
    public static int cachedLpNeeded = 100;
    public static float cachedProgress = 0f;
    public static int cachedLevel = 1;

    public static void updateCache(int lp, int lpNeeded, int level) {
        cachedLp = lp;
        cachedLpNeeded = lpNeeded;
        cachedLevel = level;
        cachedProgress = lpNeeded > 0 ? (float) lp / lpNeeded : 0f;
        cachedProgress = Math.min(1.0f, Math.max(0.0f, cachedProgress));
    }

    private static int[] getAnchorPosition(String anchor, int screenWidth, int screenHeight, int elemWidth, int elemHeight) {
        int ax, ay;
        switch (anchor) {
            case "top_left"      -> { ax = elemWidth / 2;                 ay = elemHeight / 2; }
            case "top_center"    -> { ax = screenWidth / 2;               ay = elemHeight / 2; }
            case "top_right"     -> { ax = screenWidth - elemWidth / 2;   ay = elemHeight / 2; }
            case "center_left"   -> { ax = elemWidth / 2;                 ay = screenHeight / 2; }
            case "center_right"  -> { ax = screenWidth - elemWidth / 2;   ay = screenHeight / 2; }
            case "bottom_left"   -> { ax = elemWidth / 2;                 ay = screenHeight - elemHeight / 2; }
            case "bottom_center" -> { ax = screenWidth / 2;               ay = screenHeight - elemHeight / 2; }
            case "bottom_right"  -> { ax = screenWidth - elemWidth / 2;   ay = screenHeight - elemHeight / 2; }
            default              -> { ax = screenWidth / 2;               ay = screenHeight / 2; }
        }
        return new int[]{ax, ay};
    }

    @SubscribeEvent
    public static void onRenderHUD(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.options.renderDebug) return;

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float scale = ClientConfig.hudLpBarScale;

        int renderWidth      = (int)(TEX_WIDTH      * scale);
        int renderHeight     = (int)(TEX_HEIGHT      * scale);
        int renderFullWidth  = (int)(TEX_FULL_WIDTH  * scale);

        int[] lpAnchor = getAnchorPosition(ClientConfig.hudLpBarAnchor, screenWidth, screenHeight, renderWidth, renderHeight);
        int x     = lpAnchor[0] - (renderWidth    / 2) + ClientConfig.hudLpBarX;
        int y     = lpAnchor[1] - (renderHeight   / 2) + ClientConfig.hudLpBarY;
        int xFull = lpAnchor[0] - (renderFullWidth / 2) + ClientConfig.hudLpBarX;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. Fundo vazio
        event.getGuiGraphics().blit(
            LP_EMPTY,
            x, y,
            renderWidth, renderHeight,
            0, 0,
            TEX_WIDTH, TEX_HEIGHT,
            TEX_WIDTH, TEX_HEIGHT
        );

        // 2. Preenchimento verde
        int cutTex    = (int)(TEX_FULL_WIDTH  * cachedProgress);
        int cutRender = (int)(renderFullWidth  * cachedProgress);

        if (cutRender > 0) {
            event.getGuiGraphics().blit(
                LP_FULL,
                xFull, y,
                cutRender, renderHeight,
                0, 0,
                cutTex, TEX_FULL_HEIGHT,
                TEX_FULL_WIDTH, TEX_FULL_HEIGHT
            );
        }

        // 3. Decoração por cima
        event.getGuiGraphics().blit(
            LP_BAR,
            x, y,
            renderWidth, renderHeight,
            0, 0,
            TEX_WIDTH, TEX_HEIGHT,
            TEX_WIDTH, TEX_HEIGHT
        );

        float circleScale = ClientConfig.hudLevelCircleScale;

        int circleWidth  = (int)(TEX_CIRCLE_WIDTH  * circleScale);
        int circleHeight = (int)(TEX_CIRCLE_HEIGHT * circleScale);

        int[] circleAnchor = getAnchorPosition(ClientConfig.hudLevelCircleAnchor, screenWidth, screenHeight, circleWidth, circleHeight);
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

        String levelText = String.valueOf(cachedLevel);

        net.minecraft.network.chat.Component levelComponent = net.minecraft.network.chat.Component.literal(levelText)
                .withStyle(s -> s.withFont(new ResourceLocation("craftlevel", "immortal")));

        float fontScale = ClientConfig.hudLevelCircleFontScale * circleScale / 0.1f;

        int textWidth  = (int)(mc.font.width(levelComponent) * fontScale);
        int textHeight = (int)(4.0f * fontScale);

        int textX = cx + (circleWidth  / 2) - (textWidth  / 2);
        int textY = cy + (circleHeight / 2) - (textHeight / 2);

        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(textX, textY, 0);
        event.getGuiGraphics().pose().scale(fontScale, fontScale, 1.0f);

        event.getGuiGraphics().drawString(
                mc.font,
                levelComponent,
                0, 0,
                0xFFFFFF,
                false
        );

        event.getGuiGraphics().pose().popPose();
    }
}