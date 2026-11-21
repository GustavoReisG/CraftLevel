package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.client.MobPortraitCapturer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clones do card não usam armature do Epic Fight → LivingEntityRenderer vanilla.
 */
@Mixin(targets = "yesman.epicfight.client.events.engine.RenderEngine", remap = false)
public final class EpicFightPortraitRendererMixin {

    private EpicFightPortraitRendererMixin() {}

    @Inject(method = "hasRendererFor", at = @At("HEAD"), cancellable = true, remap = false)
    private void craftlevel$noArmatureForPortrait(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living && MobPortraitCapturer.isPortraitPreview(living)) {
            cir.setReturnValue(false);
        }
    }
}
