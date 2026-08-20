package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Light Speed: 5s com +50% velocidade, invisibilidade e dano causado = 0.
 * CD: lvl 1 = 90s, lvl 2 = 60s.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class LightSpeedSkill {

    public static final int DURATION_SECONDS = 5;
    public static final int DURATION_TICKS = DURATION_SECONDS * 20;
    public static final float SPEED_BONUS = 0.50F;

    private static final UUID SPEED_MODIFIER_ID =
            UUID.fromString("a3c8e1f0-4b2d-4e9a-9c1f-7d6e5b4a3210");
    private static final String SPEED_MODIFIER_NAME = "craftlevel_light_speed";

    private static final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();

    private LightSpeedSkill() {}

    /** Lvl 1: 90s, lvl 2+: 60s. */
    public static int cooldownTicks(int level) {
        return Math.max(1, level) >= 2 ? 60 * 20 : 90 * 20;
    }

    public static int cooldownSeconds(int level) {
        return cooldownTicks(level) / 20;
    }

    public static int speedBonusPercent() {
        return Math.round(SPEED_BONUS * 100);
    }

    public static void activate(ServerPlayer player, int level) {
        long until = player.serverLevel().getGameTime() + DURATION_TICKS;
        ACTIVE_UNTIL.put(player.getUUID(), until);
        ActiveSkillTracker.put(player, SkillCatalog.LIGHT_SPEED, until);
        applyEffects(player);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.PLAYERS,
                0.45F,
                1.6F
        );
    }

    public static boolean isActive(Player player) {
        Long until = ACTIVE_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        return player.level().getGameTime() < until;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer attacker)) {
            return;
        }
        if (!isActive(attacker)) {
            return;
        }
        event.setAmount(0.0F);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        Long until = ACTIVE_UNTIL.get(player.getUUID());
        if (until == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (now >= until) {
            endEffect(player);
            return;
        }
        applyEffects(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endEffect(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        ACTIVE_UNTIL.remove(event.getOriginal().getUUID());
        ACTIVE_UNTIL.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = ACTIVE_UNTIL.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (entry.getValue() > now) {
                continue;
            }
            ServerPlayer online = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (online != null) {
                removeEffects(online);
                ActiveSkillTracker.remove(online, SkillCatalog.LIGHT_SPEED);
            }
            it.remove();
        }
    }

    private static void endEffect(ServerPlayer player) {
        ACTIVE_UNTIL.remove(player.getUUID());
        removeEffects(player);
        ActiveSkillTracker.remove(player, SkillCatalog.LIGHT_SPEED);
    }

    private static void applyEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY,
                40,
                0,
                false,
                false,
                true
        ));
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (speed.getModifier(SPEED_MODIFIER_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID,
                    SPEED_MODIFIER_NAME,
                    SPEED_BONUS,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    private static void removeEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.INVISIBILITY);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
