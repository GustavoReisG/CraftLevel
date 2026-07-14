package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.villager.VillagerTradeValidator;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Impede que o resultado apareça quando o villager não tem saldo para pagar esmeraldas. */
@Mixin(MerchantContainer.class)
public abstract class MerchantContainerMixin {

    private static final int RESULT_SLOT = 2;

    @Shadow @Final private Merchant merchant;

    @Shadow public abstract MerchantOffer getActiveOffer();

    @Shadow public abstract void setItem(int slot, ItemStack stack);

    @Inject(method = "updateSellItem", at = @At("TAIL"))
    private void craftlevel$clearBlockedResult(CallbackInfo ci) {
        if (!(merchant instanceof AbstractVillager villager)) {
            return;
        }

        MerchantOffer offer = getActiveOffer();
        if (offer == null || VillagerTradeValidator.canCompleteTrade(villager, offer)) {
            return;
        }

        setItem(RESULT_SLOT, ItemStack.EMPTY);
    }
}
