package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.damage.DamageClassifier;
import com.grgs.craftlevel.damage.ModDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DoTs de skill (Wither/Poison) atribuídos ao player que aplicou o efeito.
 * O dano vanilla desses efeitos não tem atacante — sem isso o scaling usa o mob (hurt_self).
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class PlayerAttributedEffects {

    private static final ThreadLocal<Boolean> REDIRECTING = ThreadLocal.withInitial(() -> false);

    private static final class Attribution {
        final UUID playerId;
        final long expireAtGameTime;
        final ResourceKey<DamageType> damageType;

        Attribution(UUID playerId, long expireAtGameTime, ResourceKey<DamageType> damageType) {
            this.playerId = playerId;
            this.expireAtGameTime = expireAtGameTime;
            this.damageType = damageType;
        }
    }

    /** Alvo → player responsável pelo DoT. */
    private static final Map<UUID, Attribution> BY_TARGET = new ConcurrentHashMap<>();

    private PlayerAttributedEffects() {}

    public static void applyWither(
            ServerPlayer player,
            LivingEntity target,
            int durationTicks,
            int amplifier
    ) {
        applyWither(player, target, durationTicks, amplifier, ModDamageTypes.NETHER_ELEVATION_DAMAGE);
    }

    public static void applyWither(
            ServerPlayer player,
            LivingEntity target,
            int durationTicks,
            int amplifier,
            ResourceKey<DamageType> damageType
    ) {
        applyDoT(player, target, MobEffects.WITHER, durationTicks, amplifier, damageType);
    }

    public static void applyPoison(
            ServerPlayer player,
            LivingEntity target,
            int durationTicks,
            int amplifier
    ) {
        applyDoT(player, target, MobEffects.POISON, durationTicks, amplifier, null);
    }

    public static void applyDoT(
            ServerPlayer player,
            LivingEntity target,
            MobEffect effect,
            int durationTicks,
            int amplifier
    ) {
        applyDoT(player, target, effect, durationTicks, amplifier, null);
    }

    public static void applyDoT(
            ServerPlayer player,
            LivingEntity target,
            MobEffect effect,
            int durationTicks,
            int amplifier,
            ResourceKey<DamageType> damageType
    ) {
        long expireAt = player.serverLevel().getGameTime() + Math.max(1, durationTicks);
        BY_TARGET.put(target.getUUID(), new Attribution(player.getUUID(), expireAt, damageType));
        target.addEffect(new MobEffectInstance(
                effect,
                durationTicks,
                amplifier,
                false,
                true,
                true
        ));
    }

    public static void clear(LivingEntity target) {
        BY_TARGET.remove(target.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (REDIRECTING.get() || event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }

        String type = DamageClassifier.getDamageTypeId(event.getSource());
        if (!DamageClassifier.WITHER.equals(type) && !DamageClassifier.POISON.equals(type)) {
            return;
        }

        LivingEntity target = event.getEntity();
        Attribution attribution = BY_TARGET.get(target.getUUID());
        if (attribution == null) {
            return;
        }

        long now = target.level().getGameTime();
        if (now >= attribution.expireAtGameTime) {
            BY_TARGET.remove(target.getUUID());
            return;
        }

        MinecraftServer server = target.level().getServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(attribution.playerId);
        if (player == null || !player.isAlive()) {
            BY_TARGET.remove(target.getUUID());
            return;
        }

        float amount = event.getAmount();
        event.setCanceled(true);

        REDIRECTING.set(true);
        try {
            int invuln = target.invulnerableTime;
            target.invulnerableTime = 0;
            if (attribution.damageType != null) {
                target.hurt(ModDamageTypes.create(player.level(), attribution.damageType, player), amount);
            } else {
                // Fallback: indirect_magic → spell_power no wiring do player.
                target.hurt(player.damageSources().indirectMagic(player, player), amount);
            }
            target.invulnerableTime = Math.max(invuln, target.invulnerableTime);
        } finally {
            REDIRECTING.set(false);
        }
    }
}
