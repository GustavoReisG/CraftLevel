package com.grgs.craftlevel.capability;

/** Contrato da capability de progressão do jogador (level e LP). */
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
}
