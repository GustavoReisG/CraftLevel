package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Submenu Altar Of Death nas configs. */
public class AltarOfDeathConfigMenuScreen extends Screen {

    private final Screen parent;

    public AltarOfDeathConfigMenuScreen(Screen parent) {
        super(Component.literal("CraftLevel - Altar Of Death"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Button conversion = Button.builder(Component.literal("Conversion"), btn -> {
                    AltarConversionPersistence.beginEditing(minecraft);
                    minecraft.setScreen(new AltarConversionConfigScreen(this));
                })
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 0, conversion);
        addRenderableWidget(conversion);

        Button potion = Button.builder(Component.literal("Potion"), btn ->
                        minecraft.setScreen(new AltarPotionConfigScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 1, potion);
        addRenderableWidget(potion);

        Button improvement = Button.builder(Component.literal("Improvement"), btn ->
                        minecraft.setScreen(new AltarImprovementConfigScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 2, improvement);
        addRenderableWidget(improvement);

        Button trade = Button.builder(Component.literal("Trade"), btn ->
                        minecraft.setScreen(new AltarTradeConfigScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 3, trade);
        addRenderableWidget(trade);

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
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
