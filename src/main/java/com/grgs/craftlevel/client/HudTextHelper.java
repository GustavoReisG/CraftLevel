package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Texto centralizado para barras e elementos do HUD. */
final class HudTextHelper {

    private static final ResourceLocation IMMORTAL_FONT = new ResourceLocation(CraftLevel.MODID, "immortal");
    /** Mesmo fator usado no level circle — funciona melhor que font.lineHeight para a fonte Immortal. */
    private static final float TEXT_HEIGHT_FACTOR = 4.0f;

    private HudTextHelper() {}

    static Component immortalText(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(IMMORTAL_FONT));
    }

    static void drawCenteredInBar(
            GuiGraphics graphics,
            Font font,
            Component text,
            int barX,
            int barY,
            int barWidth,
            int barHeight,
            float fontScale,
            int color,
            boolean shadow
    ) {
        int rawWidth = font.width(text);
        float fitScale = rawWidth > 0 && rawWidth * fontScale > barWidth - 4
                ? (barWidth - 4f) / (rawWidth * fontScale)
                : 1f;
        float totalScale = fontScale * fitScale;
        int textWidth = Math.round(rawWidth * totalScale);
        int textHeight = Math.round(TEXT_HEIGHT_FACTOR * totalScale);
        int textX = barX + (barWidth - textWidth) / 2;
        int textY = barY + (barHeight - textHeight) / 2;
        drawScaledText(graphics, font, text, textX, textY, totalScale, color, shadow);
    }

    static void drawLeftOfBar(
            GuiGraphics graphics,
            Font font,
            Component text,
            int barX,
            int barY,
            int barWidth,
            int barHeight,
            int gap,
            float fontScale,
            int color,
            boolean shadow
    ) {
        int textWidth = Math.round(font.width(text) * fontScale);
        int textHeight = Math.round(TEXT_HEIGHT_FACTOR * fontScale);
        int textX = barX - gap - textWidth;
        int textY = barY + (barHeight - textHeight) / 2;
        drawScaledText(graphics, font, text, textX, textY, fontScale, color, shadow);
    }

    static void drawRightOfBar(
            GuiGraphics graphics,
            Font font,
            Component text,
            int barX,
            int barY,
            int barWidth,
            int barHeight,
            int gap,
            float fontScale,
            int color,
            boolean shadow
    ) {
        int textHeight = Math.round(TEXT_HEIGHT_FACTOR * fontScale);
        int textX = barX + barWidth + gap;
        int textY = barY + (barHeight - textHeight) / 2;
        drawScaledText(graphics, font, text, textX, textY, fontScale, color, shadow);
    }

    static void drawScaledString(
            GuiGraphics graphics,
            Font font,
            String text,
            int x,
            int y,
            float scale,
            int color,
            boolean shadow
    ) {
        drawScaledText(graphics, font, Component.literal(text), x, y, scale, color, shadow);
    }

    static void drawCenteredInBox(
            GuiGraphics graphics,
            Font font,
            Component text,
            int boxX,
            int boxY,
            int boxWidth,
            int boxHeight,
            float fontScale,
            int color,
            boolean shadow
    ) {
        int textWidth = Math.round(font.width(text) * fontScale);
        int textHeight = Math.round(TEXT_HEIGHT_FACTOR * fontScale);
        int textX = boxX + (boxWidth - textWidth) / 2;
        int textY = boxY + (boxHeight - textHeight) / 2;
        drawScaledText(graphics, font, text, textX, textY, fontScale, color, shadow);
    }

    static String formatCompactNumber(float value) {
        int rounded = Math.round(value);
        if (rounded >= 1_000_000) {
            return String.format("%.1fM", rounded / 1_000_000f);
        }
        if (rounded >= 10_000) {
            return String.format("%.1fk", rounded / 1_000f);
        }
        return String.valueOf(rounded);
    }

    static String formatSaturation(float saturation) {
        float rounded = Math.round(saturation * 10f) / 10f;
        if (Math.abs(rounded - Math.round(rounded)) < 0.05f) {
            return String.valueOf(Math.round(rounded));
        }
        return String.format("%.1f", rounded);
    }

    private static void drawScaledText(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            float scale,
            int color,
            boolean shadow
    ) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        if (scale != 1f) {
            graphics.pose().scale(scale, scale, 1f);
        }
        graphics.drawString(font, text, 0, 0, color, shadow);
        graphics.pose().popPose();
    }
}
