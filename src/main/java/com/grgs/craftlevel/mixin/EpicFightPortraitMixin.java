package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.client.MobPortraitCapturer;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancela o handler do Epic Fight no portrait — senão ele cancela o vanilla e o
 * armature some com Oculus (itens ainda aparecem).
 */
@Mixin(targets = "yesman.epicfight.client.events.engine.RenderEngine$Events", remap = false)
public final class EpicFightPortraitMixin {

    private EpicFightPortraitMixin() {}

    @Inject(method = "renderLivingEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void craftlevel$vanillaPortraitOnly(RenderLivingEvent.Pre<?, ?> event, CallbackInfo ci) {
        if (MobPortraitCapturer.isPortraitPreview(event.getEntity())) {
            ci.cancel();
        }
    }
}
