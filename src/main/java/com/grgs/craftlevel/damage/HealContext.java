package com.grgs.craftlevel.damage;

import com.grgs.craftlevel.config.ConfigParser;

/** Chave de heal pendente para o próximo {@code LivingHealEvent} nesta thread. */
public final class HealContext {

    private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

    private HealContext() {}

    public static void push(String healKey) {
        PENDING.set(healKey);
    }

    public static void clear() {
        PENDING.remove();
    }

    /** Consome a chave pendente, ou {@link ConfigParser#DEFAULT_KEY}. */
    public static String takeOrDefault() {
        String key = PENDING.get();
        PENDING.remove();
        return key == null || key.isEmpty() ? ConfigParser.DEFAULT_KEY : key;
    }
}
