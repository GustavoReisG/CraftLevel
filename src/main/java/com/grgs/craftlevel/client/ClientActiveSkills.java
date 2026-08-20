package com.grgs.craftlevel.client;

import java.util.HashMap;
import java.util.Map;

/** Estado client: skillId → gameTime de expiração do efeito ativo. */
public final class ClientActiveSkills {

    private static final Map<String, Long> EXPIRE_AT = new HashMap<>();

    private ClientActiveSkills() {}

    public static void apply(Map<String, Long> expireAtGameTime) {
        EXPIRE_AT.clear();
        if (expireAtGameTime != null) {
            EXPIRE_AT.putAll(expireAtGameTime);
        }
    }

    public static Map<String, Long> snapshot() {
        return Map.copyOf(EXPIRE_AT);
    }
}
