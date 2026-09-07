package com.grgs.craftlevel.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Níveis 1–10 continuam em romano (vanilla). Acima de 10 usa número arábico
 * em vez de {@code enchantment.level.N} sem tradução.
 */
@Mixin(Enchantment.class)
public class EnchantmentMixin {

    private static final int ROMAN_MAX = 10;

    @Inject(method = "getFullname", at = @At("HEAD"), cancellable = true)
    private void craftlevel$arabicLevelsAboveTen(int level, CallbackInfoReturnable<Component> cir) {
        if (level <= ROMAN_MAX) {
            return;
        }

        Enchantment self = (Enchantment) (Object) this;
        MutableComponent name = Component.translatable(self.getDescriptionId());
        if (self.isCurse()) {
            name.withStyle(ChatFormatting.RED);
        } else {
            name.withStyle(ChatFormatting.GRAY);
        }
        name.append(CommonComponents.SPACE).append(Component.literal(String.valueOf(level)));
        cir.setReturnValue(name);
    }
}
