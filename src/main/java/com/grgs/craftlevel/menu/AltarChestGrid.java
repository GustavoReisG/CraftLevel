package com.grgs.craftlevel.menu;

/** Malha 9×3 do baú (colunas 1–9, fileiras A–C de cima para baixo). */
public final class AltarChestGrid {

    public static final int ROW_A_Y = 18;
    public static final int ROW_B_Y = 36;
    public static final int ROW_C_Y = 54;

    /** Ajuste fino para alinhar com a moldura pintada na textura do baú. */
    public static final int TEXTURE_OFFSET_X = 2;
    public static final int TEXTURE_OFFSET_Y = -2;

    private AltarChestGrid() {}

    public static int columnX(int col) {
        return AltarOfDeathMenu.SLOT_ORIGIN_X + (col - 1) * AltarOfDeathMenu.SLOT_STEP + TEXTURE_OFFSET_X;
    }

    public static int rowY(char row) {
        int y = switch (Character.toUpperCase(row)) {
            case 'A' -> ROW_A_Y;
            case 'B' -> ROW_B_Y;
            case 'C' -> ROW_C_Y;
            default -> ROW_B_Y;
        };
        return y + TEXTURE_OFFSET_Y;
    }

    public static int spanWidth(int columns) {
        return AltarOfDeathMenu.SLOT_STEP * columns;
    }

    /** Centro do slot na célula da malha (ex.: B2, B8). */
    public static int slotX(int col) {
        return columnX(col) + 7;
    }

    public static int slotY(char row) {
        return rowY(row) + 2;
    }

    /** Botão 18×18 centralizado na coluna da malha. */
    public static int buttonX(int col) {
        return columnX(col) + 6;
    }

    public static int buttonY(char row) {
        return rowY(row) + 1;
    }
}
