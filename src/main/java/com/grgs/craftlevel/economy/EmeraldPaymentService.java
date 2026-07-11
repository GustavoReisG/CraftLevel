package com.grgs.craftlevel.economy;

import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncEmeraldWalletPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/** Preenche e devolve esmeraldas da wallet/inventário nos slots de pagamento do villager. */
public final class EmeraldPaymentService {

    public static final int PAYMENT_BUFFER_SIZE = 64;

    private EmeraldPaymentService() {}

    /** Ao selecionar troca: devolve esmeraldas do campo e preenche buffer de 64. */
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

    /** Após compra: completa buffer até 64 sem limpar o que sobrou no campo. */
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

    /** Devolve todas as esmeraldas dos slots de pagamento para a wallet. */
    public static void returnPaymentEmeraldsToWallet(Player player, MerchantContainer container) {
        if (player.level().isClientSide() || container == null) {
            return;
        }

        depositSlotEmeralds(player, container, 0);
        depositSlotEmeralds(player, container, 1);
        syncWallet(player);
    }

    private static void fillPaymentBuffer(
            Player player,
            MerchantMenu menu,
            MerchantContainer container,
            MerchantOffer offer
    ) {
        fillEmeraldSlot(player, menu, container, 0, offer.getCostA());
        fillEmeraldSlot(player, menu, container, 1, offer.getCostB());
        menu.broadcastChanges();
        syncWallet(player);
    }

    private static void fillEmeraldSlot(
            Player player,
            MerchantMenu menu,
            MerchantContainer container,
            int slot,
            ItemStack cost
    ) {
        if (!cost.is(Items.EMERALD)) {
            depositSlotEmeralds(player, container, slot);
            return;
        }

        int current = emeraldCount(container, slot);
        int target = PAYMENT_BUFFER_SIZE;

        if (current > target) {
            int excess = current - target;
            removeEmeraldsFromSlot(container, slot, excess);
            depositEmeralds(player, excess);
            current = target;
        }

        int needed = target - current;
        if (needed > 0) {
            current += pullEmeraldsFromInventory(player, needed);
            needed = target - current;
        }

        if (needed > 0) {
            current += pullEmeraldsFromWallet(player, needed);
        }

        applyEmeraldStack(menu, container, slot, current);
    }

    private static void applyEmeraldStack(
            MerchantMenu menu,
            MerchantContainer container,
            int slot,
            int count
    ) {
        ItemStack stack = count > 0 ? new ItemStack(Items.EMERALD, count) : ItemStack.EMPTY;
        container.setItem(slot, stack);
        for (Slot menuSlot : menu.slots) {
            if (menuSlot.container == container && menuSlot.getContainerSlot() == slot) {
                menuSlot.set(stack);
                return;
            }
        }
    }

    private static int emeraldCount(MerchantContainer container, int slot) {
        ItemStack stack = container.getItem(slot);
        return stack.is(Items.EMERALD) ? stack.getCount() : 0;
    }

    private static void removeEmeraldsFromSlot(MerchantContainer container, int slot, int amount) {
        ItemStack stack = container.getItem(slot);
        if (!stack.is(Items.EMERALD) || amount <= 0) {
            return;
        }
        stack.shrink(amount);
        if (stack.isEmpty()) {
            container.setItem(slot, ItemStack.EMPTY);
        }
    }

    private static int pullEmeraldsFromInventory(Player player, int needed) {
        int pulled = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && pulled < needed; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(Items.EMERALD)) {
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

    private static int pullEmeraldsFromWallet(Player player, int needed) {
        if (needed <= 0) {
            return 0;
        }
        return player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET)
                .map(wallet -> {
                    int available = (int) Math.min(needed, wallet.getBalance());
                    if (available <= 0 || !wallet.tryRemoveEmeralds(available)) {
                        return 0;
                    }
                    return available;
                })
                .orElse(0);
    }

    private static void depositSlotEmeralds(Player player, MerchantContainer container, int slot) {
        ItemStack stack = container.getItem(slot);
        if (!stack.is(Items.EMERALD) || stack.isEmpty()) {
            return;
        }

        depositEmeralds(player, stack.getCount());
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

    private static void depositEmeralds(Player player, int count) {
        if (count <= 0) {
            return;
        }
        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET)
                .ifPresent(wallet -> wallet.addEmeralds(count));
    }

    private static void syncWallet(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(serverPlayer, new SyncEmeraldWalletPacket(EconomyService.getWalletBalance(player)));
        }
    }
}
