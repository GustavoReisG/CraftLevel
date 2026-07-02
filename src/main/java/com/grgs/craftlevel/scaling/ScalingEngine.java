package com.grgs.craftlevel.scaling;

/**
 * Motor de cálculo dos escaladores.
 * <p>
 * Fase 1 — {@link #computeScaledValue}: base + scaling + level (usa level - 1).
 * Fase 2 — {@link #toAttributeModifierAmount} ou {@link #applyEventAmount}: apply_mode.
 */
public final class ScalingEngine {

    private ScalingEngine() {}

    /**
     * Calcula valor escalado bruto.
     * <ul>
     *   <li>POWER: base * pow(scaling, level - 1)</li>
     *   <li>LINEAR: base + scaling * (level - 1)</li>
     *   <li>PRODUCT: base * scaling * (level - 1)</li>
     * </ul>
     */
    public static double computeScaledValue(ScalingDefinition def, int level) {
        int levelFactor = Math.max(0, level - 1);

        return switch (def.scalingMode()) {
            case POWER -> def.baseValue() * Math.pow(def.scaling(), levelFactor);
            case LINEAR -> def.baseValue() + (def.scaling() * levelFactor);
            case PRODUCT -> def.baseValue() * def.scaling() * levelFactor;
        };
    }

    /** Converte valor escalado em amount ADDITIVE para AttributeModifier. */
    public static double toAttributeModifierAmount(double currentBase, double scaledValue, ApplyMode applyMode) {
        return switch (applyMode) {
            case MULTIPLY -> (currentBase * scaledValue) - currentBase;
            case ADD -> scaledValue;
            case OVERWRITE -> scaledValue - currentBase;
        };
    }

    /** Aplica valor escalado ao amount de hurt/heal. */
    public static float applyEventAmount(float current, double scaledValue, ApplyMode applyMode) {
        return switch (applyMode) {
            case MULTIPLY -> (float) (current * scaledValue);
            case ADD -> (float) (current + scaledValue);
            case OVERWRITE -> (float) scaledValue;
        };
    }
}
