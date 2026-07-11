package com.grgs.craftlevel.network;

import com.grgs.craftlevel.client.VillagerTradeHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sincroniza saldo de esmeraldas do villager para o cliente. */
public class SyncVillagerBalancePacket {

    private final int balance;

    public SyncVillagerBalancePacket(int balance) {
        this.balance = balance;
    }

    public static void encode(SyncVillagerBalancePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.balance);
    }

    public static SyncVillagerBalancePacket decode(FriendlyByteBuf buf) {
        return new SyncVillagerBalancePacket(buf.readInt());
    }

    public static void handle(SyncVillagerBalancePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> VillagerTradeHud.updateBalance(packet.balance));
        ctx.get().setPacketHandled(true);
    }
}
