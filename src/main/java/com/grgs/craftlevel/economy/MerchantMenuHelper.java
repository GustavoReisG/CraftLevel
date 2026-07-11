package com.grgs.craftlevel.economy;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.lang.reflect.Field;

/** Acesso ao MerchantMenu via reflexão (sem mixin). */
public final class MerchantMenuHelper {

    private static Field tradeContainerField;
    private static Field traderField;

    private MerchantMenuHelper() {}

    public static Merchant getTrader(MerchantMenu menu) {
        try {
            if (traderField == null) {
                traderField = MerchantMenu.class.getDeclaredField("trader");
                traderField.setAccessible(true);
            }
            return (Merchant) traderField.get(menu);
        } catch (ReflectiveOperationException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to access MerchantMenu.trader", e);
            return null;
        }
    }

    public static MerchantContainer getTradeContainer(MerchantMenu menu) {
        try {
            if (tradeContainerField == null) {
                tradeContainerField = MerchantMenu.class.getDeclaredField("tradeContainer");
                tradeContainerField.setAccessible(true);
            }
            return (MerchantContainer) tradeContainerField.get(menu);
        } catch (ReflectiveOperationException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to access MerchantMenu.tradeContainer", e);
            return null;
        }
    }

    public static int getSelectionHint(MerchantContainer container) {
        try {
            Field field = MerchantContainer.class.getDeclaredField("selectionHint");
            field.setAccessible(true);
            return field.getInt(container);
        } catch (ReflectiveOperationException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to read MerchantContainer.selectionHint", e);
            return -1;
        }
    }

    public static MerchantOffer getSelectedOffer(MerchantMenu menu) {
        MerchantOffers offers = menu.getOffers();
        MerchantContainer container = getTradeContainer(menu);
        if (offers == null || container == null || offers.isEmpty()) {
            return null;
        }

        int index = getSelectionHint(container);
        if (index < 0 || index >= offers.size()) {
            index = 0;
        }
        return offers.get(index);
    }
}
