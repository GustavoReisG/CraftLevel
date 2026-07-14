package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Submenu Economy nas configs. */
public class EconomyConfigMenuScreen extends Screen {

    private final Screen parent;

    public EconomyConfigMenuScreen(Screen parent) {
        super(Component.literal("CraftLevel - Economy"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Button balance = Button.builder(Component.literal("Balance"), btn ->
                        minecraft.setScreen(new EconomyBalanceConfigScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 0, balance);
        addRenderableWidget(balance);

        Button villagers = Button.builder(Component.literal("Villagers"), btn ->
                        minecraft.setScreen(new VillagerConfigMenuScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 1, villagers);
        addRenderableWidget(villagers);

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
