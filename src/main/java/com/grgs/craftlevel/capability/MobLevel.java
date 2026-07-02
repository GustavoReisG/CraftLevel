package com.grgs.craftlevel.capability;

/** Level numérico atribuído a um mob no spawn (0 = não inicializado). */
public class MobLevel implements IMobLevel {
    private int level = 0;

    @Override
    public int getLevel() { return level; }

    @Override
    public void setLevel(int level) { this.level = level; }
}
