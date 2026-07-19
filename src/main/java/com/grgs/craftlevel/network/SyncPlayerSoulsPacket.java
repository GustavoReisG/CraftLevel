package com.grgs.craftlevel.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sincroniza souls do player para o cliente (HUD futuro). */
public class SyncPlayerSoulsPacket {

    private final int souls;

    public SyncPlayerSoulsPacket(int souls) {
        this.souls = souls;
    }

    public static void encode(SyncPlayerSoulsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.souls);
    }

    public static SyncPlayerSoulsPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerSoulsPacket(buf.readVarInt());
    }

    public static void handle(SyncPlayerSoulsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientSoulsState.souls = packet.souls;
            com.grgs.craftlevel.client.PlayerSoulsHud.updateSouls(packet.souls);
        });
        ctx.get().setPacketHandled(true);
    }

    /** Estado client-side mínimo. */
    public static final class ClientSoulsState {
        public static int souls = -1;
    }
}
