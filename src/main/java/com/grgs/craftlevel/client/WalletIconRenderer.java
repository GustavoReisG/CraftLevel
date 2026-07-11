package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Desenha o ícone da wallet (textura 256x256) escalado para o tamanho de exibição. */
final class WalletIconRenderer {

    static final ResourceLocation TEXTURE =
            new ResourceLocation(CraftLevel.MODID, "textures/item/wallet.png");

    static final int TEX_SIZE = 256;

    private WalletIconRenderer() {}

    static void draw(GuiGraphics graphics, int x, int y, int size) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(
                TEXTURE,
                x, y,
                size, size,
                0, 0,
                TEX_SIZE, TEX_SIZE,
                TEX_SIZE, TEX_SIZE
        );
    }
}
