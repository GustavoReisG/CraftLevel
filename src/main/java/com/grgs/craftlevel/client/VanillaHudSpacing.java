package com.grgs.craftlevel.client;

import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Ajusta leftHeight/rightHeight do ForgeGui para overlays vanilla. */
final class VanillaHudSpacing {

    private VanillaHudSpacing() {}

    static void pushLeft(ForgeGui gui, int pixels) {
        if (pixels > 0) {
            gui.leftHeight += pixels;
        }
    }

    static void pushRight(ForgeGui gui, int pixels) {
        if (pixels > 0) {
            gui.rightHeight += pixels;
        }
    }
}
