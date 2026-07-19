package com.grgs.craftlevel.mixin;

import com.grgs.craftlevel.villager.VillagerTradeValidator;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/** Impede que o resultado apareça quando o villager não tem saldo para pagar esmeraldas. */
@Mixin(MerchantContainer.class)
public abstract class MerchantContainerMixin {

    private static final int RESULT_SLOT = 2;
    private static final Field MERCHANT_FIELD =
            ObfuscationReflectionHelper.findField(MerchantContainer.class, "f_39997_");

    @Inject(method = "updateSellItem()V", at = @At("TAIL"))
    private void craftlevel$clearBlockedResult(CallbackInfo ci) {
        MerchantContainer self = (MerchantContainer) (Object) this;
        MerchantOffer offer = self.getActiveOffer();
        if (offer == null) {
            return;
        }

        AbstractVillager villager = asVillager(self);
        if (villager == null || VillagerTradeValidator.canCompleteTrade(villager, offer)) {
            return;
        }

        self.setItem(RESULT_SLOT, ItemStack.EMPTY);
    }

    @Nullable
    private static AbstractVillager asVillager(MerchantContainer container) {
        try {
            Object merchant = MERCHANT_FIELD.get(container);
            return merchant instanceof AbstractVillager villager ? villager : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
