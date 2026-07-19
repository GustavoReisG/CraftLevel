package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.economy.EmeraldPaymentService;
import com.grgs.craftlevel.economy.MerchantMenuHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Devolve esmeraldas do campo de troca para a wallet antes do vanilla entregar ao inventário.
 * O inject fica em {@link AbstractContainerMenu#removed} porque {@link MerchantMenu} não sobrescreve o método.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class MerchantMenuMixin {

    @Inject(method = "removed(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"))
    private void craftlevel$returnEmeraldsToWallet(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) {
            return;
        }
        if (!((Object) this instanceof MerchantMenu menu)) {
            return;
        }
        var container = MerchantMenuHelper.getTradeContainer(menu);
        EmeraldPaymentService.returnPaymentEmeraldsToWallet(player, container);
    }
}
