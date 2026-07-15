package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Submenu client: entradas editáveis pelo jogador (HUD). */
public class ClientConfigMenuScreen extends Screen {

    private final Screen parent;

    protected ClientConfigMenuScreen(Screen parent) {
        super(Component.literal("CraftLevel - Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ClientConfigPersistence.beginEditing(minecraft);

        Button hud = Button.builder(Component.literal("HUD"), btn ->
                minecraft.setScreen(new ClientHudMenuScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 0, hud);
        addRenderableWidget(hud);

        Button emeralds = Button.builder(Component.literal("Emeralds"), btn ->
                minecraft.setScreen(new HudElementConfigScreen(this, HudElementConfigScreen.Element.EMERALDS)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 1, emeralds);
        addRenderableWidget(emeralds);

        Button mobFocus = Button.builder(Component.literal("Mob Focus"), btn ->
                minecraft.setScreen(new HudElementConfigScreen(this, HudElementConfigScreen.Element.MOB_FOCUS)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 2, mobFocus);
        addRenderableWidget(mobFocus);

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
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
