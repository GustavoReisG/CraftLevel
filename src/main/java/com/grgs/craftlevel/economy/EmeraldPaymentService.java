package com.grgs.craftlevel.economy;

import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.item.ModItems;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncEmeraldWalletPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Preenche e devolve esmeraldas / emerald block / compressed emerald block
 * da wallet e inventário nos slots de pagamento do villager.
 */
public final class EmeraldPaymentService {

    public static final int PAYMENT_BUFFER_SIZE = 64;
    public static final int PACKS_PER_BLOCK = 9;
    public static final int PACKS_PER_COMPRESSED_BLOCK = 81;

    private EmeraldPaymentService() {}

    /** Ao selecionar troca: devolve pagamento da wallet e preenche buffer. */
    public static void refillPaymentFromWallet(Player player, MerchantMenu menu, MerchantOffer offer) {
        if (offer == null || player.level().isClientSide()) {
            return;
        }

        MerchantContainer container = MerchantMenuHelper.getTradeContainer(menu);
        if (container == null) {
            return;
        }

        returnPaymentEmeraldsToWallet(player, container);
        fillPaymentBuffer(player, menu, container, offer);
    }

    /** Após compra: completa buffer sem limpar o que sobrou no campo. */
    public static void refillPaymentBuffer(Player player, MerchantMenu menu, MerchantOffer offer) {
        if (offer == null || player.level().isClientSide()) {
            return;
        }

        MerchantContainer container = MerchantMenuHelper.getTradeContainer(menu);
        if (container == null) {
            return;
        }

        fillPaymentBuffer(player, menu, container, offer);
    }

    /** Devolve emerald / block / compressed dos slots de pagamento para a wallet. */
    public static void returnPaymentEmeraldsToWallet(Player player, MerchantContainer container) {
        if (player.level().isClientSide() || container == null) {
            return;
        }

        depositSlotCurrency(player, container, 0);
        depositSlotCurrency(player, container, 1);
        syncWallet(player);
    }

    private static void fillPaymentBuffer(
            Player player,
            MerchantMenu menu,
            MerchantContainer container,
            MerchantOffer offer
    ) {
        fillCurrencySlot(player, menu, container, 0, offer.getCostA());
        fillCurrencySlot(player, menu, container, 1, offer.getCostB());
        menu.broadcastChanges();
        syncWallet(player);
    }

    private static void fillCurrencySlot(
            Player player,
            MerchantMenu menu,
            MerchantContainer container,
            int slot,
            ItemStack cost
    ) {
        int unitPacks = unitPackValue(cost);
        if (unitPacks <= 0) {
            depositSlotCurrency(player, container, slot);
            return;
        }

        Item costItem = cost.getItem();
        ItemStack currentStack = container.getItem(slot);
        if (!currentStack.isEmpty() && !currentStack.is(costItem)) {
            if (unitPackValue(currentStack) > 0) {
                depositCurrencyStack(player, currentStack);
            }
            applyCurrencyStack(menu, container, slot, costItem, 0);
            currentStack = ItemStack.EMPTY;
        }

        int current = currentStack.is(costItem) ? currentStack.getCount() : 0;
        int target = PAYMENT_BUFFER_SIZE;

        if (current > target) {
            int excess = current - target;
            removeFromSlot(container, slot, excess);
            depositCurrency(player, (long) excess * unitPacks);
            current = target;
        }

        int needed = target - current;
        if (needed > 0) {
            current += pullFromInventory(player, costItem, needed);
            needed = target - current;
        }

        if (needed > 0) {
            current += pullFromWallet(player, unitPacks, needed);
        }

        applyCurrencyStack(menu, container, slot, costItem, current);
    }

    private static int unitPackValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(Items.EMERALD)) {
            return 1;
        }
        if (stack.is(Items.EMERALD_BLOCK)) {
            return PACKS_PER_BLOCK;
        }
        if (stack.is(ModItems.COMPRESSED_EMERALD_BLOCK.get())) {
            return PACKS_PER_COMPRESSED_BLOCK;
        }
        return 0;
    }

    private static void applyCurrencyStack(
            MerchantMenu menu,
            MerchantContainer container,
            int slot,
            Item item,
            int count
    ) {
        ItemStack stack = count > 0 ? new ItemStack(item, count) : ItemStack.EMPTY;
        container.setItem(slot, stack);
        for (Slot menuSlot : menu.slots) {
            if (menuSlot.container == container && menuSlot.getContainerSlot() == slot) {
                menuSlot.set(stack);
                return;
            }
        }
    }

    private static void removeFromSlot(MerchantContainer container, int slot, int amount) {
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty() || amount <= 0) {
            return;
        }
        stack.shrink(amount);
        if (stack.isEmpty()) {
            container.setItem(slot, ItemStack.EMPTY);
        }
    }

    private static int pullFromInventory(Player player, Item item, int needed) {
        int pulled = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && pulled < needed; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(item)) {
                continue;
            }
            int take = Math.min(stack.getCount(), needed - pulled);
            stack.shrink(take);
            if (stack.isEmpty()) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
            pulled += take;
        }
        return pulled;
    }

    private static int pullFromWallet(Player player, int unitPacks, int needed) {
        if (needed <= 0 || unitPacks <= 0) {
            return 0;
        }
        return player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET)
                .map(wallet -> {
                    long affordable = wallet.getBalance() / unitPacks;
                    int take = (int) Math.min(needed, affordable);
                    if (take <= 0) {
                        return 0;
                    }
                    long cost = (long) take * unitPacks;
                    if (!wallet.tryRemoveEmeralds(cost)) {
                        return 0;
                    }
                    return take;
                })
                .orElse(0);
    }

    private static void depositSlotCurrency(Player player, MerchantContainer container, int slot) {
        ItemStack stack = container.getItem(slot);
        if (unitPackValue(stack) <= 0) {
            return;
        }

        depositCurrencyStack(player, stack);
        container.setItem(slot, ItemStack.EMPTY);
        if (player.containerMenu instanceof MerchantMenu menu) {
            for (Slot menuSlot : menu.slots) {
                if (menuSlot.container == container && menuSlot.getContainerSlot() == slot) {
                    menuSlot.set(ItemStack.EMPTY);
                    break;
                }
            }
        }
    }

    private static void depositCurrencyStack(Player player, ItemStack stack) {
        int unit = unitPackValue(stack);
        if (unit <= 0) {
            return;
        }
        depositCurrency(player, (long) unit * stack.getCount());
    }

    private static void depositCurrency(Player player, long amount) {
        if (amount <= 0) {
            return;
        }
        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET)
                .ifPresent(wallet -> wallet.addEmeralds(amount));
    }

    private static void syncWallet(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(serverPlayer, new SyncEmeraldWalletPacket(EconomyService.getWalletBalance(player)));
        }
    }
}
