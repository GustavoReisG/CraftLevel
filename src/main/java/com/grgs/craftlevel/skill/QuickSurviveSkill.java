package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Quick Survive: abaixo de 30% de vida, +30% velocidade de movimento. 1 nível.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class QuickSurviveSkill {

    public static final float HEALTH_THRESHOLD = 0.30F;
    public static final float SPEED_BONUS = 0.30F;

    private static final UUID SPEED_MODIFIER_ID =
            UUID.fromString("c8f5d3b2-9e4a-4b7c-a2d1-6e0f9c3a5b7d");
    private static final String SPEED_MODIFIER_NAME = "craftlevel_quick_survive";

    private QuickSurviveSkill() {}

    public static int healthThresholdPercent() {
        return Math.round(HEALTH_THRESHOLD * 100);
    }

    public static int speedBonusPercent() {
        return Math.round(SPEED_BONUS * 100);
    }

    public static int skillLevel(Player player) {
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getSkillLevel(SkillCatalog.QUICK_SURVIVE))
                .orElse(0);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        refreshSpeedBonus(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearSpeedBonus(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) {
            clearSpeedBonus(original);
        }
    }

    public static void refreshSpeedBonus(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean shouldBoost = skillLevel(player) > 0 && isLowHealth(player);
        boolean hasMod = speed.getModifier(SPEED_MODIFIER_ID) != null;
        if (shouldBoost && !hasMod) {
            speed.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID,
                    SPEED_MODIFIER_NAME,
                    SPEED_BONUS,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        } else if (!shouldBoost && hasMod) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    private static boolean isLowHealth(Player player) {
        float max = player.getMaxHealth();
        if (max <= 0.0F) {
            return false;
        }
        return player.getHealth() / max < HEALTH_THRESHOLD;
    }

    private static void clearSpeedBonus(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
