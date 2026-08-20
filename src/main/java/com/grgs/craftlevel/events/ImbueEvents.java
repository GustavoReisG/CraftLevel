package com.grgs.craftlevel.events;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.damage.DamageClassifier;
import com.grgs.craftlevel.item.ItemImbue;
import com.grgs.craftlevel.skill.PlayerAttributedEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Aplica efeitos de imbue no hit e tooltip nas armas embuídas. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class ImbueEvents {

    /** Evita reentrada quando Instant Heal/Harm dispara outro {@link LivingHurtEvent}. */
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    private ImbueEvents() {}

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (APPLYING.get() || event.getEntity().level().isClientSide() || event.getAmount() <= 0) {
            return;
        }
        // Só o hit melee original — não o magic aninhado do Instant Damage/Health.
        if (!DamageClassifier.PLAYER_ATTACK.equals(DamageClassifier.getDamageTypeId(event.getSource()))) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (target instanceof Player) {
            return;
        }

        Player attacker = resolveMeleePlayer(event.getSource());
        if (attacker == null) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!ItemImbue.hasImbue(weapon) || ItemImbue.getAmount(weapon) < ItemImbue.COST_PER_HIT) {
            return;
        }

        List<MobEffectInstance> effects = ItemImbue.effectsForHit(weapon);
        if (effects.isEmpty()) {
            return;
        }

        APPLYING.set(true);
        try {
            for (MobEffectInstance effect : effects) {
                if (effect.getEffect().isInstantenous()) {
                    // Sem isso, hurt() do Instant é engolido pelos i-frames do melee.
                    int invuln = target.invulnerableTime;
                    target.invulnerableTime = 0;
                    effect.getEffect().applyInstantenousEffect(
                            attacker,
                            attacker,
                            target,
                            effect.getAmplifier(),
                            ItemImbue.INSTANT_INTENSITY
                    );
                    target.invulnerableTime = Math.max(invuln, target.invulnerableTime);
                } else {
                    MobEffect type = effect.getEffect();
                    if ((type == MobEffects.WITHER || type == MobEffects.POISON)
                            && attacker instanceof ServerPlayer serverPlayer) {
                        PlayerAttributedEffects.applyDoT(
                                serverPlayer,
                                target,
                                type,
                                effect.getDuration(),
                                effect.getAmplifier()
                        );
                    } else {
                        target.addEffect(new MobEffectInstance(effect));
                    }
                }
            }
            ItemImbue.consume(weapon, ItemImbue.COST_PER_HIT);
        } finally {
            APPLYING.set(false);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        int amount = ItemImbue.getAmount(stack);
        if (amount <= 0) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.translatable(
                "item.craftlevel.imbue.tooltip.amount",
                amount,
                ItemImbue.MAX
        ).withStyle(ChatFormatting.DARK_PURPLE));

        List<MobEffectInstance> effects = ItemImbue.getEffects(stack);
        if (!effects.isEmpty()) {
            PotionUtils.addPotionTooltip(
                    effects.stream()
                            .map(e -> e.getEffect().isInstantenous()
                                    ? e
                                    : new MobEffectInstance(
                                            e.getEffect(),
                                            Math.max(1, e.getDuration() / ItemImbue.EFFECT_DURATION_DIVISOR),
                                            e.getAmplifier(),
                                            e.isAmbient(),
                                            e.isVisible(),
                                            e.showIcon()
                                    ))
                            .toList(),
                    tooltip,
                    1.0F
            );
        }
    }

    /** Só melee direto do player (não projéteis). */
    private static Player resolveMeleePlayer(DamageSource source) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (attacker instanceof Player player && direct == attacker) {
            return player;
        }
        return null;
    }
}
