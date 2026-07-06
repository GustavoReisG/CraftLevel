package com.grgs.craftlevel.scaling;

/** Declaração de um escalador em config.json → scaling.{nome}. */
public record ScalingDefinition(
        double baseValue,
        double scaling,
        ScalingMode scalingMode,
        ApplyMode applyMode,
        /** Se true, aplica o escalador sobre o valor final do attribute (inclui modifiers de itens, etc.). */
        boolean includeModifiers
) {
    public ScalingDefinition(double baseValue, double scaling, ScalingMode scalingMode, ApplyMode applyMode) {
        this(baseValue, scaling, scalingMode, applyMode, false);
    }
}
