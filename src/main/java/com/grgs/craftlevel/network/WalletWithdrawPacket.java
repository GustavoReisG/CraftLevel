package com.grgs.craftlevel.network;

import com.grgs.craftlevel.menu.EmeraldWalletMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Ação de saque na wallet (cursor ou inventário). */
public class WalletWithdrawPacket {

    public enum Action {
        TO_CURSOR,
        TO_INVENTORY
    }

    private final Action action;

    public WalletWithdrawPacket(Action action) {
        this.action = action;
    }

    public static void encode(WalletWithdrawPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
    }

    public static WalletWithdrawPacket decode(FriendlyByteBuf buf) {
        return new WalletWithdrawPacket(buf.readEnum(Action.class));
    }

    public static void handle(WalletWithdrawPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            AbstractContainerMenu menu = player.containerMenu;
            if (!(menu instanceof EmeraldWalletMenu walletMenu)) {
                return;
            }

            if (packet.action == Action.TO_CURSOR) {
                walletMenu.withdrawToCursor();
            } else {
                walletMenu.withdrawToInventory();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
