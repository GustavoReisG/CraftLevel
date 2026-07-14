package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.economy.MerchantMenuHelper;
import com.grgs.craftlevel.villager.VillagerTradeValidator;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bloqueia retirada do resultado (incluindo shift-click) quando o villager não tem saldo. */
@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {

    private static final int MERCHANT_RESULT_SLOT = 2;

    @Shadow @Final private Merchant merchant;
    @Shadow @Final private MerchantContainer slots;

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void craftlevel$blockRemove(int amount, CallbackInfoReturnable<ItemStack> cir) {
        if (!shouldBlockTrade(null)) {
            return;
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }

    private boolean shouldBlockTrade(Player player) {
        if (player != null && player.level().isClientSide()) {
            return false;
        }
        if (!(merchant instanceof AbstractVillager villager)) {
            return false;
        }
        if (slots == null || slots.getItem(MERCHANT_RESULT_SLOT).isEmpty()) {
            return false;
        }

        MerchantOffer offer = slots.getActiveOffer();
        if (offer == null && player != null && player.containerMenu instanceof MerchantMenu menu) {
            offer = MerchantMenuHelper.getSelectedOffer(menu);
        }
        if (offer == null) {
            return false;
        }

        return !VillagerTradeValidator.canCompleteTrade(villager, offer);
    }
}
