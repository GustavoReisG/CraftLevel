package com.grgs.craftlevel.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sincroniza Glory Kills e Skill Points para o cliente. */
public class SyncPlayerGloryPacket {

    private final int gloryKills;
    private final int skillPoints;

    public SyncPlayerGloryPacket(int gloryKills, int skillPoints) {
        this.gloryKills = gloryKills;
        this.skillPoints = skillPoints;
    }

    public static void encode(SyncPlayerGloryPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.gloryKills);
        buf.writeVarInt(packet.skillPoints);
    }

    public static SyncPlayerGloryPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerGloryPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SyncPlayerGloryPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientGloryState.gloryKills = packet.gloryKills;
            ClientGloryState.skillPoints = packet.skillPoints;
            com.grgs.craftlevel.client.PlayerGloryHud.updateGlory(packet.gloryKills);
        });
        ctx.get().setPacketHandled(true);
    }

    /** Estado client-side mínimo. */
    public static final class ClientGloryState {
        public static int gloryKills = -1;
        public static int skillPoints = 0;
    }
}
