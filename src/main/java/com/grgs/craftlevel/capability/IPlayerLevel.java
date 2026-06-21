package com.grgs.craftlevel.capability;

public interface IPlayerLevel {
    int getLevel();
    void setLevel(int level);

    int getLp();
    void setLp(int lp);

    int getLpToNextLevel();

    void addLp(int amount);
    boolean canLevelUp();
    void levelUp();

    // IPlayerLevel.java
    double getUndeadBonus();
    void setUndeadBonus(double value);

    double getArthropodBonus();
    void setArthropodBonus(double value);
}