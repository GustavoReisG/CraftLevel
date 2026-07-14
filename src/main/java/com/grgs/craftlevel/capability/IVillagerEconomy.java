package com.grgs.craftlevel.capability;

/** Saldo de esmeraldas e usos de trade por villager. */
public interface IVillagerEconomy {

    int getBalance();

    void setBalance(int balance);

    void addBalance(int amount);

    boolean trySpendBalance(int amount);

    void ensureInitialized(String professionId, int starting, long gameTime);

    void resetBalance(int starting);

    String getProfessionId();

    long getLastRefreshTick();

    void setLastRefreshTick(long gameTime);

    int getTradeUses(int tradeIndex);

    void incrementTradeUses(int tradeIndex);

    void clearTradeUses();

    void applyEconomyConfig(String professionId);

    boolean isInitialized();

    void setInitialized(boolean initialized);
}
