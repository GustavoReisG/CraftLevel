package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Submenu Arena nas configs. */
public class ArenaConfigMenuScreen extends Screen {

    private final Screen parent;

    public ArenaConfigMenuScreen(Screen parent) {
        super(Component.literal("CraftLevel - Arena"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Button schematics = Button.builder(Component.literal("Schematics"), btn -> {
                    ArenaConfigPersistence.beginEditing(minecraft);
                    minecraft.setScreen(new ArenaSchematicsScreen(this));
                })
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 0, schematics);
        addRenderableWidget(schematics);

        Button hordes = Button.builder(Component.literal("Hordes"), btn -> {
                    ArenaConfigPersistence.beginEditing(minecraft);
                    minecraft.setScreen(new ArenaHordeSetsScreen(this));
                })
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 1, hordes);
        addRenderableWidget(hordes);

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
