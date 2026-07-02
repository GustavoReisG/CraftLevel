package com.grgs.craftlevel.capability;

import com.grgs.craftlevel.config.ConfigParser;
import com.grgs.craftlevel.scaling.ScalingEngine;
import com.grgs.craftlevel.scaling.ScalingRegistry;

/**
 * Estado de progressão do player: level + LP acumulado.
 * LP to next level usa o escalador lp_scaling (power + overwrite).
 */
public class PlayerLevel implements IPlayerLevel {
    private int level = 1;
    private int lp = 0;

    @Override
    public int getLevel() { return level; }

    @Override
    public void setLevel(int level) { this.level = level; }

    @Override
    public int getLp() { return lp; }

    @Override
    public void setLp(int lp) { this.lp = lp; }

    @Override
    public int getLpToNextLevel() {
        double scaled = ScalingEngine.computeScaledValue(ScalingRegistry.getLpScaling(), level);
        return (int) scaled;
    }

    @Override
    public void addLp(int amount) {
        this.lp += amount;
    }

    @Override
    public boolean canLevelUp() {
        return this.lp >= getLpToNextLevel() && this.level < ConfigParser.getMaxLevel();
    }

    @Override
    public void levelUp() {
        if (canLevelUp()) {
            this.lp -= getLpToNextLevel();
            this.level++;
        }
    }
}
