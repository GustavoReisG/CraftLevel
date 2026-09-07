package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Glory Kills no canto superior direito enquanto o altar está aberto. */
@OnlyIn(Dist.CLIENT)
public final class AltarGloryOverlay {

    private static final ResourceLocation GLORY_ICON =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/glory_kill.png");
    private static final int MARGIN = 8;
    private static final int ICON_SIZE = 16;
    private static final int GAP = 4;

    private AltarGloryOverlay() {}

    public static void renderTopRight(GuiGraphics graphics, Font font, int screenWidth) {
        int glory = SyncPlayerGloryPacket.ClientGloryState.gloryKills;
        if (glory < 0) {
            return;
        }

        Component text = HudTextHelper.immortalText(String.valueOf(glory));
        int textWidth = font.width(text);
        int totalWidth = ICON_SIZE + GAP + textWidth;
        int x = screenWidth - MARGIN - totalWidth;
        int y = MARGIN;

        RenderSystem.enableBlend();
        graphics.blit(GLORY_ICON, x, y, ICON_SIZE, ICON_SIZE, 0, 0, 32, 32, 32, 32);
        graphics.drawString(font, text, x + ICON_SIZE + GAP, y + 4, 0xFFFFFF, true);
        RenderSystem.disableBlend();
    }
}
