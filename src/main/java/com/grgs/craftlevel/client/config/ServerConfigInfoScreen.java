package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Aviso para configs de servidor — editar JSON manualmente fora do jogo. */
public class ServerConfigInfoScreen extends Screen {

    private final Screen parent;
    private final String fileName;

    protected ServerConfigInfoScreen(Screen parent, String fileName) {
        super(Component.literal("CraftLevel - " + ConfigScreenLabels.fileButtonLabel(fileName)));
        this.parent = parent;
        this.fileName = fileName;
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
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(fileName + ".json"), width / 2, 40, 0xAAAAAA);
        graphics.drawCenteredString(font, Component.literal("Config de servidor."), width / 2, 56, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal("Edite o arquivo manualmente"), width / 2, 68, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal("em config/craftlevel/ no servidor."), width / 2, 80, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
