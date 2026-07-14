package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.economy.MerchantMenuHelper;
import com.grgs.craftlevel.villager.VillagerTradeValidator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Bloqueia retirada do resultado quando o villager não tem saldo suficiente. */
@Mixin(Slot.class)
public abstract class MerchantTradeSlotMixin {

    private static final int MERCHANT_RESULT_SLOT = 2;

    @Shadow @Final public Container container;
    @Shadow public abstract int getContainerSlot();

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void craftlevel$validateVillagerTrade(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(player.containerMenu instanceof MerchantMenu menu)) {
            return;
        }
        if (!(MerchantMenuHelper.getTrader(menu) instanceof AbstractVillager villager)) {
            return;
        }

        MerchantContainer tradeContainer = MerchantMenuHelper.getTradeContainer(menu);
        if (tradeContainer == null || container != tradeContainer || getContainerSlot() != MERCHANT_RESULT_SLOT) {
            return;
        }

        MerchantOffer offer = MerchantMenuHelper.getSelectedOffer(menu);
        if (!VillagerTradeValidator.canCompleteTrade(villager, offer)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tryRemove", at = @At("HEAD"), cancellable = true)
    private void craftlevel$blockTryRemove(
            int count,
            int decrement,
            Player player,
            CallbackInfoReturnable<Optional<ItemStack>> cir
    ) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }
        if (!(player.containerMenu instanceof MerchantMenu menu)) {
            return;
        }
        if (!(MerchantMenuHelper.getTrader(menu) instanceof AbstractVillager villager)) {
            return;
        }

        MerchantContainer tradeContainer = MerchantMenuHelper.getTradeContainer(menu);
        if (tradeContainer == null || container != tradeContainer || getContainerSlot() != MERCHANT_RESULT_SLOT) {
            return;
        }

        MerchantOffer offer = MerchantMenuHelper.getSelectedOffer(menu);
        if (!VillagerTradeValidator.canCompleteTrade(villager, offer)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
