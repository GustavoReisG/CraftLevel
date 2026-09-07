package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.potion.PotionLevelText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tooltips de poção: níveis &gt; 5 usam número arábico
 * ({@code potion.level.N} / {@code potion.potency.N}).
 */
@Mixin(PotionUtils.class)
public class PotionUtilsMixin {

    @Redirect(
            method = {
                    "addPotionTooltip(Ljava/util/List;Ljava/util/List;F)V",
                    "addPotionTooltip(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;F)V"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
            )
    )
    private static MutableComponent craftlevel$arabicPotionLevels(String key) {
        return PotionLevelText.remapTranslatable(key);
    }
}
