package com.grgs.craftlevel.scaling;

/** Como base_value e scaling combinam com o level (sempre usa level - 1). */
public enum ScalingMode {
    /** base * pow(scaling, level - 1) */
    POWER,
    /** base + scaling * (level - 1) */
    LINEAR,
    /** base * scaling * (level - 1) */
    PRODUCT;

    public static ScalingMode fromString(String value, ScalingMode fallback) {
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
