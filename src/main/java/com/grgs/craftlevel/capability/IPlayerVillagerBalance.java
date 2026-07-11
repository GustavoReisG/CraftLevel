package com.grgs.craftlevel.capability;

import java.util.UUID;

/** Saldo de esmeraldas do villager exibido por jogador, por entidade. */
public interface IPlayerVillagerBalance {

    int getBalance(UUID entityId);

    void setBalance(UUID entityId, int balance);

    void addBalance(UUID entityId, int amount);

    boolean trySpendBalance(UUID entityId, int amount);

    void ensureEntity(UUID entityId, String professionId, int starting, long gameTime);

    void resetEntity(UUID entityId, int starting);

    String getProfession(UUID entityId);

    long getLastRefreshTick(UUID entityId);

    void setLastRefreshTick(UUID entityId, long gameTime);

    Iterable<UUID> trackedEntities();

    void applyEconomyConfig();

    void pruneEntity(UUID entityId);

    void copyFrom(IPlayerVillagerBalance other);

    boolean isInitialized();

    void setInitialized(boolean initialized);
}
