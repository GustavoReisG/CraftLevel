package com.grgs.craftlevel.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** NBT e regras de imbue em armas/tools. */
public final class ItemImbue {

    public static final String TAG = "CraftLevelImbue";
    public static final String AMOUNT = "Amount";

    public static final int MAX = 1000;
    public static final int PER_POTION = 250;
    public static final int COST_PER_HIT = 10;
    /** Duração do efeito no alvo = duração da poção / este valor. */
    public static final int EFFECT_DURATION_DIVISOR = 10;
    /**
     * Intensidade de efeitos instantâneos ({@code Heal}/{@code Harm}).
     * A fórmula vanilla é {@code (int)(intensity * (4 << amplifier) + 0.5)}.
     * {@code 1.0} = poção cheia por hit (OP em arma); {@code 0.25} ≈ 1 HP (amp 0) / 2 HP (amp 1).
     */
    public static final double INSTANT_INTENSITY = 0.25D;

    private ItemImbue() {}

    public static boolean isWeaponOrTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof TridentItem;
    }

    public static boolean hasImbue(ItemStack stack) {
        return getAmount(stack) > 0 && !getEffects(stack).isEmpty();
    }

    public static int getAmount(ItemStack stack) {
        CompoundTag tag = getImbueTag(stack);
        return tag == null ? 0 : Math.max(0, tag.getInt(AMOUNT));
    }

    public static List<MobEffectInstance> getEffects(ItemStack stack) {
        CompoundTag tag = getImbueTag(stack);
        if (tag == null) {
            return Collections.emptyList();
        }
        return PotionUtils.getAllEffects(tag);
    }

    public static int getColor(ItemStack stack) {
        CompoundTag tag = getImbueTag(stack);
        if (tag == null) {
            return PotionUtils.getColor(Collections.emptyList());
        }
        return PotionUtils.getColor(PotionUtils.getAllEffects(tag));
    }

    /** Poção de água: limpa o imbue da arma/tool. */
    public static boolean isCleaningPotion(ItemStack potion) {
        return !potion.isEmpty() && PotionUtils.getPotion(potion) == Potions.WATER;
    }

    /**
     * Mesmo ID de poção ({@code Potion} NBT), ou mesmos {@code CustomPotionEffects} se não houver ID.
     * Arma sem imbue aceita qualquer poção.
     */
    public static boolean isSamePotionId(ItemStack weapon, ItemStack potion) {
        if (!hasImbue(weapon)) {
            return true;
        }
        CompoundTag imbue = getImbueTag(weapon);
        if (imbue == null) {
            return true;
        }

        String weaponId = potionId(imbue);
        String applyId = potionId(potion.getTag());
        if (weaponId != null || applyId != null) {
            return Objects.equals(weaponId, applyId);
        }
        return customEffectsEqual(imbue, potion.getTag());
    }

    public static boolean canApplyFromPotion(ItemStack weapon, ItemStack potion) {
        if (!isWeaponOrTool(weapon) || potion.isEmpty()) {
            return false;
        }
        if (isCleaningPotion(potion)) {
            return hasImbue(weapon) || getAmount(weapon) > 0;
        }
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(potion);
        if (effects.isEmpty()) {
            return false;
        }
        if (getAmount(weapon) >= MAX) {
            return false;
        }
        return isSamePotionId(weapon, potion);
    }

    /**
     * Aplica a poção na arma/tool: +{@code amount} (cap {@link #MAX}) e grava o efeito.
     * Só aceita o mesmo ID de poção se a arma já tiver imbue.
     *
     * @return {@code true} se aplicou
     */
    public static boolean applyFromPotion(ItemStack weapon, ItemStack potion, int amount) {
        if (!canApplyFromPotion(weapon, potion) || isCleaningPotion(potion)) {
            return false;
        }

        int add = Math.max(0, amount);
        int current = getAmount(weapon);
        CompoundTag imbue = weapon.getOrCreateTagElement(TAG);
        imbue.putInt(AMOUNT, Math.min(MAX, current + add));

        CompoundTag potionTag = potion.getTag();
        if (potionTag != null && potionTag.contains("Potion", Tag.TAG_STRING)) {
            imbue.putString("Potion", potionTag.getString("Potion"));
        } else {
            imbue.remove("Potion");
        }
        if (potionTag != null && potionTag.contains("CustomPotionEffects", Tag.TAG_LIST)) {
            imbue.put("CustomPotionEffects", potionTag.getList("CustomPotionEffects", Tag.TAG_COMPOUND).copy());
        } else {
            imbue.remove("CustomPotionEffects");
        }
        if (potionTag != null && potionTag.contains("CustomPotionColor", Tag.TAG_INT)) {
            imbue.putInt("CustomPotionColor", potionTag.getInt("CustomPotionColor"));
        } else {
            imbue.remove("CustomPotionColor");
        }
        return true;
    }

    /** Aplica com a quantidade base ({@link #PER_POTION}). */
    public static boolean applyFromPotion(ItemStack weapon, ItemStack potion) {
        return applyFromPotion(weapon, potion, PER_POTION);
    }

    /** Remove o imbue da arma/tool. */
    public static boolean clear(ItemStack weapon) {
        if (!isWeaponOrTool(weapon)) {
            return false;
        }
        CompoundTag root = weapon.getTag();
        if (root == null || !root.contains(TAG, Tag.TAG_COMPOUND)) {
            return false;
        }
        root.remove(TAG);
        if (root.isEmpty()) {
            weapon.setTag(null);
        }
        return true;
    }

    /** Consome imbue; remove a tag se zerar. */
    public static void consume(ItemStack weapon, int amount) {
        CompoundTag tag = getImbueTag(weapon);
        if (tag == null || amount <= 0) {
            return;
        }
        int next = Math.max(0, tag.getInt(AMOUNT) - amount);
        if (next <= 0) {
            if (weapon.getTag() != null) {
                weapon.getTag().remove(TAG);
                if (weapon.getTag().isEmpty()) {
                    weapon.setTag(null);
                }
            }
            return;
        }
        tag.putInt(AMOUNT, next);
    }

    /** Efeitos com duração / {@link #EFFECT_DURATION_DIVISOR} (mín. 1 tick se não for instantâneo). */
    public static List<MobEffectInstance> effectsForHit(ItemStack weapon) {
        List<MobEffectInstance> source = getEffects(weapon);
        if (source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.stream().map(effect -> {
            if (effect.getEffect().isInstantenous()) {
                return new MobEffectInstance(effect);
            }
            int duration = Math.max(1, effect.getDuration() / EFFECT_DURATION_DIVISOR);
            return new MobEffectInstance(
                    effect.getEffect(),
                    duration,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            );
        }).toList();
    }

    private static String potionId(CompoundTag tag) {
        if (tag == null || !tag.contains("Potion", Tag.TAG_STRING)) {
            return null;
        }
        return tag.getString("Potion");
    }

    private static boolean customEffectsEqual(CompoundTag imbue, CompoundTag potionTag) {
        ListTag a = imbue.contains("CustomPotionEffects", Tag.TAG_LIST)
                ? imbue.getList("CustomPotionEffects", Tag.TAG_COMPOUND)
                : null;
        ListTag b = potionTag != null && potionTag.contains("CustomPotionEffects", Tag.TAG_LIST)
                ? potionTag.getList("CustomPotionEffects", Tag.TAG_COMPOUND)
                : null;
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.equals(b);
    }

    private static CompoundTag getImbueTag(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        return root.getCompound(TAG);
    }
}
