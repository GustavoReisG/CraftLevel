package com.grgs.craftlevel.scaling;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Motor de cálculo dos escaladores.
 * <p>
 * Fase 1 — {@link #computeScaledValue}: base + scaling + level (usa level - 1).
 * Fase 2 — {@link #toAttributeModifier} ou {@link #applyEventAmount}: apply_mode.
 */
public final class ScalingEngine {

    public record AttributeModifierSpec(double amount, AttributeModifier.Operation operation) {}

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

    /**
     * Converte valor escalado em modifier de attribute.
     * <p>
     * Com {@code includeModifiers=false}, altera apenas a base via ADDITION.
     * Com {@code includeModifiers=true}, escala o valor final (base + outros modifiers) via MULTIPLY_TOTAL.
     */
    public static AttributeModifierSpec toAttributeModifier(
            double currentBase,
            double currentTotal,
            double scaledValue,
            ApplyMode applyMode,
            boolean includeModifiers
    ) {
        double reference = includeModifiers ? currentTotal : currentBase;

        if (!includeModifiers) {
            double amount = switch (applyMode) {
                case MULTIPLY -> (reference * scaledValue) - reference;
                case ADD -> scaledValue;
                case OVERWRITE -> scaledValue - reference;
            };
            return new AttributeModifierSpec(amount, AttributeModifier.Operation.ADDITION);
        }

        double target = switch (applyMode) {
            case MULTIPLY -> reference * scaledValue;
            case ADD -> reference + scaledValue;
            case OVERWRITE -> scaledValue;
        };

        if (reference == 0) {
            return new AttributeModifierSpec(target, AttributeModifier.Operation.ADDITION);
        }

        double multiplyTotalAmount = (target / reference) - 1.0;
        return new AttributeModifierSpec(multiplyTotalAmount, AttributeModifier.Operation.MULTIPLY_TOTAL);
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
