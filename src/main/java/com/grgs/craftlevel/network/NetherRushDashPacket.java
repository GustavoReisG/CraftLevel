package com.grgs.craftlevel.network;

import com.grgs.craftlevel.client.ClientNetherRush;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S→C: inicia a investida do Nether Rush no cliente. */
public class NetherRushDashPacket {

    private final double dx;
    private final double dy;
    private final double dz;
    private final int ticks;

    public NetherRushDashPacket(double dx, double dy, double dz, int ticks) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.ticks = ticks;
    }

    public static void encode(NetherRushDashPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.dx);
        buf.writeDouble(packet.dy);
        buf.writeDouble(packet.dz);
        buf.writeVarInt(packet.ticks);
    }

    public static NetherRushDashPacket decode(FriendlyByteBuf buf) {
        return new NetherRushDashPacket(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt()
        );
    }

    public static void handle(NetherRushDashPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientNetherRush.start(packet.dx, packet.dy, packet.dz, packet.ticks)
        ));
        ctx.get().setPacketHandled(true);
    }
}
