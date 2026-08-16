package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.item.ItemImbue;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
import net.minecraft.world.entity.player.Player;

/** Imbue Master: +20% de imbue por nível sobre a base 250 (lvl 5 = 500 por poção). */
public final class ImbueMasterSkill {

    public static final int BONUS_PERCENT_PER_LEVEL = 20;

    private ImbueMasterSkill() {}

    public static int bonusPercent(int skillLevel) {
        if (skillLevel <= 0) {
            return 0;
        }
        return Math.min(5, skillLevel) * BONUS_PERCENT_PER_LEVEL;
    }

    /** Quantidade aplicada por poção: 250 / 300 / 350 / 400 / 450 / 500. */
    public static int amountPerPotion(int skillLevel) {
        int bonus = bonusPercent(skillLevel);
        return ItemImbue.PER_POTION + (ItemImbue.PER_POTION * bonus) / 100;
    }

    public static int amountForPlayer(Player player) {
        return amountPerPotion(skillLevel(player));
    }

    /** Nível no cliente (HUD/tooltip). */
    public static int clientSkillLevel() {
        return SyncPlayerGloryPacket.ClientGloryState.skillLevel(SkillCatalog.IMBUE_MASTER);
    }

    public static int skillLevel(Player player) {
        if (player == null) {
            return 0;
        }
        if (player.level().isClientSide()) {
            return clientSkillLevel();
        }
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getSkillLevel(SkillCatalog.IMBUE_MASTER))
                .orElse(0);
    }
}
