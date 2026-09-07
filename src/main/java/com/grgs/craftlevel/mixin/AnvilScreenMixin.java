package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Esconde o ícone/texto "Too Expensive!" quando o cap da bigorna está desligado. */
@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {

    @Inject(method = "renderErrorIcon", at = @At("HEAD"), cancellable = true)
    private void craftlevel$skipTooExpensiveIcon(CallbackInfo ci) {
        if (ConfigParser.anvilUncappedCost) {
            ci.cancel();
        }
    }
}
