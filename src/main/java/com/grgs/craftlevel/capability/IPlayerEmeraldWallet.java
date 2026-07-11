package com.grgs.craftlevel.capability;

/** Saldo de esmeraldas guardadas na wallet do jogador. */
public interface IPlayerEmeraldWallet {

    long getBalance();

    void setBalance(long balance);

    void addEmeralds(long amount);

    boolean tryRemoveEmeralds(long amount);

    boolean isInitialized();

    void setInitialized(boolean initialized);
}
