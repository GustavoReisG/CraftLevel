package com.grgs.craftlevel.network;

import com.grgs.craftlevel.client.CraftLevelHUD;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPlayerLevelPacket {

    private final int level;
    private final int lp;
    private final int lpNeeded;

    public SyncPlayerLevelPacket(int level, int lp, int lpNeeded) {
        this.level = level;
        this.lp = lp;
        this.lpNeeded = lpNeeded;
    }

    // Serializa para a rede
    public static void encode(SyncPlayerLevelPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.level);
        buf.writeInt(packet.lp);
        buf.writeInt(packet.lpNeeded);
    }

    // Desserializa da rede
    public static SyncPlayerLevelPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerLevelPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    // Recebido no cliente
    public static void handle(SyncPlayerLevelPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CraftLevelHUD.updateCache(packet.lp, packet.lpNeeded, packet.level);
        });
        ctx.get().setPacketHandled(true);
    }
}