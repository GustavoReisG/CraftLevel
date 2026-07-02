package com.grgs.craftlevel.scaling;

/** Declaração de um escalador em config.json → scaling.{nome}. */
public record ScalingDefinition(
        double baseValue,
        double scaling,
        ScalingMode scalingMode,
        ApplyMode applyMode
) {}
