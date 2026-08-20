package com.grgs.craftlevel.network;

import com.grgs.craftlevel.client.ClientSoulRender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S→C: entityId do mob marcado por Soul Render (−1 = sem marca). */
public class SyncSoulRenderMarkPacket {

    private final int entityId;

    public SyncSoulRenderMarkPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(SyncSoulRenderMarkPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
    }

    public static SyncSoulRenderMarkPacket decode(FriendlyByteBuf buf) {
        return new SyncSoulRenderMarkPacket(buf.readVarInt());
    }

    public static void handle(SyncSoulRenderMarkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientSoulRender.setMarkedEntityId(packet.entityId)
        ));
        ctx.get().setPacketHandled(true);
    }
}
