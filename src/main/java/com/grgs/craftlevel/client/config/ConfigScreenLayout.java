package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

/** Posicionamento de widgets nas telas de config (compatível com 1.20.1). */
final class ConfigScreenLayout {

    static final int BUTTON_WIDTH = 200;
    static final int ROW_HEIGHT = 24;
    static final int START_Y = 40;
    static final int LABEL_WIDTH = 115;
    static final int FIELD_GAP = 6;

    static final int ACTION_BUTTON_WIDTH = 100;
    static final int ACTION_GAP = 8;
    static final int BOTTOM_MARGIN = 32;

    private ConfigScreenLayout() {}

    static int centerX(Screen screen) {
        return (screen.width - BUTTON_WIDTH) / 2;
    }

    static int fieldWidth() {
        return BUTTON_WIDTH - LABEL_WIDTH - FIELD_GAP;
    }

    static int rowY(Screen screen, int row) {
        return START_Y + row * ROW_HEIGHT;
    }

    /** Campo de valor à direita; legenda desenhada com {@link #renderLabel}. */
    static void positionField(Screen screen, int row, AbstractWidget widget) {
        widget.setX(centerX(screen) + LABEL_WIDTH + FIELD_GAP);
        widget.setY(rowY(screen, row));
        widget.setWidth(fieldWidth());
    }

    static void renderLabel(Screen screen, GuiGraphics graphics, int row, String label) {
        graphics.drawString(screen.getMinecraft().font, label, centerX(screen) + 4, rowY(screen, row) + 6, 0xFFFFFF);
    }

    /** Botões de menu ocupam a linha inteira (sem legenda lateral). */
    static void positionRow(Screen screen, int row, AbstractWidget widget) {
        widget.setX(centerX(screen));
        widget.setY(rowY(screen, row));
        widget.setWidth(BUTTON_WIDTH);
    }

    static int bottomY(Screen screen) {
        return screen.height - BOTTOM_MARGIN;
    }

    static int contentBottom(Screen screen) {
        return bottomY(screen) - 8;
    }

    static void positionSave(Screen screen, net.minecraft.client.gui.components.Button button) {
        button.setWidth(ACTION_BUTTON_WIDTH);
        button.setX(screen.width / 2 - ACTION_BUTTON_WIDTH - ACTION_GAP);
        button.setY(bottomY(screen));
    }

    static void positionBack(Screen screen, net.minecraft.client.gui.components.Button button) {
        button.setWidth(ACTION_BUTTON_WIDTH);
        button.setX(screen.width / 2 + ACTION_GAP);
        button.setY(bottomY(screen));
    }

    static void positionDone(Screen screen, net.minecraft.client.gui.components.Button button) {
        button.setWidth(ACTION_BUTTON_WIDTH);
        button.setX((screen.width - ACTION_BUTTON_WIDTH) / 2);
        button.setY(bottomY(screen));
    }

    static net.minecraft.client.gui.components.Button bottomDone(Screen screen, Runnable action) {
        net.minecraft.client.gui.components.Button button =
                net.minecraft.client.gui.components.Button.builder(
                                net.minecraft.network.chat.Component.literal("Done"), btn -> action.run())
                        .width(ACTION_BUTTON_WIDTH)
                        .build();
        positionDone(screen, button);
        return button;
    }

    static net.minecraft.client.gui.components.Button bottomSave(Screen screen, Runnable action) {
        net.minecraft.client.gui.components.Button button =
                net.minecraft.client.gui.components.Button.builder(
                                net.minecraft.network.chat.Component.literal("Save"), btn -> action.run())
                        .width(ACTION_BUTTON_WIDTH)
                        .build();
        positionSave(screen, button);
        return button;
    }

    static net.minecraft.client.gui.components.Button bottomBack(Screen screen, Runnable action) {
        net.minecraft.client.gui.components.Button button =
                net.minecraft.client.gui.components.Button.builder(
                                net.minecraft.network.chat.Component.literal("Back"), btn -> action.run())
                        .width(ACTION_BUTTON_WIDTH)
                        .build();
        positionBack(screen, button);
        return button;
    }
}
