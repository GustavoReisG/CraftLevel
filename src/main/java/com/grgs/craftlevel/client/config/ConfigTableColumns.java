package com.grgs.craftlevel.client.config;

/** Larguras de colunas alinhadas à tela Mob Levels (46% / 44% / 10%). */
final class ConfigTableColumns {

    private static final float COL_PRIMARY = 0.46f;
    private static final float COL_SECONDARY = 0.44f;

    private ConfigTableColumns() {}

    record ThreeColumns(int primary, int secondary, int remove) {}

    /** Potion | Time | Level | Remove (~46% / 22% / 22% / 10%). */
    record FourColumns(int primary, int secondary, int tertiary, int remove) {}

    static ThreeColumns layout(int tableWidth) {
        int primary = (int) (tableWidth * COL_PRIMARY);
        int secondary = (int) (tableWidth * COL_SECONDARY);
        int remove = tableWidth - primary - secondary;
        return new ThreeColumns(primary, secondary, remove);
    }

    static FourColumns layoutFour(int tableWidth) {
        int remove = Math.max(28, (int) (tableWidth * 0.10f));
        int rest = tableWidth - remove;
        int primary = (int) (rest * 0.50f);
        int secondary = (int) (rest * 0.25f);
        int tertiary = rest - primary - secondary;
        return new FourColumns(primary, secondary, tertiary, remove);
    }

    static int centeredX(int columnStart, int columnWidth, int widgetWidth) {
        return columnStart + (columnWidth - widgetWidth) / 2;
    }
}
