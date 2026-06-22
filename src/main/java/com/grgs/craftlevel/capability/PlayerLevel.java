package com.grgs.craftlevel.capability;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigParser;

import static com.grgs.craftlevel.config.ConfigManager.lpConfig;

/** Implementação da progressão do jogador: level, LP e cache de dano condicional de encantamentos. */
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

    /** Calcula LP para o próximo level usando tiers T1–T4 de lp.json. */
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

    /** Dano de Smite/Bane calculado pelo WeaponScaler e aplicado no LivingHurtEvent. */
    private final java.util.HashMap<String, Double> conditionalEnchantDamage = new java.util.HashMap<>();

    @Override
    public java.util.Map<String, Double> getConditionalEnchantDamage() {
        return conditionalEnchantDamage;
    }

    @Override
    public void clearConditionalEnchantDamage() {
        conditionalEnchantDamage.clear();
    }

    @Override
    public void addConditionalEnchantDamage(String entityType, double damage) {
        conditionalEnchantDamage.merge(entityType, damage, Double::sum);
    }
}
