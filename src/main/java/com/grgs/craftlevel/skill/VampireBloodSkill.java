package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Vampire Blood: à noite, +% vida máx. e +% dano.
 * Lvl 1: 5%, lvl 2: 10%.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class VampireBloodSkill {

    private static final UUID HEALTH_MODIFIER_ID =
            UUID.fromString("b7e4c2a1-8d3f-4a6b-9e1c-5f0d8a2b4c6e");
    private static final String HEALTH_MODIFIER_NAME = "craftlevel_vampire_blood";

    private VampireBloodSkill() {}

    /** Lvl 1: 5%, lvl 2+: 10%. */
    public static int bonusPercent(int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.max(1, level) >= 2 ? 10 : 5;
    }

    public static float bonusFraction(int level) {
        return bonusPercent(level) / 100.0F;
    }

    public static int skillLevel(Player player) {
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getSkillLevel(SkillCatalog.VAMPIRE_BLOOD))
                .orElse(0);
    }

    public static boolean isNightActive(Player player) {
        return skillLevel(player) > 0 && !player.level().isDay();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        // Atualiza 1×/s — suficiente para dia/noite.
        if (player.tickCount % 20 != 0) {
            return;
        }
        refreshHealthBonus(player);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer attacker)) {
            return;
        }
        if (attacker == event.getEntity()) {
            return;
        }
        if (!isNightActive(attacker)) {
            return;
        }
        float mult = 1.0F + bonusFraction(skillLevel(attacker));
        if (mult > 1.0F) {
            event.setAmount(event.getAmount() * mult);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearHealthBonus(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) {
            clearHealthBonus(original);
        }
    }

    /** Chamado após unlock/upgrade para aplicar na hora se for noite. */
    public static void refreshHealthBonus(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.removeModifier(HEALTH_MODIFIER_ID);
        int level = skillLevel(player);
        if (level > 0 && !player.level().isDay()) {
            health.addTransientModifier(new AttributeModifier(
                    HEALTH_MODIFIER_ID,
                    HEALTH_MODIFIER_NAME,
                    bonusFraction(level),
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void clearHealthBonus(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HEALTH_MODIFIER_ID);
        }
    }
}
