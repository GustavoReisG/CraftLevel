package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Botões e ícones do altar.
 * {@code altar_bottons.png}: 3 estados 25×25 → GUI 18×18.
 * {@code altar_icons.png}: 4 ícones 25×25 → GUI 18×18.
 */
@OnlyIn(Dist.CLIENT)
public final class AltarGuiIcons {

    public static final int WIDGET_SIZE = 18;

    private static final int SRC_SIZE = 25;
    private static final int BUTTON_TEX_W = 75;
    private static final int ICON_TEX_W = 100;
    private static final int TEX_H = 25;

    private static final ResourceLocation BUTTONS =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/altar_bottons.png");
    private static final ResourceLocation ICONS =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/altar_icons.png");

    /** Ordem em {@code altar_icons.png}: tempo, força, conversão, improvement. */
    public enum Icon {
        POTION_TIME(0),
        POTION_STRENGTH(1),
        CONVERSION(2),
        IMPROVEMENT(3);

        private final int index;

        Icon(int index) {
            this.index = index;
        }

        int u() {
            return index * SRC_SIZE;
        }
    }

    private AltarGuiIcons() {}

    public static void blitButton(GuiGraphics graphics, int x, int y, boolean hovered, boolean enabled, boolean pressed) {
        int u;
        if (!enabled || pressed) {
            u = SRC_SIZE * 2; // layer 3 — inativo ou clicado
        } else if (hovered) {
            u = SRC_SIZE; // layer 2 — hover
        } else {
            u = 0; // layer 1 — normal / clicável
        }
        graphics.blit(BUTTONS, x, y, WIDGET_SIZE, WIDGET_SIZE, u, 0, SRC_SIZE, SRC_SIZE, BUTTON_TEX_W, TEX_H);
    }

    public static void blitIcon(GuiGraphics graphics, int x, int y, Icon icon) {
        graphics.blit(ICONS, x, y, WIDGET_SIZE, WIDGET_SIZE, icon.u(), 0, SRC_SIZE, SRC_SIZE, ICON_TEX_W, TEX_H);
    }

    public static void blitIcon(GuiGraphics graphics, int x, int y, Icon icon, boolean enabled) {
        if (!enabled) {
            graphics.setColor(0.5F, 0.5F, 0.5F, 1.0F);
        }
        blitIcon(graphics, x, y, icon);
        if (!enabled) {
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
