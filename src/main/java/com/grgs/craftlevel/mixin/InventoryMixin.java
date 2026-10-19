package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.arena.ArenaDimension;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Força 1 ponto de durabilidade por peça, ignorando o cálculo vanilla (dano ÷ 4). Na arena, zero. */
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
        Inventory inventory = (Inventory) (Object) this;
        if (inventory.player.level().dimension().equals(ArenaDimension.LEVEL)) {
            return 0;
        }
        return 1;
    }
}
