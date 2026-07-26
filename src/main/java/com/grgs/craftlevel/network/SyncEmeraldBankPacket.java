package com.grgs.craftlevel.network;

import com.grgs.craftlevel.client.PlayerEmeraldBankClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sincroniza saldo do emerald bank para o cliente. */
public class SyncEmeraldBankPacket {

    private final long balance;

    public SyncEmeraldBankPacket(long balance) {
        this.balance = balance;
    }

    public static void encode(SyncEmeraldBankPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.balance);
    }

    public static SyncEmeraldBankPacket decode(FriendlyByteBuf buf) {
        return new SyncEmeraldBankPacket(buf.readLong());
    }

    public static void handle(SyncEmeraldBankPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PlayerEmeraldBankClient.updateBalance(packet.balance));
        ctx.get().setPacketHandled(true);
    }
}
