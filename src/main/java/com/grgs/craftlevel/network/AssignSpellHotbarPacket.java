package com.grgs.craftlevel.network;

import com.grgs.craftlevel.skill.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C→S: atribui (ou limpa) uma skill em um slot da hotbar de feitiços. */
public class AssignSpellHotbarPacket {

    private final String skillId;
    private final int slot;

    public AssignSpellHotbarPacket(String skillId, int slot) {
        this.skillId = skillId == null ? "" : skillId;
        this.slot = slot;
    }

    public static void encode(AssignSpellHotbarPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.skillId);
        buf.writeVarInt(packet.slot);
    }

    public static AssignSpellHotbarPacket decode(FriendlyByteBuf buf) {
        return new AssignSpellHotbarPacket(buf.readUtf(), buf.readVarInt());
    }

    public static void handle(AssignSpellHotbarPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || packet.skillId.isEmpty()) {
                return;
            }
            SkillService.assignHotbar(player, packet.skillId, packet.slot);
        });
        ctx.get().setPacketHandled(true);
    }
}
