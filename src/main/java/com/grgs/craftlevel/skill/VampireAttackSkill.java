package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.damage.HealContext;
import com.grgs.craftlevel.damage.ModHealTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vampire Attack: por alguns segundos, dano causado regenera 10% em vida.
 * Heal type: {@link #HEAL_KEY}.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class VampireAttackSkill {

    public static final String HEAL_KEY = ModHealTypes.VAMPIRE_ATTACK_HEAL;
    public static final float LIFESTEAL_FRACTION = 0.10F;
    public static final int COOLDOWN_TICKS = 60 * 20;

    private static final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();

    private VampireAttackSkill() {}

    /** Lvl 1: 10s, lvl 2: 15s, lvl 3: 20s. */
    public static int durationSeconds(int level) {
        return switch (Math.max(1, level)) {
            case 1 -> 10;
            case 2 -> 15;
            default -> 20;
        };
    }

    public static int durationTicks(int level) {
        return durationSeconds(level) * 20;
    }

    public static void activate(ServerPlayer player, int level) {
        long until = player.serverLevel().getGameTime() + durationTicks(level);
        ACTIVE_UNTIL.put(player.getUUID(), until);
        ActiveSkillTracker.put(player, SkillCatalog.VAMPIRE_ATTACK, until);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS,
                0.7F,
                1.4F
        );
    }

    public static boolean isActive(Player player) {
        Long until = ACTIVE_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        return player.level().getGameTime() < until;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof ServerPlayer attacker)) {
            return;
        }
        if (attacker == event.getEntity()) {
            return;
        }
        if (!isActive(attacker)) {
            return;
        }

        float healAmount = event.getAmount() * LIFESTEAL_FRACTION;
        if (healAmount <= 0.0F) {
            return;
        }

        HealContext.push(HEAL_KEY);
        try {
            attacker.heal(healAmount);
        } finally {
            HealContext.clear();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE_UNTIL.remove(event.getEntity().getUUID());
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
            if (entry.getValue() <= now) {
                it.remove();
            }
        }
    }
}
