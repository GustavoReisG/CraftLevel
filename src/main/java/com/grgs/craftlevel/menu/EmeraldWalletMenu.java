package com.grgs.craftlevel.menu;

import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.economy.EconomyService;
import com.grgs.craftlevel.item.ModItems;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncEmeraldWalletPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

/** Menu alinhado à malha do hopper vanilla (textura emerald_wallet.png 176x133).
 * <p>
 * Malha (pixels, origem canto superior esquerdo):
 * <ul>
 *   <li>GUI: 176 x 133</li>
 *   <li>Slots: células 18x18, coluna 0 em x=8, passo 18</li>
 *   <li>Ícone saldo: 8, 6</li>
 *   <li>Depósito: 80, 20 (slot central do hopper)</li>
 *   <li>Botão saque: 103, 19 (offset +5 direita, -1 cima do depósito)</li>
 *   <li>Inventário: y=51 / 69 / 87</li>
 *   <li>Hotbar: y=109</li>
 * </ul>
 */
public class EmeraldWalletMenu extends AbstractContainerMenu {

    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 133;
    public static final int SLOT_SIZE = 18;
    public static final int SLOT_ORIGIN_X = 8;
    public static final int SLOT_STEP = 18;

    public static final int PACK_SIZE = 64;

    private static final int DEPOSIT_SLOT = 0;
    private static final int DEPOSIT_SLOT_X = 80;
    private static final int DEPOSIT_SLOT_Y = 20;
    private static final int WITHDRAW_BUTTON_OFFSET_X = 23;
    private static final int WITHDRAW_BUTTON_OFFSET_Y = -1;
    private static final int PLAYER_INV_Y = 51;
    private static final int HOTBAR_Y = 109;

    private final Player player;

    public EmeraldWalletMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.EMERALD_WALLET.get(), containerId);
        this.player = playerInventory.player;
        EconomyService.ensurePlayerEconomy(player);

        addSlot(new WalletDepositSlot(player, DEPOSIT_SLOT, DEPOSIT_SLOT_X, DEPOSIT_SLOT_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, SLOT_ORIGIN_X + col * SLOT_STEP, PLAYER_INV_Y + row * SLOT_STEP));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, SLOT_ORIGIN_X + col * SLOT_STEP, HOTBAR_Y));
        }

        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(serverPlayer, new SyncEmeraldWalletPacket(EconomyService.getWalletBalance(player)));
        }
    }

    public static int withdrawButtonScreenX(int leftPos) {
        return leftPos + DEPOSIT_SLOT_X + WITHDRAW_BUTTON_OFFSET_X;
    }

    public static int withdrawButtonScreenY(int topPos) {
        return topPos + DEPOSIT_SLOT_Y + WITHDRAW_BUTTON_OFFSET_Y;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clickPlayer) {
        if (slotId == DEPOSIT_SLOT && clickType == ClickType.PICKUP && button == 0) {
            depositFromCarried();
            return;
        }
        super.clicked(slotId, button, clickType, clickPlayer);
    }

    public void withdrawToCursor() {
        if (player.level().isClientSide()) {
            return;
        }

        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            withdrawPackToCarried();
        } else if (carried.is(Items.EMERALD)) {
            completePackOnCarried();
        }
        syncWallet();
    }

    public void withdrawToInventory() {
        if (player.level().isClientSide()) {
            return;
        }

        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(wallet -> {
            long balance = wallet.getBalance();
            if (balance <= 0) {
                return;
            }

            int amount = (int) Math.min(PACK_SIZE, balance);
            ItemStack stack = new ItemStack(Items.EMERALD, amount);
            int before = stack.getCount();
            player.getInventory().add(stack);
            int added = before - stack.getCount();
            if (added > 0) {
                wallet.tryRemoveEmeralds(added);
            }
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
        });
        syncWallet();
    }

    private void depositFromCarried() {
        ItemStack carried = getCarried();
        int packs = EmeraldDepositHelper.packValue(carried);
        if (packs <= 0) {
            return;
        }

        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(wallet -> wallet.addEmeralds(packs));
        setCarried(ItemStack.EMPTY);
        syncWallet();
    }

    private void withdrawPackToCarried() {
        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(wallet -> {
            long balance = wallet.getBalance();
            if (balance <= 0) {
                return;
            }
            int amount = (int) Math.min(PACK_SIZE, balance);
            if (wallet.tryRemoveEmeralds(amount)) {
                setCarried(new ItemStack(Items.EMERALD, amount));
            }
        });
    }

    private void completePackOnCarried() {
        ItemStack carried = getCarried();
        if (!carried.is(Items.EMERALD) || carried.getCount() >= PACK_SIZE) {
            return;
        }

        int needed = PACK_SIZE - carried.getCount();
        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(wallet -> {
            int available = (int) Math.min(needed, wallet.getBalance());
            if (available <= 0) {
                return;
            }
            if (wallet.tryRemoveEmeralds(available)) {
                carried.grow(available);
                setCarried(carried);
            }
        });
    }

    private void syncWallet() {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(serverPlayer, new SyncEmeraldWalletPacket(EconomyService.getWalletBalance(player)));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player clickPlayer, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            original = stackInSlot.copy();

            if (index == DEPOSIT_SLOT) {
                if (!moveItemStackTo(stackInSlot, 1, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (EmeraldDepositHelper.isDepositable(stackInSlot)) {
                depositEmeralds(EmeraldDepositHelper.packValue(stackInSlot));
                slot.set(ItemStack.EMPTY);
                syncWallet();
                return original;
            } else if (!moveItemStackTo(stackInSlot, 1, slots.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return original;
    }

    private void depositEmeralds(int count) {
        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(wallet -> wallet.addEmeralds(count));
    }

    @Override
    public boolean stillValid(Player clickPlayer) {
        return clickPlayer.getInventory().contains(new ItemStack(ModItems.EMERALD_WALLET.get()));
    }
}
