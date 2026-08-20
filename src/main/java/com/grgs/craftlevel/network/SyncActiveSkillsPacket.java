package com.grgs.craftlevel.network;

import com.grgs.craftlevel.client.ClientActiveSkills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** S→C: skills com efeito ativo (skillId → gameTime de expiração). */
public class SyncActiveSkillsPacket {

    private final Map<String, Long> expireAtGameTime;

    public SyncActiveSkillsPacket(Map<String, Long> expireAtGameTime) {
        this.expireAtGameTime = expireAtGameTime == null ? Map.of() : Map.copyOf(expireAtGameTime);
    }

    public static void encode(SyncActiveSkillsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.expireAtGameTime.size());
        packet.expireAtGameTime.forEach((id, expire) -> {
            buf.writeUtf(id);
            buf.writeLong(expire);
        });
    }

    public static SyncActiveSkillsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Long> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(), buf.readLong());
        }
        return new SyncActiveSkillsPacket(map);
    }

    public static void handle(SyncActiveSkillsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientActiveSkills.apply(packet.expireAtGameTime)
        ));
        ctx.get().setPacketHandled(true);
    }
}
