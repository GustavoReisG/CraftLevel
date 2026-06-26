package com.grgs.craftlevel.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Envia posição e valor de dano para renderizar indicador flutuante no cliente. */
public class DamageIndicatorPacket {

    private final double x, y, z;
    private final float damage;

    public DamageIndicatorPacket(double x, double y, double z, float damage) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.damage = damage;
    }

    public static void encode(DamageIndicatorPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeFloat(packet.damage);
    }

    public static DamageIndicatorPacket decode(FriendlyByteBuf buf) {
        return new DamageIndicatorPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
    }

    public static void handle(DamageIndicatorPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.grgs.craftlevel.client.DamageIndicatorRenderer.addIndicator(
                    packet.x, packet.y, packet.z, packet.damage
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
