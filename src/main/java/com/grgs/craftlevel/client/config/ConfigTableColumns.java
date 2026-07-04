package com.grgs.craftlevel.client.config;

/** Larguras de colunas alinhadas à tela Mob Levels (46% / 44% / 10%). */
final class ConfigTableColumns {

    private static final float COL_PRIMARY = 0.46f;
    private static final float COL_SECONDARY = 0.44f;

    private ConfigTableColumns() {}

    record ThreeColumns(int primary, int secondary, int remove) {}

    static ThreeColumns layout(int tableWidth) {
        int primary = (int) (tableWidth * COL_PRIMARY);
        int secondary = (int) (tableWidth * COL_SECONDARY);
        int remove = tableWidth - primary - secondary;
        return new ThreeColumns(primary, secondary, remove);
    }

    static int centeredX(int columnStart, int columnWidth, int widgetWidth) {
        return columnStart + (columnWidth - widgetWidth) / 2;
    }
}
