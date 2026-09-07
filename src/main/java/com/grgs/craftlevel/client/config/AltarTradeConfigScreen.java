package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Placeholder da aba Trade do altar. */
public class AltarTradeConfigScreen extends Screen {

    private final Screen parent;

    public AltarTradeConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Altar Trade"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        graphics.drawCenteredString(font, "Coming soon", width / 2, height / 2 - 10, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
