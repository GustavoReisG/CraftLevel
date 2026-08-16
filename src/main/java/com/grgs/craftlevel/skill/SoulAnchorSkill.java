package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.capability.PlayerGloryProvider;
import net.minecraft.server.level.ServerPlayer;

/** Soul Anchor: chance passiva de não perder Soul ao morrer (5% por nível, máx. 25%). */
public final class SoulAnchorSkill {

    private SoulAnchorSkill() {}

    /** 5% por nível, limitado ao max level da skill (25% no lvl 5). */
    public static int saveChancePercent(int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.min(25, level * 5);
    }

    /**
     * @return {@code true} se a Soul deve ser preservada nesta morte.
     */
    public static boolean trySaveSoul(ServerPlayer player) {
        int level = player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getSkillLevel(SkillCatalog.SOUL_ANCHOR))
                .orElse(0);
        int chance = saveChancePercent(level);
        if (chance <= 0) {
            return false;
        }
        return player.getRandom().nextInt(100) < chance;
    }
}
