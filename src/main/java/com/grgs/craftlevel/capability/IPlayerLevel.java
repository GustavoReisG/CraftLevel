package com.grgs.craftlevel.capability;

/**
 * Contrato da progressão do jogador persistida em NBT.
 * Level máximo vem de config.json → lp_scaling.lvl_max.
 */
public interface IPlayerLevel {
    int getLevel();
    void setLevel(int level);

    int getLp();
    void setLp(int lp);

    /** LP necessário para subir do level atual ao próximo (usa lp_scaling). */
    int getLpToNextLevel();

    void addLp(int amount);
    boolean canLevelUp();
    void levelUp();
}
