package com.grgs.craftlevel.capability;

/** Saldo de esmeraldas guardadas no bank do jogador (via Storage items). */
public interface IPlayerEmeraldBank {

    long getBalance();

    void setBalance(long balance);

    void addEmeralds(long amount);

    boolean tryRemoveEmeralds(long amount);

    boolean isInitialized();

    void setInitialized(boolean initialized);
}
