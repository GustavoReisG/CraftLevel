package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Campo curto com o texto desenhado no centro da caixa. */
final class CenteredEditBox extends EditBox {

    private final Font textFont;

    CenteredEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.textFont = font;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!isVisible()) {
            return;
        }
        int border = isFocused() ? -1 : -6250336;
        graphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, border);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, -16777216);

        String value = getValue();
        int textWidth = textFont.width(value);
        int x = getX() + Math.max(4, (width - textWidth) / 2);
        int y = getY() + (height - 8) / 2;
        graphics.drawString(textFont, value, x, y, 0xE0E0E0);

        if (isFocused() && (System.currentTimeMillis() / 300L) % 2L == 0L) {
            int cursor = Math.min(getCursorPosition(), value.length());
            int cursorX = x + textFont.width(value.substring(0, cursor));
            graphics.fill(cursorX, y - 1, cursorX + 1, y + 9, -3092272);
        }
    }
}
