package com.grgs.craftlevel.capability;

/** Implementação mutável do saldo do emerald bank. */
public class PlayerEmeraldBank implements IPlayerEmeraldBank {

    private long balance;
    private boolean initialized;

    @Override
    public long getBalance() {
        return balance;
    }

    @Override
    public void setBalance(long balance) {
        this.balance = Math.max(0L, balance);
    }

    @Override
    public void addEmeralds(long amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    @Override
    public boolean tryRemoveEmeralds(long amount) {
        if (amount <= 0 || balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
