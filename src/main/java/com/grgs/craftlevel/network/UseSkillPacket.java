package com.grgs.craftlevel.network;

import com.grgs.craftlevel.skill.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C→S: lança uma skill ativa (targetEntityId opcional, −1 = nenhum). */
public class UseSkillPacket {

    private final String skillId;
    private final int targetEntityId;

    public UseSkillPacket(String skillId) {
        this(skillId, -1);
    }

    public UseSkillPacket(String skillId, int targetEntityId) {
        this.skillId = skillId == null ? "" : skillId;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(UseSkillPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.skillId);
        buf.writeVarInt(packet.targetEntityId);
    }

    public static UseSkillPacket decode(FriendlyByteBuf buf) {
        return new UseSkillPacket(buf.readUtf(), buf.readVarInt());
    }

    public static void handle(UseSkillPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || packet.skillId.isEmpty()) {
                return;
            }
            SkillService.useSkill(player, packet.skillId, packet.targetEntityId);
        });
        ctx.get().setPacketHandled(true);
    }
}
