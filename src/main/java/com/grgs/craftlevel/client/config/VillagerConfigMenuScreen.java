package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Submenu Villagers nas configs de economia. */
@OnlyIn(Dist.CLIENT)
public class VillagerConfigMenuScreen extends Screen {

    private final Screen parent;

    public VillagerConfigMenuScreen(Screen parent) {
        super(Component.literal("CraftLevel - Villagers"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Button professions = Button.builder(Component.literal("Professions"), btn ->
                        minecraft.setScreen(new VillagerProfessionsListScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 0, professions);
        addRenderableWidget(professions);

        Button trades = Button.builder(Component.literal("Trades"), btn ->
                        minecraft.setScreen(new VillagerTradesListScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 1, trades);
        addRenderableWidget(trades);

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
