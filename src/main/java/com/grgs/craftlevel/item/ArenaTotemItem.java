package com.grgs.craftlevel.item;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.damage.ModDamageTypes;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.PlayTotemAnimationPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Totens da arena (só venda no Altar). Ativam na morte (como Undying) ou no right-click.
 * Vitality: cura total. Overcharge: explosão de charged creeper (só mobs).
 * Blessing: Speed/Strength/Resistance III por 3 min.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public class ArenaTotemItem extends Item {

    public enum Kind {
        VITALITY,
        OVERCHARGE,
        BLESSING
    }

    /** Potência de charged creeper. */
    private static final float OVERCHARGE_POWER = 6.0F;
    private static final int BLESSING_TICKS = 3 * 60 * 20;
    private static final int BLESSING_AMPLIFIER = 2; // nível III
    private static final int INVULN_TICKS = 40;

    private static final Set<UUID> OVERCHARGE_BLASTS = ConcurrentHashMap.newKeySet();

    private final Kind kind;

    public ArenaTotemItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.UNCOMMON;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.fail(stack);
        }

        ItemStack visual = stack.copyWithCount(1);
        activate(serverLevel, serverPlayer, kind, visual);
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Salva de dano mortal se o totem estiver na mão principal ou offhand. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (bypassesTotem(event.getSource())) {
            return;
        }
        float lethalThreshold = player.getHealth() + player.getAbsorptionAmount();
        if (event.getAmount() < lethalThreshold) {
            return;
        }
        if (!tryPopTotem(player)) {
            return;
        }
        event.setAmount(0.0F);
        player.setAbsorptionAmount(0.0F);
    }

    /** Backup para mortes que não passam pelo hurt (igual Last Chance). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (bypassesTotem(event.getSource())) {
            return;
        }
        if (!tryPopTotem(player)) {
            return;
        }
        event.setCanceled(true);
        player.hurtTime = 0;
        player.invulnerableTime = Math.max(player.invulnerableTime, INVULN_TICKS);
    }

    private static boolean bypassesTotem(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /**
     * Consome 1 totem da mão e aplica o efeito. {@code true} se salvou.
     */
    private static boolean tryPopTotem(ServerPlayer player) {
        InteractionHand hand = null;
        ArenaTotemItem totem = null;
        for (InteractionHand candidate : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(candidate);
            if (stack.getItem() instanceof ArenaTotemItem arenaTotem) {
                hand = candidate;
                totem = arenaTotem;
                break;
            }
        }
        if (hand == null || totem == null) {
            return false;
        }

        ItemStack held = player.getItemInHand(hand);
        ItemStack visual = held.copyWithCount(1);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            player.setItemInHand(hand, held);
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        activate(level, player, totem.kind(), visual);
        player.invulnerableTime = Math.max(player.invulnerableTime, INVULN_TICKS);
        return true;
    }

    private static void activate(ServerLevel level, ServerPlayer player, Kind kind, ItemStack visual) {
        switch (kind) {
            case VITALITY -> {
                player.removeAllEffects();
                player.setHealth(player.getMaxHealth());
                player.setAbsorptionAmount(0.0F);
            }
            case OVERCHARGE -> {
                // Mesma vida do Undying (1 HP), sem os efeitos de poção vanilla.
                player.removeAllEffects();
                player.setHealth(1.0F);
                player.setAbsorptionAmount(0.0F);
                detonateOvercharge(level, player);
            }
            case BLESSING -> {
                // Mesma vida do Undying (1 HP) + buffs próprios.
                player.removeAllEffects();
                player.setHealth(1.0F);
                player.setAbsorptionAmount(0.0F);
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, BLESSING_TICKS, BLESSING_AMPLIFIER, false, true, true));
                player.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_BOOST, BLESSING_TICKS, BLESSING_AMPLIFIER, false, true, true));
                player.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE, BLESSING_TICKS, BLESSING_AMPLIFIER, false, true, true));
            }
        }
        playTotemFeedback(player, visual);
    }

    private static void detonateOvercharge(ServerLevel level, ServerPlayer player) {
        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
        OVERCHARGE_BLASTS.add(player.getUUID());
        try {
            level.explode(
                    player,
                    ModDamageTypes.overchargeDamage(level, player),
                    null,
                    player.getX(),
                    player.getY() + 0.5D,
                    player.getZ(),
                    OVERCHARGE_POWER,
                    false,
                    Level.ExplosionInteraction.NONE
            );
        } finally {
            OVERCHARGE_BLASTS.remove(player.getUUID());
        }
    }

    private static void playTotemFeedback(ServerPlayer player, ItemStack visual) {
        PacketHandler.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new PlayTotemAnimationPacket(player.getId(), visual)
        );
    }

    /** Remove players da explosão do Overcharge (mobs sofrem, terreno não). */
    @SubscribeEvent
    public static void onOverchargeDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getExplosion().getDirectSourceEntity() instanceof Player player)) {
            return;
        }
        if (!OVERCHARGE_BLASTS.contains(player.getUUID())) {
            return;
        }
        event.getAffectedBlocks().clear();
        event.getAffectedEntities().removeIf(entity -> entity instanceof Player);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("item.craftlevel.arena_totem.tooltip.undying")
                .withStyle(ChatFormatting.GRAY));
        String key = switch (kind) {
            case VITALITY -> "item.craftlevel.totem_of_vitality.tooltip";
            case OVERCHARGE -> "item.craftlevel.totem_of_overcharge.tooltip";
            case BLESSING -> "item.craftlevel.totem_of_blessing.tooltip";
        };
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}
