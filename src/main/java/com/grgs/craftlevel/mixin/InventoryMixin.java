package com.grgs.craftlevel.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Força 1 ponto de durabilidade por peça, ignorando o cálculo vanilla (dano ÷ 4). */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @ModifyArg(
            method = "hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F[I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"
            ),
            index = 0
    )
    private int craftlevel$fixedArmorBreak(int amount) {
        return 1;
    }
}
