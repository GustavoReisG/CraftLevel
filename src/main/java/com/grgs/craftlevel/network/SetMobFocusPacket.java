package com.grgs.craftlevel.network;

import com.grgs.craftlevel.skill.MobFocusTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C→S: sincroniza o Mob Focus do cliente (−1 = limpar). */
public class SetMobFocusPacket {

    private static final double MAX_FOCUS_DISTANCE_SQR = 64.0 * 64.0;

    private final int entityId;

    public SetMobFocusPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(SetMobFocusPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
    }

    public static SetMobFocusPacket decode(FriendlyByteBuf buf) {
        return new SetMobFocusPacket(buf.readVarInt());
    }

    public static void handle(SetMobFocusPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (packet.entityId < 0) {
                MobFocusTracker.clear(player);
                return;
            }
            Entity entity = player.serverLevel().getEntity(packet.entityId);
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living == player) {
                MobFocusTracker.clear(player);
                return;
            }
            if (player.distanceToSqr(living) > MAX_FOCUS_DISTANCE_SQR) {
                MobFocusTracker.clear(player);
                return;
            }
            MobFocusTracker.set(player, living.getId());
        });
        ctx.get().setPacketHandled(true);
    }
}
