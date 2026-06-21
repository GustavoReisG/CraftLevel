package com.grgs.craftlevel.capability;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigParser;

import static com.grgs.craftlevel.config.ConfigManager.lpConfig;

public class PlayerLevel implements IPlayerLevel {
    private int level = 1;
    private int lp = 0;
    private double undeadBonus = 0;
    private double arthropodBonus = 0;
    private double illagerBonus = 0;

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

        CraftLevel.LOGGER.info("[CraftLevel] Config loaded from");

        double multiplier;

        ConfigParser.parseLp(lpConfig);

        if (level <= 50) {
            multiplier = ConfigParser.lpMultiplier.T1();
        } else if (level <= 100) {
            multiplier = ConfigParser.lpMultiplier.T2();
        } else if (level <= 150) {
            multiplier = ConfigParser.lpMultiplier.T3();
        } else {
            multiplier = ConfigParser.lpMultiplier.T4();
        }
        return (int) (ConfigParser.lpBase * Math.pow(multiplier, level - 1));
    }

    @Override
    public void addLp(int amount) {
        this.lp += amount;
    }

    @Override
    public boolean canLevelUp() {
        return this.lp >= getLpToNextLevel() && this.level < 200;
    }

    @Override
    public void levelUp() {
        if (canLevelUp()) {
            this.lp -= getLpToNextLevel();
            this.level++;
        }
    }

    @Override
    public double getUndeadBonus() { return undeadBonus; }
    @Override
    public void setUndeadBonus(double value) { this.undeadBonus = value; }

    @Override
    public double getArthropodBonus() { return arthropodBonus; }
    @Override
    public void setArthropodBonus(double value) { this.arthropodBonus = value; }
}