package com.grgs.craftlevel.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

/** Soft-detect Iris/Oculus (sem dependência compile-time). */
@OnlyIn(Dist.CLIENT)
final class ShaderCompat {

    private static final boolean OCULUS_OR_IRIS =
            ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");

    private ShaderCompat() {}

    /** Stencil no FB principal conflita com Oculus/Iris — usar só scissor. */
    static boolean avoidStencil() {
        return OCULUS_OR_IRIS;
    }
}
