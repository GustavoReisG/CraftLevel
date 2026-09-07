package com.grgs.craftlevel.client;

import com.grgs.craftlevel.menu.AltarTab;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Tooltip legível das abas do altar. */
@OnlyIn(Dist.CLIENT)
public final class AltarScrambleTooltip {

    private AltarScrambleTooltip() {}

    public static int tabColor(AltarTab tab) {
        return switch (tab) {
            case CONVERSION -> 0xFF8B5CF6;
            case POTION -> 0xFFEAB308;
            case IMPROVEMENT -> 0xFF38BDF8;
            case TRADE -> 0xFF34D399;
        };
    }

    public static void renderBelow(GuiGraphics graphics, Font font, Component name, int centerX, int topY, int color) {
        Component line = Component.literal(name.getString())
                .withStyle(Style.EMPTY.withColor(color).withBold(true));
        int textWidth = font.width(line);
        int x = centerX - textWidth / 2;
        graphics.renderComponentTooltip(font, List.of(line), x, topY);
    }
}
