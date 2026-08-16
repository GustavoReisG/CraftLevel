package com.grgs.craftlevel.network;

import com.grgs.craftlevel.skill.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C→S: lança uma skill ativa. */
public class UseSkillPacket {

    private final String skillId;

    public UseSkillPacket(String skillId) {
        this.skillId = skillId == null ? "" : skillId;
    }

    public static void encode(UseSkillPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.skillId);
    }

    public static UseSkillPacket decode(FriendlyByteBuf buf) {
        return new UseSkillPacket(buf.readUtf());
    }

    public static void handle(UseSkillPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || packet.skillId.isEmpty()) {
                return;
            }
            SkillService.useSkill(player, packet.skillId);
        });
        ctx.get().setPacketHandled(true);
    }
}
