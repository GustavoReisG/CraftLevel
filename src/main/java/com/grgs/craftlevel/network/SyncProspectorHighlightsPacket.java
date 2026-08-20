package com.grgs.craftlevel.network;

import com.grgs.craftlevel.client.ClientProspectorHighlights;
import com.grgs.craftlevel.skill.ProspectorSkill;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** S→C: minérios destacados pelo Prospector até expireAtGameTime. */
public class SyncProspectorHighlightsPacket {

    private final long expireAtGameTime;
    private final List<ProspectorSkill.Highlight> highlights;

    public SyncProspectorHighlightsPacket(long expireAtGameTime, List<ProspectorSkill.Highlight> highlights) {
        this.expireAtGameTime = expireAtGameTime;
        this.highlights = highlights == null ? List.of() : List.copyOf(highlights);
    }

    public static void encode(SyncProspectorHighlightsPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.expireAtGameTime);
        buf.writeVarInt(packet.highlights.size());
        for (ProspectorSkill.Highlight h : packet.highlights) {
            buf.writeBlockPos(h.pos());
            buf.writeInt(h.rgb());
        }
    }

    public static SyncProspectorHighlightsPacket decode(FriendlyByteBuf buf) {
        long expire = buf.readLong();
        int size = buf.readVarInt();
        List<ProspectorSkill.Highlight> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            BlockPos pos = buf.readBlockPos();
            int rgb = buf.readInt();
            list.add(new ProspectorSkill.Highlight(pos, rgb));
        }
        return new SyncProspectorHighlightsPacket(expire, list);
    }

    public static void handle(SyncProspectorHighlightsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientProspectorHighlights.apply(packet.expireAtGameTime, packet.highlights)
        ));
        ctx.get().setPacketHandled(true);
    }
}
