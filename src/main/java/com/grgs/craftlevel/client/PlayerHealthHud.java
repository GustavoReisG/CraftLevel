package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Barra de HP do jogador com texturas (lp_empty / hp_full / hp_bar), no estilo da LP bar.
 * Controlada por client.json → hud.health_bar.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class PlayerHealthHud {

    private static final ResourceLocation HP_BAR = new ResourceLocation(CraftLevel.MODID, "textures/gui/hp_bar.png");
    private static final ResourceLocation LP_EMPTY = new ResourceLocation(CraftLevel.MODID, "textures/gui/lp_empty.png");
    private static final ResourceLocation HP_FULL = new ResourceLocation(CraftLevel.MODID, "textures/gui/hp_full.png");
    private static final ResourceLocation IMMORTAL_FONT = new ResourceLocation(CraftLevel.MODID, "immortal");

    private static final int TEX_WIDTH = 1568;
    private static final int TEX_HEIGHT = 182;
    private static final int TEX_FULL_WIDTH = 1336;
    private static final int TEX_FULL_HEIGHT = 182;

    private static final int BASE_ABSORPTION_GAP = 3;
    private static final int ABSORPTION_COLOR = 0xFFFFD700;

    private PlayerHealthHud() {}

    @SubscribeEvent
    public static void onHideVanillaHearts(RenderGuiOverlayEvent.Pre event) {
        if (!ClientConfig.hudHealthBarEnabled) {
            return;
        }
        if (VanillaGuiOverlay.PLAYER_HEALTH.id().equals(event.getOverlay().id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!ClientConfig.hudHealthBarEnabled) {
            return;
        }
        if (!VanillaGuiOverlay.CROSSHAIR.id().equals(event.getOverlay().id())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.options.renderDebug || player.isSpectator()) {
            return;
        }

        float scale = ClientConfig.hudHealthBarScale;
        int renderWidth = Math.round(TEX_WIDTH * scale);
        int renderHeight = Math.round(TEX_HEIGHT * scale);
        int renderFullWidth = Math.round(TEX_FULL_WIDTH * scale);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int[] anchor = ClientHudLayout.anchorCenter(
                ClientConfig.hudHealthBarAnchor,
                screenWidth,
                screenHeight,
                renderWidth,
                renderHeight
        );
        int x = anchor[0] - (renderWidth / 2) + ClientConfig.hudHealthBarX;
        int y = anchor[1] - (renderHeight / 2) + ClientConfig.hudHealthBarY;
        int xFull = anchor[0] - (renderFullWidth / 2) + ClientConfig.hudHealthBarX;

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float progress = maxHealth > 0 ? Math.min(1f, health / maxHealth) : 0f;

        GuiGraphics graphics = event.getGuiGraphics();
        float fontScale = ClientConfig.hudHealthBarFontScale;
        int absorptionGap = Math.max(1, Math.round(BASE_ABSORPTION_GAP * scale));
        drawAbsorptionText(graphics, mc, x, y, renderWidth, renderHeight, absorptionGap, fontScale, player.getAbsorptionAmount());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.blit(
                LP_EMPTY,
                x, y,
                renderWidth, renderHeight,
                0, 0,
                TEX_WIDTH, TEX_HEIGHT,
                TEX_WIDTH, TEX_HEIGHT
        );

        int cutTex = Math.round(TEX_FULL_WIDTH * progress);
        int cutRender = Math.round(renderFullWidth * progress);
        if (cutRender > 0) {
            graphics.blit(
                    HP_FULL,
                    xFull, y,
                    cutRender, renderHeight,
                    0, 0,
                    cutTex, TEX_FULL_HEIGHT,
                    TEX_FULL_WIDTH, TEX_FULL_HEIGHT
            );
        }

        graphics.blit(
                HP_BAR,
                x, y,
                renderWidth, renderHeight,
                0, 0,
                TEX_WIDTH, TEX_HEIGHT,
                TEX_WIDTH, TEX_HEIGHT
        );

        drawHealthText(graphics, mc, x, y, renderWidth, renderHeight, fontScale, health, maxHealth);
    }

    private static void drawAbsorptionText(
            GuiGraphics graphics,
            Minecraft mc,
            int barX,
            int barY,
            int barWidth,
            int barHeight,
            int absorptionGap,
            float fontScale,
            float absorption
    ) {
        if (absorption <= 0) {
            return;
        }

        String text = formatHealth(absorption);
        Component component = immortalText(text);
        float totalScale = fontScale;
        int textWidth = Math.round(mc.font.width(component) * totalScale);
        int textHeight = Math.round(mc.font.lineHeight * totalScale);
        int textX = barX - absorptionGap - textWidth;
        int textY = barY + (barHeight - textHeight) / 2;

        drawScaledText(graphics, mc.font, component, textX, textY, totalScale, ABSORPTION_COLOR);
    }

    private static void drawHealthText(
            GuiGraphics graphics,
            Minecraft mc,
            int barX,
            int barY,
            int barWidth,
            int barHeight,
            float fontScale,
            float health,
            float maxHealth
    ) {
        String text = formatHealth(health) + " / " + formatHealth(maxHealth);
        Component component = immortalText(text);
        int rawWidth = mc.font.width(component);
        float fitScale = rawWidth > 0 && rawWidth * fontScale > barWidth - 4
                ? (barWidth - 4f) / (rawWidth * fontScale)
                : 1f;
        float totalScale = fontScale * fitScale;

        int textWidth = Math.round(rawWidth * totalScale);
        int textHeight = Math.round(mc.font.lineHeight * totalScale);
        int textX = barX + (barWidth - textWidth) / 2;
        int textY = barY + (barHeight - textHeight) / 2;

        drawScaledText(graphics, mc.font, component, textX, textY, totalScale, 0xFFFFFF);
    }

    private static void drawScaledText(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            float scale,
            int color
    ) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        if (scale != 1f) {
            graphics.pose().scale(scale, scale, 1f);
        }
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }

    private static Component immortalText(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(IMMORTAL_FONT));
    }

    private static String formatHealth(float value) {
        int rounded = Math.round(value);
        if (rounded >= 1_000_000) {
            return String.format("%.1fM", rounded / 1_000_000f);
        }
        if (rounded >= 10_000) {
            return String.format("%.1fk", rounded / 1_000f);
        }
        return String.valueOf(rounded);
    }
}
