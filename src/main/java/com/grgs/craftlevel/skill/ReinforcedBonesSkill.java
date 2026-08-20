package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Reinforced Bones: reduz dano de queda em 30% / 40% / 50% (lvl 1–3).
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class ReinforcedBonesSkill {

    private ReinforcedBonesSkill() {}

    /** Lvl 1: 30%, lvl 2: 40%, lvl 3: 50%. */
    public static int reductionPercent(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 30;
            case 2 -> 40;
            default -> 50;
        };
    }

    public static float damageMultiplier(int level) {
        return 1.0F - reductionPercent(level) / 100.0F;
    }

    public static int skillLevel(Player player) {
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getSkillLevel(SkillCatalog.REINFORCED_BONES))
                .orElse(0);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getSource().is(DamageTypeTags.IS_FALL)) {
            return;
        }
        int level = skillLevel(player);
        if (level <= 0) {
            return;
        }
        event.setAmount(event.getAmount() * damageMultiplier(level));
    }
}
