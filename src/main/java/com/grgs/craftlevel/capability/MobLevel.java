package com.grgs.craftlevel.capability;

public class MobLevel implements IMobLevel {
    private int level = 0;

    @Override
    public int getLevel() { return level; }

    @Override
    public void setLevel(int level) { this.level = level; }
}