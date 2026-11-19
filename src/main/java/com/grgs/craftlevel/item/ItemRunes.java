package com.grgs.craftlevel.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** NBT e regras das runas em armas/armaduras (máx. 2 por item). */
public final class ItemRunes {

    public static final String TAG = "CraftLevelRunes";
    public static final String LIST = "List";
    public static final String BROKEN = "Broken";
    public static final int MAX_PER_ITEM = 2;

    public static final float VELOCE_PER_ITEM = 0.05F;
    public static final float REACH_PER_ITEM = 0.10F;
    public static final float FORTUNE_PER_ITEM = 0.05F;
    public static final float RETURN_PER_ITEM = 0.05F;

    private ItemRunes() {}

    public static boolean canReceiveRunes(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof ArmorItem
                || item instanceof ElytraItem
                || item instanceof SwordItem
                || item instanceof AxeItem
                || item instanceof TridentItem
                || item instanceof BowItem
                || item instanceof CrossbowItem;
    }

    public static List<RuneKind> get(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null) {
            return Collections.emptyList();
        }
        ListTag list = root.getList(LIST, Tag.TAG_STRING);
        List<RuneKind> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            RuneKind kind = RuneKind.byId(list.getString(i));
            if (kind != null && !result.contains(kind)) {
                result.add(kind);
            }
        }
        return result;
    }

    public static boolean has(ItemStack stack, RuneKind kind) {
        return get(stack).contains(kind);
    }

    public static int count(ItemStack stack) {
        return get(stack).size();
    }

    public static boolean canApply(ItemStack gear, RuneKind kind) {
        if (!canReceiveRunes(gear) || kind == null) {
            return false;
        }
        List<RuneKind> current = get(gear);
        if (current.contains(kind)) {
            return false;
        }
        return current.size() < MAX_PER_ITEM;
    }

    /** Copia o gear e adiciona a runa (sem consumir o item de runa). */
    public static ItemStack withRune(ItemStack gear, RuneKind kind) {
        ItemStack out = gear.copy();
        out.setCount(1);
        if (!canApply(out, kind)) {
            return ItemStack.EMPTY;
        }
        CompoundTag root = out.getOrCreateTagElement(TAG);
        ListTag list = root.contains(LIST, Tag.TAG_LIST)
                ? root.getList(LIST, Tag.TAG_STRING)
                : new ListTag();
        list.add(StringTag.valueOf(kind.getSerializedName()));
        root.put(LIST, list);
        return out;
    }

    public static boolean isBroken(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        return root != null && root.getBoolean(BROKEN);
    }

    public static void setBroken(ItemStack stack, boolean broken) {
        if (stack.isEmpty()) {
            return;
        }
        if (broken) {
            stack.getOrCreateTagElement(TAG).putBoolean(BROKEN, true);
        } else {
            CompoundTag root = getRoot(stack);
            if (root == null) {
                return;
            }
            root.remove(BROKEN);
            if (root.isEmpty()) {
                if (stack.getTag() != null) {
                    stack.getTag().remove(TAG);
                    if (stack.getTag().isEmpty()) {
                        stack.setTag(null);
                    }
                }
            }
        }
    }

    /** Conta peças ativas (não quebradas) com a runa no inventário de equipamento + mãos. */
    public static int countActive(LivingEntity entity, RuneKind kind) {
        int total = 0;
        for (ItemStack stack : relevantStacks(entity)) {
            if (!stack.isEmpty() && !isBroken(stack) && has(stack, kind)) {
                total++;
            }
        }
        return total;
    }

    public static boolean hasActive(LivingEntity entity, RuneKind kind) {
        return countActive(entity, kind) > 0;
    }

    public static Set<RuneKind> activeKinds(LivingEntity entity) {
        EnumSet<RuneKind> set = EnumSet.noneOf(RuneKind.class);
        for (ItemStack stack : relevantStacks(entity)) {
            if (stack.isEmpty() || isBroken(stack)) {
                continue;
            }
            set.addAll(get(stack));
        }
        return set;
    }

    private static Iterable<ItemStack> relevantStacks(LivingEntity entity) {
        List<ItemStack> stacks = new ArrayList<>(6);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                stacks.add(entity.getItemBySlot(slot));
            }
        }
        stacks.add(entity.getMainHandItem());
        stacks.add(entity.getOffhandItem());
        return stacks;
    }

    @Nullable
    private static CompoundTag getRoot(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        return tag.getCompound(TAG);
    }

    public static void clearBrokenIfRepaired(ItemStack stack) {
        if (!isBroken(stack) || !stack.isDamageableItem()) {
            return;
        }
        // Fora do estado “quase destruído”: considera reparado.
        if (stack.getDamageValue() < stack.getMaxDamage() - 1) {
            setBroken(stack, false);
        }
    }

    public static boolean tryPreventBreak(ItemStack stack) {
        if (stack.isEmpty() || !has(stack, RuneKind.UNBREAKABLE) || !stack.isDamageableItem()) {
            return false;
        }
        int max = stack.getMaxDamage();
        if (max <= 1) {
            return false;
        }
        stack.setDamageValue(max - 1);
        setBroken(stack, true);
        return true;
    }

    public static float veloceMultiplier(Player player) {
        return 1.0F + VELOCE_PER_ITEM * countActive(player, RuneKind.VELOCE);
    }

    public static float reachMultiplier(Player player) {
        return 1.0F + REACH_PER_ITEM * countActive(player, RuneKind.REACH);
    }

    public static float fortuneBonus(Player player) {
        return FORTUNE_PER_ITEM * countActive(player, RuneKind.FORTUNE);
    }

    public static float returnChance(Player player) {
        return RETURN_PER_ITEM * countActive(player, RuneKind.RETURN);
    }

    public static boolean isSoulbound(ItemStack stack) {
        return !stack.isEmpty() && has(stack, RuneKind.SOULBOUND);
    }
}
