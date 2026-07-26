package com.grgs.craftlevel.client;

/** Cache client-side do saldo do emerald bank (para tooltips). */
public final class PlayerEmeraldBankClient {

    private static long cachedBalance = -1L;

    private PlayerEmeraldBankClient() {}

    public static void updateBalance(long balance) {
        cachedBalance = balance;
    }

    public static long getCachedBalance() {
        return cachedBalance;
    }
}
