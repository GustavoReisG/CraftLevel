package com.grgs.craftlevel.menu;

import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.economy.EconomyService;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncEmeraldWalletPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Slot fantasma que consome esmeraldas e credita a wallet. */
public class WalletDepositSlot extends Slot {

    private final Player player;

    public WalletDepositSlot(Player player, int slot, int x, int y) {
        super(new EmptyContainer(), slot, x, y);
        this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return EmeraldDepositHelper.isDepositable(stack);
    }

    @Override
    public void set(ItemStack stack) {
        int packs = EmeraldDepositHelper.packValue(stack);
        if (packs > 0) {
            player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET)
                    .ifPresent(wallet -> wallet.addEmeralds(packs));
            if (player instanceof ServerPlayer serverPlayer) {
                PacketHandler.sendToPlayer(serverPlayer, new SyncEmeraldWalletPacket(EconomyService.getWalletBalance(player)));
            }
        }
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean hasItem() {
        return false;
    }

    @Override
    public void setChanged() {
        // Slot fantasma — nada a persistir.
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
