package com.grgs.craftlevel.network;

import com.grgs.craftlevel.client.EmeraldWalletScreen;
import com.grgs.craftlevel.client.PlayerEmeraldHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sincroniza saldo da wallet para o cliente. */
public class SyncEmeraldWalletPacket {

    private final long balance;

    public SyncEmeraldWalletPacket(long balance) {
        this.balance = balance;
    }

    public static void encode(SyncEmeraldWalletPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.balance);
    }

    public static SyncEmeraldWalletPacket decode(FriendlyByteBuf buf) {
        return new SyncEmeraldWalletPacket(buf.readLong());
    }

    public static void handle(SyncEmeraldWalletPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEmeraldHud.updateBalance(packet.balance);
            EmeraldWalletScreen.updateBalance(packet.balance);
        });
        ctx.get().setPacketHandled(true);
    }
}
