package com.grgs.craftlevel.menu;

/** Abas do altar; a ordem define o ordinal sincronizado no menu. */
public enum AltarTab {
    CONVERSION(0, "altar_conversion_gui"),
    POTION(1, "altar_potion_gui"),
    IMPROVEMENT(2, "altar_improvement_gui"),
    TRADE(3, "altar_trade_gui");

    private final int id;
    private final String textureName;

    AltarTab(int id, String textureName) {
        this.id = id;
        this.textureName = textureName;
    }

    public int id() {
        return id;
    }

    public String textureName() {
        return textureName;
    }

    public String titleKey() {
        return "gui.craftlevel.altar.tab." + name().toLowerCase();
    }

    public String iconName() {
        return switch (this) {
            case CONVERSION -> "transmutation";
            case POTION -> "potion";
            case IMPROVEMENT -> "improvement";
            case TRADE -> "trade";
        };
    }

    public static AltarTab byId(int id) {
        for (AltarTab tab : values()) {
            if (tab.id == id) {
                return tab;
            }
        }
        return CONVERSION;
    }
}
