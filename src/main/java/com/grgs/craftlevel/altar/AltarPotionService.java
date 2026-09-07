package com.grgs.craftlevel.altar;

import com.grgs.craftlevel.block.AltarOfDeathBlockEntity;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.config.AltarPotionParser;
import com.grgs.craftlevel.glory.GloryKillsService;
import com.grgs.craftlevel.item.ModItems;
import com.grgs.craftlevel.menu.AltarOfDeathMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Melhorias de poção na aba Potion do altar. */
public final class AltarPotionService {

    public static final int DURATION_BONUS_SECONDS = 30;
    public static final int DURATION_BONUS_TICKS = DURATION_BONUS_SECONDS * 20;

    private static final String TIME_UPGRADES_TAG = "CraftLevelAltarTimeUpgrades";
    private static final String STRENGTH_UPGRADES_TAG = "CraftLevelAltarStrengthUpgrades";
    /** Tag antiga (contador único) — lida só para compatibilidade. */
    private static final String LEGACY_UPGRADES_TAG = "CraftLevelAltarUpgrades";

    /** Efeitos de poção vanilla sem variante de amplifier (só duração / únicos). */
    private static final Set<MobEffect> NO_AMPLIFIER_EFFECTS = Set.of(
            MobEffects.NIGHT_VISION,
            MobEffects.INVISIBILITY,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.WATER_BREATHING,
            MobEffects.SLOW_FALLING,
            MobEffects.WEAKNESS,
            MobEffects.BLINDNESS,
            MobEffects.CONFUSION,
            MobEffects.GLOWING,
            MobEffects.BAD_OMEN,
            MobEffects.HERO_OF_THE_VILLAGE,
            MobEffects.DARKNESS
    );

    private AltarPotionService() {}

    public static int gloryCostTime() {
        return AltarPotionParser.gloryCostTime;
    }

    public static int gloryCostStrength() {
        return AltarPotionParser.gloryCostStrength;
    }

    /** Limites gerais (poções sem entrada na lista). */
    public static int maxTimeUpgrades() {
        return AltarPotionParser.maxUpgradesTime;
    }

    /** Limites gerais (poções sem entrada na lista). */
    public static int maxStrengthUpgrades() {
        return AltarPotionParser.maxUpgradesStrength;
    }

    /**
     * Max time upgrades da poção: se algum efeito está na lista, usa o menor {@code time}
     * dessas entradas; senão, o valor geral.
     */
    public static int maxTimeUpgrades(ItemStack stack) {
        AltarPotionParser.EffectLimit limit = resolveListedLimit(stack);
        return limit != null ? limit.time() : maxTimeUpgrades();
    }

    /**
     * Max strength upgrades da poção: se algum efeito está na lista, usa o menor {@code level}
     * dessas entradas; senão, o valor geral.
     */
    public static int maxStrengthUpgrades(ItemStack stack) {
        AltarPotionParser.EffectLimit limit = resolveListedLimit(stack);
        return limit != null ? limit.level() : maxStrengthUpgrades();
    }

    public static int maxUpgradesTotal() {
        return maxTimeUpgrades() + maxStrengthUpgrades();
    }

