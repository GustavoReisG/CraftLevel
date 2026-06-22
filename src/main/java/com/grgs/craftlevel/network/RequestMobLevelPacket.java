package com.grgs.craftlevel.network;

import com.grgs.craftlevel.capability.MobLevelProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente pede ao servidor o level de um mob que ainda não foi sincronizado. */
public class RequestMobLevelPacket {

    private final int entityId;

    public RequestMobLevelPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(RequestMobLevelPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
    }

    public static RequestMobLevelPacket decode(FriendlyByteBuf buf) {
        return new RequestMobLevelPacket(buf.readInt());
    }

    /** Servidor responde com SyncMobLevelPacket contendo o level da capability do mob. */
    public static void handle(RequestMobLevelPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level() instanceof ServerLevel serverLevel)) return;

            Entity entity = serverLevel.getEntity(packet.entityId);
            if (!(entity instanceof Mob mob)) return;

            mob.getCapability(MobLevelProvider.MOB_LEVEL).ifPresent(cap -> {
                PacketHandler.sendToPlayer(player, new SyncMobLevelPacket(mob.getId(), cap.getLevel()));
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
