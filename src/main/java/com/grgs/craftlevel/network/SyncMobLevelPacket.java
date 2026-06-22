package com.grgs.craftlevel.network;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.MobLevelProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Envia level de um mob específico do servidor para o cliente (por entity ID). */
public class SyncMobLevelPacket {

    private final int entityId;
    private final int level;

    public SyncMobLevelPacket(int entityId, int level) {
        this.entityId = entityId;
        this.level = level;
    }

    public static void encode(SyncMobLevelPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeInt(packet.level);
    }

    public static SyncMobLevelPacket decode(FriendlyByteBuf buf) {
        return new SyncMobLevelPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(SyncMobLevelPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> tryApply(packet, 0));
        ctx.get().setPacketHandled(true);
    }

    /**
     * Tenta aplicar o level na capability do mob no cliente.
     * Reagenda se a entidade ainda não existir no client world (latência de spawn).
     */
    private static void tryApply(SyncMobLevelPacket packet, int attempts) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(packet.entityId);

        if (entity instanceof Mob mob) {
            mob.getCapability(MobLevelProvider.MOB_LEVEL).ifPresent(cap -> {
                cap.setLevel(packet.level);
                CraftLevel.LOGGER.info("[CraftLevel] Mob level synced: entityId={} level={}", packet.entityId, packet.level);
            });
        } else if (attempts < 100) {
            mc.tell(() -> tryApply(packet, attempts + 1));
        } else {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to sync mob level after 20 attempts: entityId={}", packet.entityId);
        }
    }
}
