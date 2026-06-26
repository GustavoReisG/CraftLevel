package com.grgs.craftlevel.capability;

import com.grgs.craftlevel.config.ConfigParser;

import static com.grgs.craftlevel.config.ConfigManager.lpConfig;

/** Implementação da progressão do jogador: level e LP. */
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

    /** Calcula LP para o próximo level usando Scaling de lp.json. */
    @Override
    public int getLpToNextLevel() {
        ConfigParser.parseLp(lpConfig);
        return (int) (ConfigParser.lpBase * Math.pow(ConfigParser.lpMultiplier, level - 1));
    }

    @Override
    public void addLp(int amount) {
        this.lp += amount;
    }

    /** Level máximo hardcoded em 200. */
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
}
