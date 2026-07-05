package com.grgs.craftlevel.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Desenha barras de progresso: EMPTY → FULL → BAR. */
final class HudBarRenderer {

    static final int TEX_WIDTH = 1036;
    static final int TEX_HEIGHT = 116;

    private HudBarRenderer() {}

    static void draw(
            GuiGraphics graphics,
            ResourceLocation empty,
            ResourceLocation full,
            ResourceLocation bar,
            int x,
            int y,
            float scale,
            float progress
    ) {
        int renderWidth = Math.round(TEX_WIDTH * scale);
        int renderHeight = Math.round(TEX_HEIGHT * scale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.blit(
                empty,
                x, y,
                renderWidth, renderHeight,
                0, 0,
                TEX_WIDTH, TEX_HEIGHT,
                TEX_WIDTH, TEX_HEIGHT
        );

        progress = Math.min(1f, Math.max(0f, progress));
        int cutTex = Math.round(TEX_WIDTH * progress);
        int cutRender = Math.round(renderWidth * progress);
        if (cutRender > 0) {
            graphics.blit(
                    full,
                    x, y,
                    cutRender, renderHeight,
                    0, 0,
                    cutTex, TEX_HEIGHT,
                    TEX_WIDTH, TEX_HEIGHT
            );
        }

        graphics.blit(
                bar,
                x, y,
                renderWidth, renderHeight,
                0, 0,
                TEX_WIDTH, TEX_HEIGHT,
                TEX_WIDTH, TEX_HEIGHT
        );
    }

    static int[] barDimensions(float scale) {
        return new int[] {
                Math.round(TEX_WIDTH * scale),
                Math.round(TEX_HEIGHT * scale)
        };
    }
}
