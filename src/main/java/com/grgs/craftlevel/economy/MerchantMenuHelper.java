package com.grgs.craftlevel.economy;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * Acesso ao MerchantMenu via reflexão.
 * Usa nomes SRG para funcionar no runtime do modpack (campos ofuscados).
 */
public final class MerchantMenuHelper {

    // MerchantMenu.trader / tradeContainer
    private static final Field TRADER_FIELD =
            ObfuscationReflectionHelper.findField(MerchantMenu.class, "f_40027_");
    private static final Field TRADE_CONTAINER_FIELD =
            ObfuscationReflectionHelper.findField(MerchantMenu.class, "f_40028_");
    // MerchantContainer.selectionHint
    private static final Field SELECTION_HINT_FIELD =
            ObfuscationReflectionHelper.findField(MerchantContainer.class, "f_40000_");

    private MerchantMenuHelper() {}

    public static Merchant getTrader(MerchantMenu menu) {
        try {
            Object value = TRADER_FIELD.get(menu);
            return value instanceof Merchant merchant ? merchant : null;
        } catch (IllegalAccessException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to access MerchantMenu.trader", e);
            return null;
        }
    }

    public static MerchantContainer getTradeContainer(MerchantMenu menu) {
        try {
            Object value = TRADE_CONTAINER_FIELD.get(menu);
            return value instanceof MerchantContainer container ? container : null;
        } catch (IllegalAccessException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to access MerchantMenu.tradeContainer", e);
            return null;
        }
    }

    public static int getSelectionHint(MerchantContainer container) {
        try {
            return SELECTION_HINT_FIELD.getInt(container);
        } catch (IllegalAccessException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to read MerchantContainer.selectionHint", e);
            return -1;
        }
    }

    public static MerchantOffer getSelectedOffer(MerchantMenu menu) {
        MerchantContainer container = getTradeContainer(menu);
        if (container != null) {
            MerchantOffer active = container.getActiveOffer();
            if (active != null) {
                return active;
            }
        }

        MerchantOffers offers = menu.getOffers();
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
