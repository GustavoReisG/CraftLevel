package com.grgs.craftlevel.mixin;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Remove XP (orbs) concedido ao jogador em cada troca com merchant. */
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {

    @Redirect(
            method = "notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/npc/AbstractVillager;rewardTradeXp(Lnet/minecraft/world/item/trading/MerchantOffer;)V"
            )
    )
    private void craftlevel$skipTradeXp(AbstractVillager self, MerchantOffer offer) {
        // sem XP por troca
    }
}
