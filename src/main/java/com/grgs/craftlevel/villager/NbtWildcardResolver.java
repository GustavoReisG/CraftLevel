package com.grgs.craftlevel.villager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolve curingas {@code "*"} em valores NBT.
 * <ul>
 *   <li>{@code id:"*"} / {@code lvl:"*"} → encantamento tradeable + nível</li>
 *   <li>{@code id:"*"} + {@code level} → spell aleatório do Iron's Spells (se presente)</li>
 *   <li>{@code Potion:"*"} → poção aleatória do registry (exceto empty)</li>
 * </ul>
 */
public final class NbtWildcardResolver {

    public static final String WILDCARD = "*";

    private NbtWildcardResolver() {}

    public static boolean containsWildcard(String snbt) {
        return snbt != null && snbt.contains("\"" + WILDCARD + "\"");
    }

    public static void resolve(CompoundTag root, RandomSource random) {
        if (root == null || random == null) {
            return;
        }
        walk(root, random);
    }

    private static void walk(Tag tag, RandomSource random) {
        if (tag instanceof CompoundTag compound) {
            resolvePotionTag(compound, random);
            resolveSpellCompound(compound, random);
            resolveEnchantmentCompound(compound, random);
            for (String key : List.copyOf(compound.getAllKeys())) {
                Tag child = compound.get(key);
                if (child != null) {
                    walk(child, random);
                }
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                walk(list.get(i), random);
            }
        }
    }

    private static void resolvePotionTag(CompoundTag compound, RandomSource random) {
        if (!isWildcardString(compound.get("Potion"))) {
            return;
        }
        Potion potion = pickPotion(random);
        if (potion == null) {
            return;
        }
        ResourceLocation key = BuiltInRegistries.POTION.getKey(potion);
        if (key == null) {
            return;
        }
        compound.putString("Potion", key.toString());
    }

    /** ISP scroll entries usam {@code level}; encantamentos usam {@code lvl}. */
    private static void resolveSpellCompound(CompoundTag compound, RandomSource random) {
        if (!isWildcardString(compound.get("id"))) {
            return;
        }
        if (!compound.contains("level") || compound.contains("lvl")) {
            return;
        }
        String spellId = pickIronSpellId(random);
        if (spellId == null) {
            return;
        }
        compound.putString("id", spellId);
    }

    private static void resolveEnchantmentCompound(CompoundTag compound, RandomSource random) {
        // Evita tratar entry de spell (Iron's Spells) como encantamento.
        if (compound.contains("level") && !compound.contains("lvl")) {
            return;
        }

        boolean idWildcard = isWildcardString(compound.get("id"));
        boolean lvlWildcard = isWildcardString(compound.get("lvl"));
        if (!idWildcard && !lvlWildcard) {
            return;
        }

        Enchantment enchantment;
        if (idWildcard) {
            enchantment = pickTradeableEnchantment(random, lvlWildcard ? -1 : readLevelHint(compound));
            if (enchantment == null) {
                return;
            }
            ResourceLocation key = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
            if (key == null) {
                return;
            }
            compound.putString("id", key.toString());
        } else {
            enchantment = readEnchantment(compound);
            if (enchantment == null) {
                return;
            }
        }

        if (lvlWildcard) {
            int level = Mth.nextInt(random, enchantment.getMinLevel(), enchantment.getMaxLevel());
            compound.putShort("lvl", (short) level);
        } else if (compound.contains("lvl")) {
            int level = readLevelHint(compound);
            if (level >= 0) {
                compound.putShort(
                        "lvl",
                        (short) Mth.clamp(level, enchantment.getMinLevel(), enchantment.getMaxLevel())
                );
            }
        }
    }

    private static boolean isWildcardString(Tag tag) {
        return tag instanceof StringTag stringTag && WILDCARD.equals(stringTag.getAsString());
    }

    private static Enchantment readEnchantment(CompoundTag compound) {
        if (!compound.contains("id", Tag.TAG_STRING)) {
            return null;
        }
        ResourceLocation key = ResourceLocation.tryParse(compound.getString("id"));
        if (key == null) {
            return null;
        }
        return BuiltInRegistries.ENCHANTMENT.get(key);
    }

    private static int readLevelHint(CompoundTag compound) {
        Tag lvl = compound.get("lvl");
        if (lvl instanceof NumericTag numeric) {
            return numeric.getAsInt();
        }
        return -1;
    }

    private static Enchantment pickTradeableEnchantment(RandomSource random, int requiredLevel) {
        List<Enchantment> options = new ArrayList<>();
        for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
            if (!enchantment.isTradeable()) {
                continue;
            }
            if (requiredLevel >= 0
                    && (requiredLevel < enchantment.getMinLevel() || requiredLevel > enchantment.getMaxLevel())) {
                continue;
            }
            options.add(enchantment);
        }
        if (options.isEmpty()) {
            return null;
        }
        return options.get(random.nextInt(options.size()));
    }

    private static Potion pickPotion(RandomSource random) {
        List<Potion> options = new ArrayList<>();
        for (Potion potion : BuiltInRegistries.POTION) {
            if (potion == Potions.EMPTY) {
                continue;
            }
            options.add(potion);
        }
        if (options.isEmpty()) {
            return null;
        }
        return options.get(random.nextInt(options.size()));
    }

    /** Reflection — sem dependência hard em Iron's Spells. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String pickIronSpellId(RandomSource random) {
        try {
            Class<?> spellRegistry = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
            Object registrySupplier = spellRegistry.getField("REGISTRY").get(null);
            Object registry = registrySupplier.getClass().getMethod("get").invoke(registrySupplier);
            java.util.Collection values =
                    (java.util.Collection) registry.getClass().getMethod("getValues").invoke(registry);
            List<String> ids = new ArrayList<>();
            for (Object spell : values) {
                Object key = registry.getClass().getMethod("getKey", Object.class).invoke(registry, spell);
                if (key == null) {
                    continue;
                }
                String id = key.toString();
                if (id.endsWith(":none") || id.equals("none")) {
                    continue;
                }
                ids.add(id);
            }
            if (ids.isEmpty()) {
                return null;
            }
            return ids.get(random.nextInt(ids.size()));
        } catch (Throwable ignored) {
            return null;
        }
    }
}
