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
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/** Bloqueia retirada do resultado (incluindo shift-click) quando o villager não tem saldo. */
@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {

    private static final int MERCHANT_RESULT_SLOT = 2;
    private static final Field MERCHANT_FIELD =
            ObfuscationReflectionHelper.findField(MerchantResultSlot.class, "f_40081_");
    private static final Field SLOTS_FIELD =
            ObfuscationReflectionHelper.findField(MerchantResultSlot.class, "f_40078_");

    @Inject(method = "remove(I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void craftlevel$blockRemove(int amount, CallbackInfoReturnable<ItemStack> cir) {
        if (!shouldBlockTrade(null)) {
            return;
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }

    private boolean shouldBlockTrade(@Nullable Player player) {
        if (player != null && player.level().isClientSide()) {
            return false;
        }

        Merchant merchant = getMerchant();
        MerchantContainer slots = getSlots();
        if (!(merchant instanceof AbstractVillager villager) || slots == null) {
            return false;
        }
        if (slots.getItem(MERCHANT_RESULT_SLOT).isEmpty()) {
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

    @Nullable
    private Merchant getMerchant() {
        try {
            Object value = MERCHANT_FIELD.get(this);
            return value instanceof Merchant merchant ? merchant : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @Nullable
    private MerchantContainer getSlots() {
        try {
            Object value = SLOTS_FIELD.get(this);
            return value instanceof MerchantContainer container ? container : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
