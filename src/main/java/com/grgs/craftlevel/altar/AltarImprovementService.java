package com.grgs.craftlevel.altar;

import com.grgs.craftlevel.block.AltarOfDeathBlockEntity;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.config.AltarImprovementParser;
import com.grgs.craftlevel.glory.GloryKillsService;
import com.grgs.craftlevel.menu.AltarOfDeathMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aprimoramento de encantamentos na aba Improvement do altar. */
public final class AltarImprovementService {

    public record EnchantEntry(Enchantment enchantment, int level) {
        public Component displayName() {
            return enchantment.getFullname(level);
        }
    }

    private AltarImprovementService() {}

    public static int gloryCost() {
        return AltarImprovementParser.gloryCost;
    }

    public static int maxLevel(Enchantment enchantment) {
        return AltarImprovementParser.maxLevel(enchantment);
    }

    public static boolean canUpgrade(Enchantment enchantment, int level) {
        return enchantment != null
                && !enchantment.isCurse()
                && level > 0
                && level < maxLevel(enchantment);
    }

    /** Todos os encantamentos do item (para a lista de seleção). */
    public static List<EnchantEntry> listEnchantments(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
        List<EnchantEntry> list = new ArrayList<>(map.size());
        for (Map.Entry<Enchantment, Integer> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                list.add(new EnchantEntry(entry.getKey(), entry.getValue()));
            }
        }
        return list;
    }

    public static boolean hasEnchantments(ItemStack stack) {
        return !listEnchantments(stack).isEmpty();
    }

    public static boolean tryImprove(
            ServerPlayer player,
            AltarOfDeathMenu menu,
            BlockPos altarPos,
            int enchantIndex
    ) {
        if (player.level().isDay()) {
            player.displayClientMessage(
                    Component.translatable("block.craftlevel.altar_of_death.need_night"),
                    true
            );
            return false;
        }

        ItemStack input = menu.getImprovementStack();
        List<EnchantEntry> entries = listEnchantments(input);
        if (enchantIndex < 0 || enchantIndex >= entries.size()) {
            player.displayClientMessage(
                    Component.translatable("gui.craftlevel.altar.improvement.no_selection"),
                    true
            );
            return false;
        }

        EnchantEntry selected = entries.get(enchantIndex);
        if (!canUpgrade(selected.enchantment(), selected.level())) {
            player.displayClientMessage(
                    Component.translatable("gui.craftlevel.altar.improvement.max_level"),
                    true
            );
            return false;
        }

        int cost = gloryCost();
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

            ItemStack result = upgradeEnchantment(input, selected.enchantment());
            if (result.isEmpty()) {
                return false;
            }

            glory.setGloryKills(gloryKills - cost);
            menu.setImprovementStack(result);
            menu.broadcastChanges();

            GloryKillsService.sync(player);
            applyEffects(player.level(), altarPos);
            return true;
        }).orElse(false);
    }

    private static ItemStack upgradeEnchantment(ItemStack input, Enchantment target) {
        Map<Enchantment, Integer> current = EnchantmentHelper.getEnchantments(input);
        Integer level = current.get(target);
        if (level == null || !canUpgrade(target, level)) {
            return ItemStack.EMPTY;
        }

        Map<Enchantment, Integer> next = new LinkedHashMap<>(current);
        next.put(target, level + 1);

        ItemStack result = input.copy();
        EnchantmentHelper.setEnchantments(next, result);
        return result;
    }

    private static void applyEffects(Level level, BlockPos pos) {
        AltarOfDeathBlockEntity.applyExchangeDarkness(level, pos);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.1F);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }
}
