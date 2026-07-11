package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.economy.EmeraldPaymentService;
import com.grgs.craftlevel.economy.MerchantMenuHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Devolve esmeraldas do campo de troca para a wallet antes do vanilla entregar ao inventário. */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {

    @Inject(method = "removed", at = @At("HEAD"))
    private void craftlevel$returnEmeraldsToWallet(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) {
            return;
        }
        MerchantMenu menu = (MerchantMenu) (Object) this;
        var container = MerchantMenuHelper.getTradeContainer(menu);
        EmeraldPaymentService.returnPaymentEmeraldsToWallet(player, container);
    }
}
