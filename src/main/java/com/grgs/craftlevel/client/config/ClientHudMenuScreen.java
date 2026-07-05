package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Submenu HUD: LP Bar e Level Circle. */
public class ClientHudMenuScreen extends Screen {

    private final Screen parent;

    protected ClientHudMenuScreen(Screen parent) {
        super(Component.literal("CraftLevel - HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Button lpBar = menuButton("LP Bar",
                () -> new HudElementConfigScreen(this, HudElementConfigScreen.Element.LP_BAR));
        ConfigScreenLayout.positionRow(this, 0, lpBar);
        addRenderableWidget(lpBar);

        Button levelCircle = menuButton("Level Circle",
                () -> new HudElementConfigScreen(this, HudElementConfigScreen.Element.LEVEL_CIRCLE));
        ConfigScreenLayout.positionRow(this, 1, levelCircle);
        addRenderableWidget(levelCircle);

        Button healthBar = menuButton("Health Bar",
                () -> new HudElementConfigScreen(this, HudElementConfigScreen.Element.HEALTH_BAR));
        ConfigScreenLayout.positionRow(this, 2, healthBar);
        addRenderableWidget(healthBar);

        Button hungerBar = menuButton("Hunger Bar",
                () -> new HudElementConfigScreen(this, HudElementConfigScreen.Element.HUNGER_BAR));
        ConfigScreenLayout.positionRow(this, 3, hungerBar);
        addRenderableWidget(hungerBar);

        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private Button menuButton(String label, ScreenFactory factory) {
        return Button.builder(Component.literal(label), btn -> minecraft.setScreen(factory.create()))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
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

    @FunctionalInterface
    private interface ScreenFactory {
        Screen create();
    }
}
