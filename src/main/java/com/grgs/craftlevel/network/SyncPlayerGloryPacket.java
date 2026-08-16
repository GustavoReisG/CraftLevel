package com.grgs.craftlevel.network;

import com.grgs.craftlevel.capability.PlayerGlory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Sincroniza Glory Kills, Skill Points, níveis, CD e hotbar de feitiços. */
public class SyncPlayerGloryPacket {

    private final int gloryKills;
    private final int skillPoints;
    private final Map<String, Integer> skillLevels;
    private final Map<String, Integer> cooldownRemaining;
    private final String[] spellHotbar;

    public SyncPlayerGloryPacket(
            int gloryKills,
            int skillPoints,
            Map<String, Integer> skillLevels,
            Map<String, Integer> cooldownRemaining,
            String[] spellHotbar
    ) {
        this.gloryKills = gloryKills;
        this.skillPoints = skillPoints;
        this.skillLevels = skillLevels == null ? Map.of() : Map.copyOf(skillLevels);
        this.cooldownRemaining = cooldownRemaining == null ? Map.of() : Map.copyOf(cooldownRemaining);
        this.spellHotbar = new String[PlayerGlory.SPELL_HOTBAR_SIZE];
        if (spellHotbar != null) {
            System.arraycopy(spellHotbar, 0, this.spellHotbar, 0, Math.min(spellHotbar.length, this.spellHotbar.length));
        }
    }

    public static void encode(SyncPlayerGloryPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.gloryKills);
        buf.writeVarInt(packet.skillPoints);

        buf.writeVarInt(packet.skillLevels.size());
        packet.skillLevels.forEach((id, level) -> {
            buf.writeUtf(id);
            buf.writeVarInt(level);
        });

        buf.writeVarInt(packet.cooldownRemaining.size());
        packet.cooldownRemaining.forEach((id, remaining) -> {
            buf.writeUtf(id);
            buf.writeVarInt(remaining);
        });

        for (int i = 0; i < PlayerGlory.SPELL_HOTBAR_SIZE; i++) {
            String id = packet.spellHotbar[i];
            buf.writeUtf(id == null ? "" : id);
        }
    }

    public static SyncPlayerGloryPacket decode(FriendlyByteBuf buf) {
        int gloryKills = buf.readVarInt();
        int skillPoints = buf.readVarInt();

        int skillCount = buf.readVarInt();
        Map<String, Integer> skillLevels = new HashMap<>();
        for (int i = 0; i < skillCount; i++) {
            skillLevels.put(buf.readUtf(), buf.readVarInt());
        }

        int cdCount = buf.readVarInt();
        Map<String, Integer> cooldownRemaining = new HashMap<>();
        for (int i = 0; i < cdCount; i++) {
            cooldownRemaining.put(buf.readUtf(), buf.readVarInt());
        }

        String[] hotbar = new String[PlayerGlory.SPELL_HOTBAR_SIZE];
        for (int i = 0; i < PlayerGlory.SPELL_HOTBAR_SIZE; i++) {
            String id = buf.readUtf();
            hotbar[i] = id.isEmpty() ? null : id;
        }
        return new SyncPlayerGloryPacket(gloryKills, skillPoints, skillLevels, cooldownRemaining, hotbar);
    }

    public static void handle(SyncPlayerGloryPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> com.grgs.craftlevel.client.ClientGlorySync.apply(
                        packet.gloryKills,
                        packet.skillPoints,
                        packet.skillLevels,
                        packet.cooldownRemaining,
                        packet.spellHotbar
                )
        ));
        ctx.get().setPacketHandled(true);
    }

    /** Estado client-side mínimo. */
    public static final class ClientGloryState {
        public static int gloryKills = -1;
        public static int skillPoints = 0;
        public static final Map<String, Integer> skillLevels = new HashMap<>();
        public static final Map<String, Long> cooldownReadyAt = new HashMap<>();
        public static final String[] spellHotbar = new String[PlayerGlory.SPELL_HOTBAR_SIZE];

        public static int skillLevel(String skillId) {
            return Math.max(0, skillLevels.getOrDefault(skillId, 0));
        }

        public static int remainingCooldownTicks(String skillId, long gameTime) {
            long readyAt = cooldownReadyAt.getOrDefault(skillId, 0L);
            return (int) Math.max(0L, readyAt - gameTime);
        }
    }
}
