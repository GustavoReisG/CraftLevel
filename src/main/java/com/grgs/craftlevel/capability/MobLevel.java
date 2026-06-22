package com.grgs.craftlevel.capability;

/** Implementação simples do level de um mob. */
public class MobLevel implements IMobLevel {
    private int level = 0;

    @Override
    public int getLevel() { return level; }

    @Override
    public void setLevel(int level) { this.level = level; }
}
