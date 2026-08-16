package com.grgs.craftlevel.network;

import com.grgs.craftlevel.skill.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C→S: gasta SP para unlock/upgrade da skill. */
public class SpendSkillPointPacket {

    private final String skillId;

    public SpendSkillPointPacket(String skillId) {
        this.skillId = skillId == null ? "" : skillId;
    }

    public static void encode(SpendSkillPointPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.skillId);
    }

    public static SpendSkillPointPacket decode(FriendlyByteBuf buf) {
        return new SpendSkillPointPacket(buf.readUtf());
    }

    public static void handle(SpendSkillPointPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || packet.skillId.isEmpty()) {
                return;
            }
            SkillService.spendForNextLevel(player, packet.skillId);
        });
        ctx.get().setPacketHandled(true);
    }
}
