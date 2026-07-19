package com.grgs.craftlevel.capability;

/** Souls do jogador — buffer de mortes antes de perder itens/esmeraldas. */
public interface IPlayerSouls {

    int getSouls();

    void setSouls(int souls);

    int getRestoreProgressTicks();

    void setRestoreProgressTicks(int ticks);

    boolean isInitialized();

    void setInitialized(boolean initialized);
}
