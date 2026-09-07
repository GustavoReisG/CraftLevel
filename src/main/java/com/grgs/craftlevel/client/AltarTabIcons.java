package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.menu.AltarTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Ícones das abas do altar (PNG dedicado por aba). */
@OnlyIn(Dist.CLIENT)
public final class AltarTabIcons {

    private static final int DEDICATED_ICON_TEX = 32;

    private AltarTabIcons() {}

    public static ResourceLocation texture(AltarTab tab) {
        return new ResourceLocation(CraftLevel.MODID, "textures/gui/altar/tab_" + tab.iconName() + ".png");
    }

    public static void blitTabIcon(GuiGraphics graphics, int x, int y, int size, AltarTab tab) {
        int color = AltarScrambleTooltip.tabColor(tab);
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        graphics.setColor(red, green, blue, 1.0F);
        graphics.blit(texture(tab), x, y, size, size, 0, 0,
                DEDICATED_ICON_TEX, DEDICATED_ICON_TEX, DEDICATED_ICON_TEX, DEDICATED_ICON_TEX);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
