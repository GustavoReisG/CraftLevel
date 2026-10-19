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

    /** Tags antigas (contador de upgrades) — removidas ao modificar a poção. */
    private static final String TIME_UPGRADES_TAG = "CraftLevelAltarTimeUpgrades";
    private static final String STRENGTH_UPGRADES_TAG = "CraftLevelAltarStrengthUpgrades";
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

    /** Limite geral de duração (segundos), para poções sem entrada na lista. */
    public static int maxDurationSeconds() {
        return AltarPotionParser.maxDurationSeconds;
    }

    /** Limite geral de nível, para poções sem entrada na lista. */
    public static int maxLevel() {
        return AltarPotionParser.maxLevel;
    }

    public static int maxDurationSeconds(MobEffect effect) {
        return AltarPotionParser.maxDurationSeconds(effect);
    }

    public static int maxDurationTicks(MobEffect effect) {
        return maxDurationSeconds(effect) * 20;
    }

    public static int maxLevel(MobEffect effect) {
        return AltarPotionParser.maxLevel(effect);
    }

    /**
     * Menor duração máxima (segundos) entre os efeitos listados da poção;
     * se nenhum estiver na lista, usa o limite geral.
     */
    public static int maxDurationSeconds(ItemStack stack) {
        AltarPotionParser.EffectLimit limit = resolveListedLimit(stack);
        return limit != null ? limit.time() : maxDurationSeconds();
    }

    /**
     * Menor nível máximo entre os efeitos listados da poção;
     * se nenhum estiver na lista, usa o limite geral.
     */
    public static int maxLevel(ItemStack stack) {
        AltarPotionParser.EffectLimit limit = resolveListedLimit(stack);
        return limit != null ? limit.level() : maxLevel();
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

    /** Maior duração (ticks) entre efeitos não-instantâneos; 0 se não houver. */
    public static int currentMaxDurationTicks(ItemStack stack) {
        int max = 0;
        if (!hasEffects(stack)) {
            return 0;
        }
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            if (isInstant(effect)) {
                continue;
            }
            max = Math.max(max, effect.getDuration());
        }
        return max;
    }

    /** Maior nível (amplifier + 1) entre efeitos que aceitam upgrade de amplifier. */
    public static int currentMaxLevel(ItemStack stack) {
        int max = 0;
        if (!hasEffects(stack)) {
            return 0;
        }
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            if (!supportsAmplifierUpgrade(effect.getEffect())) {
                continue;
            }
            max = Math.max(max, effect.getAmplifier() + 1);
        }
        return max;
    }

    public static boolean hasDurationRemaining(ItemStack stack) {
        if (!hasEffects(stack) || isInstantOnlyPotion(stack)) {
            return false;
        }
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            if (isInstant(effect)) {
                continue;
            }
            if (effect.getDuration() < maxDurationTicks(effect.getEffect())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasLevelRemaining(ItemStack stack) {
        if (!hasEffects(stack) || !hasAmplifierUpgradeableEffects(stack)) {
            return false;
        }
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            if (!supportsAmplifierUpgrade(effect.getEffect())) {
                continue;
            }
            int level = effect.getAmplifier() + 1;
            if (level < maxLevel(effect.getEffect())) {
                return true;
            }
        }
        return false;
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
        return hasEffects(stack) && hasDurationRemaining(stack);
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
        return hasEffects(stack) && hasLevelRemaining(stack);
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
            if (isInstantOnlyPotion(input)) {
                player.displayClientMessage(
                        Component.translatable("gui.craftlevel.altar.potion.instant"),
                        true
                );
                return false;
            }
            if (!hasDurationRemaining(input)) {
                player.displayClientMessage(
                        Component.translatable(
                                "gui.craftlevel.altar.potion.max_duration",
                                formatDuration(maxDurationSeconds(input))
                        ),
                        true
                );
                return false;
            }
        } else if (!canUpgradeEffect(input)) {
            player.displayClientMessage(
                    Component.translatable(
                            "gui.craftlevel.altar.potion.max_level",
                            maxLevel(input)
                    ),
                    true
            );
            return false;
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
                int cap = maxDurationTicks(effect.getEffect());
                int next = Math.min(cap, effect.getDuration() + DURATION_BONUS_TICKS);
                modified.add(new MobEffectInstance(
                        effect.getEffect(),
                        next,
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.isVisible(),
                        effect.showIcon()
                ));
            }
        }
        return finalizeResult(input, modified);
    }

    private static ItemStack applyEffectUpgrade(ItemStack input) {
        List<MobEffectInstance> modified = new ArrayList<>();
        for (MobEffectInstance effect : PotionUtils.getMobEffects(input)) {
            if (!supportsAmplifierUpgrade(effect.getEffect())) {
                modified.add(copyEffect(effect));
                continue;
            }
            int maxAmplifier = Math.max(0, maxLevel(effect.getEffect()) - 1);
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
        return finalizeResult(input, modified);
    }

    private static ItemStack finalizeResult(ItemStack input, List<MobEffectInstance> effects) {
        ItemStack result = input.copyWithCount(1);
        PotionUtils.setPotion(result, Potions.AWKWARD);
        PotionUtils.setCustomEffects(result, effects);
        CompoundTag tag = result.getOrCreateTag();
        tag.remove(LEGACY_UPGRADES_TAG);
        tag.remove(TIME_UPGRADES_TAG);
        tag.remove(STRENGTH_UPGRADES_TAG);
        return result;
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

    public static String formatDuration(int totalSeconds) {
        int seconds = Math.max(0, totalSeconds);
        int minutes = seconds / 60;
        int rem = seconds % 60;
        if (minutes <= 0) {
            return rem + "s";
        }
        if (rem == 0) {
            return minutes + "m";
        }
        return minutes + "m " + rem + "s";
    }

    private static void applyEffects(Level level, BlockPos pos) {
        AltarOfDeathBlockEntity.applyExchangeDarkness(level, pos);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.2F);
    }
}
