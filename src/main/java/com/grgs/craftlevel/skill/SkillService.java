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
            if (SkillCatalog.VAMPIRE_BLOOD.equals(skillId)) {
                VampireBloodSkill.refreshHealthBonus(player);
            }
        });
    }

    public static void useSkill(ServerPlayer player, String skillId) {
        useSkill(player, skillId, -1);
    }

    public static void useSkill(ServerPlayer player, String skillId, int targetEntityId) {
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
            } else if (SkillCatalog.VAMPIRE_ATTACK.equals(skillId)) {
                VampireAttackSkill.activate(player, level);
            } else if (SkillCatalog.SPIRIT_RETURN.equals(skillId)) {
                SpiritReturnSkill.activate(player, level);
            } else if (SkillCatalog.CARNAGE.equals(skillId)) {
                if (!CarnageSkill.activate(player, level)) {
                    return;
                }
                // CD começa só após a última carga (ou fim da janela sem cargas).
                GloryKillsService.sync(player);
                return;
            } else if (SkillCatalog.SUPERNOVA.equals(skillId)) {
                SupernovaSkill.activate(player, level);
            } else if (SkillCatalog.PROTECTIVE_SHOCKWAVE.equals(skillId)) {
                ProtectiveShockwaveSkill.activate(player, level);
            } else if (SkillCatalog.SOUL_RENDER.equals(skillId)) {
                SoulRenderSkill.activate(player, level);
            } else if (SkillCatalog.LIGHT_SPEED.equals(skillId)) {
                LightSpeedSkill.activate(player, level);
            } else if (SkillCatalog.DARKNESS_LIGHT.equals(skillId)) {
                DarknessLightSkill.activate(player, level);
            } else if (SkillCatalog.MINER_FRENZY.equals(skillId)) {
                MinerFrenzySkill.activate(player, level);
            } else if (SkillCatalog.MIND_FURY.equals(skillId)) {
                MindFurySkill.activate(player, level);
            } else if (SkillCatalog.NETHER_ELEVATION.equals(skillId)) {
                if (!NetherElevationSkill.activate(player, level, targetEntityId)) {
                    return;
                }
            } else if (SkillCatalog.NETHER_RUSH.equals(skillId)) {
                NetherRushSkill.activate(player, level);
            } else if (SkillCatalog.FROZEN_RUSH.equals(skillId)) {
                FrozenRushSkill.activate(player, level);
            } else if (SkillCatalog.PROSPECTOR.equals(skillId)) {
                ProspectorSkill.activate(player, level);
            } else if (SkillCatalog.FROZEN_MIND.equals(skillId)) {
                if (!FrozenMindSkill.activate(player, level, targetEntityId)) {
                    return;
                }
            } else if (SkillCatalog.ENDER_BACKPACK.equals(skillId)) {
                EnderBackpackSkill.activate(player, level);
            } else if (SkillCatalog.PORTABLE_CRAFTING.equals(skillId)) {
                PortableCraftingSkill.activate(player, level);
            } else if (SkillCatalog.GOODNIGHT.equals(skillId)) {
                if (!GoodnightSkill.activate(player, level)) {
                    return;
                }
            } else if (SkillCatalog.RECALL.equals(skillId)) {
                if (!RecallSkill.activate(player, level)) {
                    return;
                }
                GloryKillsService.sync(player);
                return;
            } else if (SkillCatalog.SKY_WALKER.equals(skillId)) {
                SkyWalkerSkill.activate(player, level);
            } else if (SkillCatalog.INFERNO_BOLT.equals(skillId)) {
                InfernoBoltSkill.activate(player, level);
            } else if (SkillCatalog.SOUL_LANCE.equals(skillId)) {
                SoulLanceSkill.activate(player, level);
            } else if (SkillCatalog.BLINK_STEP.equals(skillId)) {
                if (!BlinkStepSkill.activate(player, level)) {
                    return;
                }
            } else if (SkillCatalog.CRIPPLING_AURA.equals(skillId)) {
                CripplingAuraSkill.activate(player, level);
            } else if (SkillCatalog.TERROR_PULSE.equals(skillId)) {
                TerrorPulseSkill.activate(player, level);
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
