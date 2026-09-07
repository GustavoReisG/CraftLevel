package com.grgs.craftlevel.altar;

import com.grgs.craftlevel.block.AltarOfDeathBlockEntity;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.config.AltarConversionParser;
import com.grgs.craftlevel.config.AltarConversionParser.ConversionRule;
import com.grgs.craftlevel.glory.GloryKillsService;
import com.grgs.craftlevel.menu.AltarOfDeathMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Conversão de itens + Glory em Skill Points na aba Conversion. */
public final class AltarConversionService {

    private AltarConversionService() {}

    public static boolean tryConvert(
            ServerPlayer player,
            AltarOfDeathMenu menu,
            BlockPos altarPos,
            boolean batch
    ) {
        if (player.level().isDay()) {
            player.displayClientMessage(
                    Component.translatable("block.craftlevel.altar_of_death.need_night"),
                    true
            );
            return false;
        }

        ItemStack input = menu.getConversionStack();
        if (input.isEmpty()) {
            return false;
        }

        var ruleOpt = AltarConversionParser.findMatch(input);
        if (ruleOpt.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("gui.craftlevel.altar.conversion.invalid_item"),
                    true
            );
            return false;
        }

        ConversionRule rule = ruleOpt.get();
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY).map(glory -> {
            int gloryKills = glory.getGloryKills();
            int maxByGlory = gloryKills / rule.gloryCost();
            if (maxByGlory <= 0) {
                player.displayClientMessage(
                        Component.translatable(
                                "block.craftlevel.altar_of_death.need_kills",
                                rule.gloryCost(),
                                gloryKills
                        ),
                        true
                );
                return false;
            }

            int maxByItems = input.getCount() / rule.count();
            int conversions = batch
                    ? Math.min(maxByGlory, maxByItems)
                    : 1;
            if (conversions <= 0) {
                return false;
            }

            int glorySpent = rule.gloryCost() * conversions;
            int skillGained = rule.skillPoints() * conversions;
            glory.setGloryKills(gloryKills - glorySpent);
            glory.setSkillPoints(glory.getSkillPoints() + skillGained);

            if (!player.getAbilities().instabuild) {
                int consumed = rule.count() * conversions;
                ItemStack remaining = input.copy();
                remaining.shrink(consumed);
                menu.setConversionStack(remaining.isEmpty() ? ItemStack.EMPTY : remaining);
            }
            menu.broadcastChanges();

            GloryKillsService.sync(player);
            sendSuccessMessage(player, glory.getSkillPoints(), skillGained);
            applyEffects(player.level(), altarPos, conversions);
            return true;
        }).orElse(false);
    }

    private static void sendSuccessMessage(ServerPlayer player, int totalSp, int gained) {
        Component message = gained == 1
                ? Component.translatable(
                        "block.craftlevel.altar_of_death.skill_point_gained",
                        totalSp
                )
                : Component.translatable(
                        "block.craftlevel.altar_of_death.skill_points_gained",
                        gained,
                        totalSp
                );
        player.displayClientMessage(message, true);
    }

    private static void applyEffects(Level level, BlockPos pos, int conversions) {
        AltarOfDeathBlockEntity.applyExchangeDarkness(level, pos);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.2F);
        level.playSound(
                null,
                pos,
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS,
                0.8F,
                1.0F + conversions * 0.05F
        );
    }
}