    /** Menor limite listado entre os efeitos da poção, ou null se nenhum estiver na lista. */
    private static AltarPotionParser.EffectLimit resolveListedLimit(ItemStack stack) {
        if (!hasEffects(stack)) {
            return null;
        }
        AltarPotionParser.EffectLimit best = null;
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            AltarPotionParser.EffectLimit limit = AltarPotionParser.limitOf(effect.getEffect());
            if (limit == null) {
                continue;
            }
            if (best == null) {
                best = limit;
            } else {
                best = new AltarPotionParser.EffectLimit(
                        Math.min(best.time(), limit.time()),
                        Math.min(best.level(), limit.level())
                );
            }
        }
        return best;
    }

    public static boolean isPotionItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof PotionItem || stack.is(ModItems.IMBUE_POTION.get());
    }

    public static boolean hasEffects(ItemStack stack) {
        return isPotionItem(stack) && !PotionUtils.getMobEffects(stack).isEmpty();
    }

    public static int getTimeUpgradeCount(ItemStack stack) {
        return readUpgradeCount(stack, TIME_UPGRADES_TAG);
    }

    public static int getStrengthUpgradeCount(ItemStack stack) {
        return readUpgradeCount(stack, STRENGTH_UPGRADES_TAG);
    }

    public static int getUpgradeCount(ItemStack stack) {
        return getTimeUpgradeCount(stack) + getStrengthUpgradeCount(stack);
    }

    public static boolean hasTimeUpgradesRemaining(ItemStack stack) {
        return getTimeUpgradeCount(stack) < maxTimeUpgrades(stack);
    }

    public static boolean hasStrengthUpgradesRemaining(ItemStack stack) {
        return getStrengthUpgradeCount(stack) < maxStrengthUpgrades(stack);
    }

    /** Poção cujos efeitos são todos instantâneos (cura, dano, etc.). */
    public static boolean isInstantOnlyPotion(ItemStack stack) {
        if (!hasEffects(stack)) {
            return false;
        }
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            if (!isInstant(effect)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canExtendDuration(ItemStack stack) {
        if (!hasEffects(stack) || !hasTimeUpgradesRemaining(stack) || isInstantOnlyPotion(stack)) {
            return false;
        }
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            if (!isInstant(effect)) {
                return true;
            }
        }
        return false;
    }

    /** True se a poção tem ao menos um efeito que aceita upgrade de amplifier. */
    public static boolean hasAmplifierUpgradeableEffects(ItemStack stack) {
        if (!hasEffects(stack)) {
            return false;
        }
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            if (supportsAmplifierUpgrade(effect.getEffect())) {
                return true;
            }
        }
        return false;
    }

    public static boolean supportsAmplifierUpgrade(MobEffect effect) {
        return effect != null && !NO_AMPLIFIER_EFFECTS.contains(effect);
    }

    public static boolean canUpgradeEffect(ItemStack stack) {
        if (!hasEffects(stack)
                || !hasStrengthUpgradesRemaining(stack)
                || !hasAmplifierUpgradeableEffects(stack)) {
            return false;
        }
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            if (!supportsAmplifierUpgrade(effect.getEffect())) {
                continue;
            }
            int level = effect.getAmplifier() + 1;
            if (level < AltarPotionParser.maxLevel(effect.getEffect())) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryExtendDuration(
            ServerPlayer player,
            AltarOfDeathMenu menu,
            BlockPos altarPos
    ) {
        return tryModify(player, menu, altarPos, true);
    }

    public static boolean tryUpgradeEffect(
            ServerPlayer player,
            AltarOfDeathMenu menu,
            BlockPos altarPos
    ) {
        return tryModify(player, menu, altarPos, false);
    }

    private static boolean tryModify(
            ServerPlayer player,
            AltarOfDeathMenu menu,
            BlockPos altarPos,
            boolean extendDuration
    ) {
        if (player.level().isDay()) {
            player.displayClientMessage(
                    Component.translatable("block.craftlevel.altar_of_death.need_night"),
                    true
            );
            return false;
        }

        ItemStack input = menu.getPotionStack();
        if (!hasEffects(input)) {
            player.displayClientMessage(
                    Component.translatable("gui.craftlevel.altar.potion.invalid_item"),
                    true
            );
            return false;
        }

        if (!menu.getPotionOutputStack().isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("gui.craftlevel.altar.potion.output_full"),
                    true
            );
            return false;
        }

        if (extendDuration) {
            if (!hasTimeUpgradesRemaining(input)) {
                player.displayClientMessage(
                        Component.translatable(
                                "gui.craftlevel.altar.potion.max_upgrades_time",
                                maxTimeUpgrades(input)
                        ),
                        true
                );
                return false;
            }
            if (!canExtendDuration(input)) {
                player.displayClientMessage(
                        Component.translatable("gui.craftlevel.altar.potion.instant"),
                        true
                );
                return false;
            }
        } else {
            if (!hasStrengthUpgradesRemaining(input)) {
                player.displayClientMessage(
                        Component.translatable(
                                "gui.craftlevel.altar.potion.max_upgrades_strength",
                                maxStrengthUpgrades(input)
                        ),
                        true
                );
                return false;
            }
            if (!canUpgradeEffect(input)) {
                return false;
            }
        }

        int cost = extendDuration ? gloryCostTime() : gloryCostStrength();
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY).map(glory -> {
            int gloryKills = glory.getGloryKills();
            if (gloryKills < cost) {
                player.displayClientMessage(
                        Component.translatable(
                                "block.craftlevel.altar_of_death.need_kills",
                                cost,
                                gloryKills
                        ),
                        true
                );
                return false;
            }

            ItemStack result = extendDuration
                    ? applyDurationBonus(input)
                    : applyEffectUpgrade(input);

            glory.setGloryKills(gloryKills - cost);

            menu.setPotionStack(ItemStack.EMPTY);
            menu.setPotionOutputStack(result);
            menu.broadcastChanges();

            GloryKillsService.sync(player);
            applyEffects(player.level(), altarPos);
            return true;
        }).orElse(false);
    }

    private static ItemStack applyDurationBonus(ItemStack input) {
        List<MobEffectInstance> modified = new ArrayList<>();
        for (MobEffectInstance effect : PotionUtils.getMobEffects(input)) {
            if (isInstant(effect)) {
                modified.add(copyEffect(effect));
            } else {
                modified.add(new MobEffectInstance(
                        effect.getEffect(),
                        effect.getDuration() + DURATION_BONUS_TICKS,
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.isVisible(),
                        effect.showIcon()
                ));
            }
        }
        return finalizeResult(input, modified, true);
    }

    private static ItemStack applyEffectUpgrade(ItemStack input) {
        List<MobEffectInstance> modified = new ArrayList<>();
        for (MobEffectInstance effect : PotionUtils.getMobEffects(input)) {
            if (!supportsAmplifierUpgrade(effect.getEffect())) {
                modified.add(copyEffect(effect));
                continue;
            }
            int maxAmplifier = Math.max(0, AltarPotionParser.maxLevel(effect.getEffect()) - 1);
            int amplifier = Math.min(255, Math.min(maxAmplifier, effect.getAmplifier() + 1));
            modified.add(new MobEffectInstance(
                    effect.getEffect(),
                    effect.getDuration(),
                    amplifier,
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }
        return finalizeResult(input, modified, false);
    }

    private static ItemStack finalizeResult(ItemStack input, List<MobEffectInstance> effects, boolean timeUpgrade) {
        ItemStack result = input.copyWithCount(1);
        PotionUtils.setPotion(result, Potions.AWKWARD);
        PotionUtils.setCustomEffects(result, effects);
        CompoundTag tag = result.getOrCreateTag();
        tag.remove(LEGACY_UPGRADES_TAG);
        if (timeUpgrade) {
            tag.putInt(TIME_UPGRADES_TAG, getTimeUpgradeCount(input) + 1);
            tag.putInt(STRENGTH_UPGRADES_TAG, getStrengthUpgradeCount(input));
        } else {
            tag.putInt(TIME_UPGRADES_TAG, getTimeUpgradeCount(input));
            tag.putInt(STRENGTH_UPGRADES_TAG, getStrengthUpgradeCount(input) + 1);
        }
        return result;
    }

    private static int readUpgradeCount(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0;
        }
        if (tag.contains(key)) {
            return Math.max(0, tag.getInt(key));
        }
        // Poções antigas: contador único — não conta em nenhum dos dois (começa limpo).
        return 0;
    }

    private static MobEffectInstance copyEffect(MobEffectInstance effect) {
        return new MobEffectInstance(
                effect.getEffect(),
                effect.getDuration(),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon()
        );
    }

    private static boolean isInstant(MobEffectInstance effect) {
        return effect.getEffect().isInstantenous();
    }

    private static void applyEffects(Level level, BlockPos pos) {
        AltarOfDeathBlockEntity.applyExchangeDarkness(level, pos);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.2F);
    }
}
