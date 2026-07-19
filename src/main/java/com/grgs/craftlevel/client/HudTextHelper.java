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

    static void drawScaledComponent(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            float scale,
            int color,
            boolean shadow
    ) {
        drawScaledText(graphics, font, text, x, y, scale, color, shadow);
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
        drawCenteredInBox(graphics, font, text, boxX, boxY, boxWidth, boxHeight, fontScale, 0.0f, color, shadow);
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
            float yOffset,
            int color,
            boolean shadow
    ) {
        int textWidth = Math.round(font.width(text) * fontScale);
        int textHeight = Math.round(TEXT_HEIGHT_FACTOR * fontScale);
        int textX = boxX + (boxWidth - textWidth) / 2;
        int textY = boxY + (boxHeight - textHeight) / 2 + Math.round(yOffset * fontScale);
        drawScaledText(graphics, font, text, textX, textY, fontScale, color, shadow);
    }

    /**
     * Texto Immortal centralizado num retângulo [left,right] × linha vertical {@code centerY}
     * (coordenadas já em pixels de tela). Corta com "..." se passar da largura.
     */
    static void drawCenteredClipped(
            GuiGraphics graphics,
            Font font,
            String text,
            int left,
            int right,
            int centerY,
            float fontScale,
            int color,
            boolean shadow
    ) {
        int boxWidth = Math.max(0, right - left);
        if (boxWidth <= 0 || text == null || text.isEmpty()) {
            return;
        }

        int maxUnscaled = Math.max(1, Math.round(boxWidth / Math.max(fontScale, 0.01f)));
        Component component = immortalText(truncateToWidth(font, text, maxUnscaled));
        int textWidth = Math.round(font.width(component) * fontScale);
        int textHeight = Math.round(TEXT_HEIGHT_FACTOR * fontScale);
        int textX = left + (boxWidth - textWidth) / 2;
        int textY = centerY - textHeight / 2;
        drawScaledText(graphics, font, component, textX, textY, fontScale, color, shadow);
    }

    /** Texto Immortal com centro em ({@code centerX}, {@code centerY}). */
    static void drawCenteredAt(
            GuiGraphics graphics,
            Font font,
            String text,
            int centerX,
            int centerY,
            float fontScale,
            int color,
            boolean shadow
    ) {
        Component component = immortalText(text);
        int textWidth = Math.round(font.width(component) * fontScale);
        int textHeight = Math.round(TEXT_HEIGHT_FACTOR * fontScale);
        int textX = centerX - textWidth / 2;
        int textY = centerY - textHeight / 2;
        drawScaledText(graphics, font, component, textX, textY, fontScale, color, shadow);
    }

    static String truncateToWidth(Font font, String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(immortalText(text)) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = font.width(immortalText(ellipsis));
        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        int end = text.length();
        while (end > 0 && font.width(immortalText(text.substring(0, end))) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }

    /** Abrevia vida/valores: 1000→1K, 1_000_000→1M, 1_000_000_000→1B. */
    static String formatCompactNumber(float value) {
        double amount = Math.max(0d, value);
        if (amount >= 1_000_000_000d) {
            return formatCompactSuffix(amount / 1_000_000_000d, "B");
        }
        if (amount >= 1_000_000d) {
            return formatCompactSuffix(amount / 1_000_000d, "M");
        }
        if (amount >= 1_000d) {
            return formatCompactSuffix(amount / 1_000d, "K");
        }
        return String.valueOf(Math.round(amount));
    }

    private static String formatCompactSuffix(double scaled, String suffix) {
        double rounded = Math.round(scaled * 10d) / 10d;
        if (Math.abs(rounded - Math.rint(rounded)) < 0.05d) {
            return String.valueOf((long) Math.rint(rounded)) + suffix;
        }
        return String.format("%.1f%s", rounded, suffix);
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
