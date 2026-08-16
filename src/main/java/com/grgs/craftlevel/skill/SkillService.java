package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.capability.IPlayerGlory;
import com.grgs.craftlevel.capability.PlayerGlory;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.SkillsParser;
import com.grgs.craftlevel.glory.GloryKillsService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Unlock/upgrade, hotbar e uso de skills no servidor. */
public final class SkillService {

    private SkillService() {}

    public static void spendForNextLevel(ServerPlayer player, String skillId) {
        Optional<SkillDefinition> optional = SkillCatalog.byId(skillId);
        if (optional.isEmpty()) {
            return;
        }
        SkillDefinition def = optional.get();
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            int current = glory.getSkillLevel(skillId);
            int next = current + 1;
            if (next > def.maxLevel()) {
                player.displayClientMessage(Component.translatable("gui.craftlevel.skill.max"), true);
                return;
            }

            SkillsParser.LevelConfig config = SkillsParser.forLevel(skillId, next);
            int playerLevel = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL)
                    .map(cap -> Math.max(1, cap.getLevel()))
                    .orElse(1);
            boolean creative = player.getAbilities().instabuild;
            if (!creative && playerLevel < config.minPlayerLevel()) {
                player.displayClientMessage(
                        Component.translatable(
                                "gui.craftlevel.skill.need_level",
                                config.minPlayerLevel(),
                                playerLevel
                        ),
                        true
                );
                return;
            }
            if (!creative && glory.getSkillPoints() < config.skillPointCost()) {
                player.displayClientMessage(
                        Component.translatable(
                                "gui.craftlevel.skill.need_points",
                                config.skillPointCost(),
                                glory.getSkillPoints()
                        ),
                        true
                );
                return;
            }

            if (!creative) {
                glory.setSkillPoints(glory.getSkillPoints() - config.skillPointCost());
            }
            glory.setSkillLevel(skillId, next);
            GloryKillsService.sync(player);
        });
    }

    public static void useSkill(ServerPlayer player, String skillId) {
        Optional<SkillDefinition> optional = SkillCatalog.byId(skillId);
        if (optional.isEmpty()) {
            return;
        }
        SkillDefinition def = optional.get();
        if (def.kind() != SkillKind.ACTIVE) {
            return;
        }
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            int level = glory.getSkillLevel(skillId);
            if (level <= 0) {
                player.displayClientMessage(Component.translatable("gui.craftlevel.skill.locked"), true);
                return;
            }
            long now = player.serverLevel().getGameTime();
            long readyAt = glory.getCooldownReadyAt(skillId);
            if (readyAt > now) {
                int remainingSec = (int) Math.ceil((readyAt - now) / 20.0);
                player.displayClientMessage(
                        Component.translatable("gui.craftlevel.skill.cooldown", remainingSec),
                        true
                );
                return;
            }

            if (SkillCatalog.SENSE_OF_PREY.equals(skillId)) {
                SenseOfPreySkill.activate(player, level);
            } else if (SkillCatalog.GHOST_SATURATION.equals(skillId)) {
                GhostSaturationSkill.activate(player, level);
            } else {
                return;
            }

            glory.setCooldownReadyAt(skillId, now + SkillCooldowns.ticks(skillId, level));
            GloryKillsService.sync(player);
        });
    }

    public static void assignHotbar(ServerPlayer player, String skillId, int slot) {
        if (slot < 0 || slot >= PlayerGlory.SPELL_HOTBAR_SIZE) {
            return;
        }
        Optional<SkillDefinition> optional = SkillCatalog.byId(skillId);
        if (optional.isEmpty() || optional.get().kind() != SkillKind.ACTIVE) {
            return;
        }
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            if (glory.getSkillLevel(skillId) <= 0) {
                player.displayClientMessage(Component.translatable("gui.craftlevel.skill.locked"), true);
                return;
            }

            String[] hotbar = glory.getSpellHotbar();
            if (skillId.equals(hotbar[slot])) {
                glory.setSpellHotbarSlot(slot, null);
                GloryKillsService.sync(player);
                return;
            }

            for (int i = 0; i < hotbar.length; i++) {
                if (skillId.equals(hotbar[i])) {
                    glory.setSpellHotbarSlot(i, null);
                }
            }
            glory.setSpellHotbarSlot(slot, skillId);
            GloryKillsService.sync(player);
        });
    }

    public static int remainingCooldownTicks(IPlayerGlory glory, String skillId, long gameTime) {
        long readyAt = glory.getCooldownReadyAt(skillId);
        return (int) Math.max(0L, readyAt - gameTime);
    }
}
