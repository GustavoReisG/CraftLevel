package com.grgs.craftlevel.scaling;

/** Como o valor escalado é aplicado ao valor atual (attribute ou evento). */
public enum ApplyMode {
    /** Multiplica o valor atual pelo escalado. */
    MULTIPLY,
    /** Soma o escalado ao valor atual. */
    ADD,
    /** Substitui pelo valor escalado. */
    OVERWRITE;

    public static ApplyMode fromString(String value, ApplyMode fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
