package com.grgs.craftlevel.economy;

import com.grgs.craftlevel.capability.PlayerEmeraldBankProvider;
import com.grgs.craftlevel.item.EmeraldStorageItem;
import com.grgs.craftlevel.item.EmeraldStorageItems;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncEmeraldBankPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/** Preenche e devolve Storage items do emerald bank nos slots de pagamento do villager. */
public final class BankPaymentService {

    public static final int PAYMENT_BUFFER_SIZE = 64;

    private BankPaymentService() {}

    /** Ao selecionar troca: devolve Storage dos slots e preenche buffer se o custo for Storage. */
    public static void refillPaymentFromBank(Player player, MerchantMenu menu, MerchantOffer offer) {
        if (offer == null || player.level().isClientSide()) {
            return;
        }

        MerchantContainer container = MerchantMenuHelper.getTradeContainer(menu);
        if (container == null) {
            return;
        }

        returnPaymentStorageToBank(player, container);
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

    /** Devolve Storage dos slots de pagamento para o bank. */
    public static void returnPaymentStorageToBank(Player player, MerchantContainer container) {
        if (player.level().isClientSide() || container == null) {
            return;
        }

        depositSlotStorage(player, container, 0);
        depositSlotStorage(player, container, 1);
        syncBank(player);
    }

    /**
     * Converte todos os Storage do inventário, cursor e slots de pagamento em saldo do bank.
     * Chamado ao fechar o MerchantMenu.
     */
    public static void depositAllStorageOnClose(Player player, MerchantMenu menu) {
        if (player.level().isClientSide()) {
            return;
        }

        MerchantContainer container = MerchantMenuHelper.getTradeContainer(menu);
        returnPaymentStorageToBank(player, container);

        ItemStack carried = menu.getCarried();
        if (EmeraldStorageItems.isStorage(carried)) {
            depositStorageStack(player, carried);
            menu.setCarried(ItemStack.EMPTY);
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!EmeraldStorageItems.isStorage(stack)) {
                continue;
            }
            depositStorageStack(player, stack);
            player.getInventory().setItem(i, ItemStack.EMPTY);
        }

        syncBank(player);
    }

    private static void fillPaymentBuffer(
            Player player,
            MerchantMenu menu,
            MerchantContainer container,
            MerchantOffer offer
    ) {
        fillStorageSlot(player, menu, container, 0, offer.getCostA());
        fillStorageSlot(player, menu, container, 1, offer.getCostB());
        menu.broadcastChanges();
        syncBank(player);
    }

    private static void fillStorageSlot(
            Player player,
            MerchantMenu menu,
            MerchantContainer container,
            int slot,
            ItemStack cost
    ) {
        if (!EmeraldStorageItems.isStorage(cost)) {
            depositSlotStorage(player, container, slot);
            return;
        }

        Item costItem = cost.getItem();
        int unitValue = EmeraldStorageItems.unitValue(costItem);
        if (unitValue <= 0) {
            return;
        }

        ItemStack currentStack = container.getItem(slot);
        if (!currentStack.isEmpty() && !currentStack.is(costItem)) {
            if (EmeraldStorageItems.isStorage(currentStack)) {
                depositStorageStack(player, currentStack);
            }
            applyStorageStack(menu, container, slot, costItem, 0);
            currentStack = ItemStack.EMPTY;
        }

        int current = currentStack.is(costItem) ? currentStack.getCount() : 0;
        int target = PAYMENT_BUFFER_SIZE;

        if (current > target) {
            int excess = current - target;
            removeFromSlot(container, slot, excess);
            depositValue(player, (long) excess * unitValue);
            current = target;
        }

        int needed = target - current;
        if (needed > 0) {
            current += pullStorageFromInventory(player, costItem, needed);
            needed = target - current;
        }

        if (needed > 0) {
            current += pullStorageFromBank(player, costItem, unitValue, needed);
        }

        applyStorageStack(menu, container, slot, costItem, current);
    }

    private static void applyStorageStack(
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

    private static int pullStorageFromInventory(Player player, Item item, int needed) {
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

    private static int pullStorageFromBank(Player player, Item item, int unitValue, int needed) {
        if (needed <= 0 || unitValue <= 0 || !(item instanceof EmeraldStorageItem)) {
            return 0;
        }
        return player.getCapability(PlayerEmeraldBankProvider.EMERALD_BANK)
                .map(bank -> {
                    long affordable = bank.getBalance() / unitValue;
                    int take = (int) Math.min(needed, affordable);
                    if (take <= 0) {
                        return 0;
                    }
                    long cost = (long) take * unitValue;
                    if (!bank.tryRemoveEmeralds(cost)) {
                        return 0;
                    }
                    return take;
                })
                .orElse(0);
    }

    private static void depositSlotStorage(Player player, MerchantContainer container, int slot) {
        ItemStack stack = container.getItem(slot);
        if (!EmeraldStorageItems.isStorage(stack)) {
            return;
        }

        depositStorageStack(player, stack);
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

    private static void depositStorageStack(Player player, ItemStack stack) {
        long value = EmeraldStorageItems.stackValue(stack);
        if (value > 0) {
            depositValue(player, value);
        }
    }

    private static void depositValue(Player player, long amount) {
        if (amount <= 0) {
            return;
        }
        player.getCapability(PlayerEmeraldBankProvider.EMERALD_BANK)
                .ifPresent(bank -> bank.addEmeralds(amount));
    }

    private static void syncBank(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(serverPlayer, new SyncEmeraldBankPacket(EconomyService.getBankBalance(player)));
        }
    }
}
