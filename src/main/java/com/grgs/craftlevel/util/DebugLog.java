package com.grgs.craftlevel.util;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigParser;

/**
 * Logs de diagnóstico controlados por {@code debug} em config.json.
 * Quando debug = false, todas as chamadas são ignoradas.
 */
public final class DebugLog {

    private DebugLog() {}

    public static void info(String message, Object... args) {
        if (!ConfigParser.debug) {
            return;
        }
        CraftLevel.LOGGER.info("[CraftLevel] " + message, args);
    }

    public static void warn(String message, Object... args) {
        if (!ConfigParser.debug) {
            return;
        }
        CraftLevel.LOGGER.warn("[CraftLevel] " + message, args);
    }
}
