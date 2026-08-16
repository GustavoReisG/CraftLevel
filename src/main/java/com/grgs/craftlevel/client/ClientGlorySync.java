package com.grgs.craftlevel.client;

import com.grgs.craftlevel.capability.PlayerGlory;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

/** Aplica sync de glory/skills no cliente. */
@OnlyIn(Dist.CLIENT)
public final class ClientGlorySync {

    private ClientGlorySync() {}

    public static void apply(
            int gloryKills,
            int skillPoints,
            Map<String, Integer> skillLevels,
            Map<String, Integer> cooldownRemaining,
            String[] spellHotbar
    ) {
        SyncPlayerGloryPacket.ClientGloryState.gloryKills = gloryKills;
        SyncPlayerGloryPacket.ClientGloryState.skillPoints = skillPoints;
        SyncPlayerGloryPacket.ClientGloryState.skillLevels.clear();
        SyncPlayerGloryPacket.ClientGloryState.skillLevels.putAll(skillLevels);
        SyncPlayerGloryPacket.ClientGloryState.cooldownReadyAt.clear();

        long now = 0L;
        var player = Minecraft.getInstance().player;
        if (player != null && player.level() != null) {
            now = player.level().getGameTime();
        }
        for (Map.Entry<String, Integer> entry : cooldownRemaining.entrySet()) {
            SyncPlayerGloryPacket.ClientGloryState.cooldownReadyAt.put(
                    entry.getKey(),
                    now + Math.max(0, entry.getValue())
            );
        }
        System.arraycopy(spellHotbar, 0, SyncPlayerGloryPacket.ClientGloryState.spellHotbar, 0, PlayerGlory.SPELL_HOTBAR_SIZE);
        PlayerGloryHud.updateGlory(gloryKills);
    }
}
