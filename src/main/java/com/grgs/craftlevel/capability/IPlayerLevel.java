package com.grgs.craftlevel.capability;

/** Contrato da capability de progressão do jogador (level, LP e encantamentos condicionais). */
public interface IPlayerLevel {
    int getLevel();
    void setLevel(int level);

    int getLp();
    void setLp(int lp);

    /** LP necessário para subir do level atual ao próximo. */
    int getLpToNextLevel();

    void addLp(int amount);
    boolean canLevelUp();
    void levelUp();

    /** Dano extra de encantamentos condicionais (Smite, Bane) indexado por tipo de mob. */
    java.util.Map<String, Double> getConditionalEnchantDamage();
    void clearConditionalEnchantDamage();
    void addConditionalEnchantDamage(String entityType, double damage);
}
