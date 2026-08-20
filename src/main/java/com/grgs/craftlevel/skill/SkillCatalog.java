package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** Catálogo imutável de skills. */
public final class SkillCatalog {

    public static final String SENSE_OF_PREY = "sense_of_prey";
    public static final String GHOST_SATURATION = "ghost_saturation";
    public static final String SOUL_ANCHOR = "soul_anchor";
    public static final String SPECTRAL_CLIMB = "spectral_climb";
    public static final String IMBUE_MASTER = "imbue_master";
    public static final String VAMPIRE_ATTACK = "vampire_attack";
    public static final String SPIRIT_RETURN = "spirit_return";
    public static final String CARNAGE = "carnage";
    public static final String SUPERNOVA = "supernova";
    public static final String PROTECTIVE_SHOCKWAVE = "protective_shockwave";
    public static final String SOUL_RENDER = "soul_render";
    public static final String LIGHT_SPEED = "light_speed";
    public static final String DARKNESS_LIGHT = "darkness_light";
    public static final String MINER_FRENZY = "miner_frenzy";
    public static final String LAST_CHANCE = "last_chance";
    public static final String REINFORCED_BONES = "reinforced_bones";
    public static final String MIND_FURY = "mind_fury";
    public static final String NETHER_ELEVATION = "nether_elevation";
    public static final String NETHER_RUSH = "nether_rush";
    public static final String PROSPECTOR = "prospector";
    public static final String FROZEN_MIND = "frozen_mind";
    public static final String FROZEN_RUSH = "frozen_rush";
    public static final String ENDER_BACKPACK = "ender_backpack";
    public static final String VAMPIRE_BLOOD = "vampire_blood";
    public static final String QUICK_SURVIVE = "quick_survive";
    public static final String BLOOD_RUSH = "blood_rush";
    public static final String STRENGTH_RECOVERY = "strength_recovery";
    public static final String PORTABLE_CRAFTING = "portable_crafting";
    public static final String GOODNIGHT = "goodnight";
    public static final String RECALL = "recall";

    public static final SkillDefinition SENSE_OF_PREY_DEF = new SkillDefinition(
            SENSE_OF_PREY,
            SkillKind.ACTIVE,
            SkillCategory.UTILITY,
            5,
            60 * 20,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/sense_of_prey.png"),
            "gui.craftlevel.skill.sense_of_prey",
            "gui.craftlevel.skill.sense_of_prey.desc"
    );

    public static final SkillDefinition GHOST_SATURATION_DEF = new SkillDefinition(
            GHOST_SATURATION,
            SkillKind.ACTIVE,
            SkillCategory.DEFENSE,
            3,
            GhostSaturationSkill.cooldownTicks(1),
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/ghost_saturation.png"),
            "gui.craftlevel.skill.ghost_saturation",
            "gui.craftlevel.skill.ghost_saturation.desc"
    );

    public static final SkillDefinition SOUL_ANCHOR_DEF = new SkillDefinition(
            SOUL_ANCHOR,
            SkillKind.PASSIVE,
            SkillCategory.PASSIVE,
            5,
            0,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/soul_anchor.png"),
            "gui.craftlevel.skill.soul_anchor",
            "gui.craftlevel.skill.soul_anchor.desc"
    );

    public static final SkillDefinition SPECTRAL_CLIMB_DEF = new SkillDefinition(
            SPECTRAL_CLIMB,
            SkillKind.PASSIVE,
            SkillCategory.PASSIVE,
            3,
            0,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/spectral_climb.png"),
            "gui.craftlevel.skill.spectral_climb",
            "gui.craftlevel.skill.spectral_climb.desc"
    );

    public static final SkillDefinition IMBUE_MASTER_DEF = new SkillDefinition(
            IMBUE_MASTER,
            SkillKind.PASSIVE,
            SkillCategory.PASSIVE,
            5,
            0,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/imbue_master.png"),
            "gui.craftlevel.skill.imbue_master",
            "gui.craftlevel.skill.imbue_master.desc"
    );

    public static final SkillDefinition VAMPIRE_ATTACK_DEF = new SkillDefinition(
            VAMPIRE_ATTACK,
            SkillKind.ACTIVE,
            SkillCategory.DAMAGE,
            3,
            VampireAttackSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/vampire_attack.png"),
            "gui.craftlevel.skill.vampire_attack",
            "gui.craftlevel.skill.vampire_attack.desc"
    );

    public static final SkillDefinition SPIRIT_RETURN_DEF = new SkillDefinition(
            SPIRIT_RETURN,
            SkillKind.ACTIVE,
            SkillCategory.DEFENSE,
            3,
            SpiritReturnSkill.cooldownTicks(1),
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/spirit_return.png"),
            "gui.craftlevel.skill.spirit_return",
            "gui.craftlevel.skill.spirit_return.desc"
    );

    public static final SkillDefinition CARNAGE_DEF = new SkillDefinition(
            CARNAGE,
            SkillKind.ACTIVE,
            SkillCategory.DAMAGE,
            3,
            CarnageSkill.cooldownTicks(1),
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/carnage.png"),
            "gui.craftlevel.skill.carnage",
            "gui.craftlevel.skill.carnage.desc"
    );

    public static final SkillDefinition SUPERNOVA_DEF = new SkillDefinition(
            SUPERNOVA,
            SkillKind.ACTIVE,
            SkillCategory.DAMAGE,
            3,
            SupernovaSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/supernova.png"),
            "gui.craftlevel.skill.supernova",
            "gui.craftlevel.skill.supernova.desc"
    );

    public static final SkillDefinition PROTECTIVE_SHOCKWAVE_DEF = new SkillDefinition(
            PROTECTIVE_SHOCKWAVE,
            SkillKind.ACTIVE,
            SkillCategory.DEFENSE,
            3,
            ProtectiveShockwaveSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/protective_shockwave.png"),
            "gui.craftlevel.skill.protective_shockwave",
            "gui.craftlevel.skill.protective_shockwave.desc"
    );

    public static final SkillDefinition SOUL_RENDER_DEF = new SkillDefinition(
            SOUL_RENDER,
            SkillKind.ACTIVE,
            SkillCategory.DAMAGE,
            3,
            SoulRenderSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/soul_render.png"),
            "gui.craftlevel.skill.soul_render",
            "gui.craftlevel.skill.soul_render.desc"
    );

    public static final SkillDefinition LIGHT_SPEED_DEF = new SkillDefinition(
            LIGHT_SPEED,
            SkillKind.ACTIVE,
            SkillCategory.DEFENSE,
            2,
            LightSpeedSkill.cooldownTicks(1),
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/light_speed.png"),
            "gui.craftlevel.skill.light_speed",
            "gui.craftlevel.skill.light_speed.desc"
    );

    public static final SkillDefinition DARKNESS_LIGHT_DEF = new SkillDefinition(
            DARKNESS_LIGHT,
            SkillKind.ACTIVE,
            SkillCategory.UTILITY,
            3,
            DarknessLightSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/darkness_light.png"),
            "gui.craftlevel.skill.darkness_light",
            "gui.craftlevel.skill.darkness_light.desc"
    );

    public static final SkillDefinition MINER_FRENZY_DEF = new SkillDefinition(
            MINER_FRENZY,
            SkillKind.ACTIVE,
            SkillCategory.UTILITY,
            3,
            MinerFrenzySkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/miner_frenzy.png"),
            "gui.craftlevel.skill.miner_frenzy",
            "gui.craftlevel.skill.miner_frenzy.desc"
    );

    public static final SkillDefinition LAST_CHANCE_DEF = new SkillDefinition(
            LAST_CHANCE,
            SkillKind.PASSIVE,
            SkillCategory.PASSIVE,
            1,
            LastChanceSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/last_chance.png"),
            "gui.craftlevel.skill.last_chance",
            "gui.craftlevel.skill.last_chance.desc"
    );

    public static final SkillDefinition REINFORCED_BONES_DEF = new SkillDefinition(
            REINFORCED_BONES,
            SkillKind.PASSIVE,
            SkillCategory.PASSIVE,
            3,
            0,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/reinforced_bones.png"),
            "gui.craftlevel.skill.reinforced_bones",
            "gui.craftlevel.skill.reinforced_bones.desc"
    );

    public static final SkillDefinition MIND_FURY_DEF = new SkillDefinition(
            MIND_FURY,
            SkillKind.ACTIVE,
            SkillCategory.DAMAGE,
            2,
            MindFurySkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/mind_fury.png"),
            "gui.craftlevel.skill.mind_fury",
            "gui.craftlevel.skill.mind_fury.desc"
    );

    public static final SkillDefinition NETHER_ELEVATION_DEF = new SkillDefinition(
            NETHER_ELEVATION,
            SkillKind.ACTIVE,
            SkillCategory.DAMAGE,
            3,
            NetherElevationSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/nether_elevation.png"),
            "gui.craftlevel.skill.nether_elevation",
            "gui.craftlevel.skill.nether_elevation.desc"
    );

    public static final SkillDefinition NETHER_RUSH_DEF = new SkillDefinition(
            NETHER_RUSH,
            SkillKind.ACTIVE,
            SkillCategory.DAMAGE,
            3,
            NetherRushSkill.cooldownTicks(1),
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/nether_rush.png"),
            "gui.craftlevel.skill.nether_rush",
            "gui.craftlevel.skill.nether_rush.desc"
    );

    public static final SkillDefinition PROSPECTOR_DEF = new SkillDefinition(
            PROSPECTOR,
            SkillKind.ACTIVE,
            SkillCategory.UTILITY,
            3,
            ProspectorSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/prospector.png"),
            "gui.craftlevel.skill.prospector",
            "gui.craftlevel.skill.prospector.desc"
    );

    public static final SkillDefinition FROZEN_MIND_DEF = new SkillDefinition(
            FROZEN_MIND,
            SkillKind.ACTIVE,
            SkillCategory.DEFENSE,
            3,
            FrozenMindSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/frozen_mind.png"),
            "gui.craftlevel.skill.frozen_mind",
            "gui.craftlevel.skill.frozen_mind.desc"
    );

    public static final SkillDefinition FROZEN_RUSH_DEF = new SkillDefinition(
            FROZEN_RUSH,
            SkillKind.ACTIVE,
            SkillCategory.DEFENSE,
            3,
            FrozenRushSkill.cooldownTicks(1),
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/frozen_rush.png"),
            "gui.craftlevel.skill.frozen_rush",
            "gui.craftlevel.skill.frozen_rush.desc"
    );

    public static final SkillDefinition ENDER_BACKPACK_DEF = new SkillDefinition(
            ENDER_BACKPACK,
            SkillKind.ACTIVE,
            SkillCategory.UTILITY,
            1,
            EnderBackpackSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/ender_backpack.png"),
            "gui.craftlevel.skill.ender_backpack",
            "gui.craftlevel.skill.ender_backpack.desc"
    );

    public static final SkillDefinition VAMPIRE_BLOOD_DEF = new SkillDefinition(
            VAMPIRE_BLOOD,
            SkillKind.PASSIVE,
            SkillCategory.PASSIVE,
            2,
            0,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/vampire_blood.png"),
            "gui.craftlevel.skill.vampire_blood",
            "gui.craftlevel.skill.vampire_blood.desc"
    );

    public static final SkillDefinition QUICK_SURVIVE_DEF = new SkillDefinition(
            QUICK_SURVIVE,
            SkillKind.PASSIVE,
            SkillCategory.PASSIVE,
            1,
            0,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/quick_survive.png"),
            "gui.craftlevel.skill.quick_survive",
            "gui.craftlevel.skill.quick_survive.desc"
    );

    public static final SkillDefinition BLOOD_RUSH_DEF = new SkillDefinition(
            BLOOD_RUSH,
            SkillKind.PASSIVE,
            SkillCategory.PASSIVE,
            3,
            BloodRushSkill.cooldownTicks(1),
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/blood_rush.png"),
            "gui.craftlevel.skill.blood_rush",
            "gui.craftlevel.skill.blood_rush.desc"
    );

    public static final SkillDefinition STRENGTH_RECOVERY_DEF = new SkillDefinition(
            STRENGTH_RECOVERY,
            SkillKind.PASSIVE,
            SkillCategory.PASSIVE,
            3,
            StrengthRecoverySkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/strength_recovery.png"),
            "gui.craftlevel.skill.strength_recovery",
            "gui.craftlevel.skill.strength_recovery.desc"
    );

    public static final SkillDefinition PORTABLE_CRAFTING_DEF = new SkillDefinition(
            PORTABLE_CRAFTING,
            SkillKind.ACTIVE,
            SkillCategory.UTILITY,
            1,
            PortableCraftingSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/portable_crafting.png"),
            "gui.craftlevel.skill.portable_crafting",
            "gui.craftlevel.skill.portable_crafting.desc"
    );

    public static final SkillDefinition GOODNIGHT_DEF = new SkillDefinition(
            GOODNIGHT,
            SkillKind.ACTIVE,
            SkillCategory.UTILITY,
            1,
            GoodnightSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/goodnight.png"),
            "gui.craftlevel.skill.goodnight",
            "gui.craftlevel.skill.goodnight.desc"
    );

    public static final SkillDefinition RECALL_DEF = new SkillDefinition(
            RECALL,
            SkillKind.ACTIVE,
            SkillCategory.UTILITY,
            1,
            RecallSkill.COOLDOWN_TICKS,
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/recall.png"),
            "gui.craftlevel.skill.recall",
            "gui.craftlevel.skill.recall.desc"
    );

    private static final List<SkillDefinition> ALL = List.of(
            SENSE_OF_PREY_DEF,
            GHOST_SATURATION_DEF,
            SOUL_ANCHOR_DEF,
            SPECTRAL_CLIMB_DEF,
            IMBUE_MASTER_DEF,
            VAMPIRE_ATTACK_DEF,
            SPIRIT_RETURN_DEF,
            CARNAGE_DEF,
            SUPERNOVA_DEF,
            PROTECTIVE_SHOCKWAVE_DEF,
            SOUL_RENDER_DEF,
            LIGHT_SPEED_DEF,
            DARKNESS_LIGHT_DEF,
            MINER_FRENZY_DEF,
            LAST_CHANCE_DEF,
            REINFORCED_BONES_DEF,
            MIND_FURY_DEF,
            NETHER_ELEVATION_DEF,
            NETHER_RUSH_DEF,
            PROSPECTOR_DEF,
            FROZEN_MIND_DEF,
            FROZEN_RUSH_DEF,
            ENDER_BACKPACK_DEF,
            VAMPIRE_BLOOD_DEF,
            QUICK_SURVIVE_DEF,
            BLOOD_RUSH_DEF,
            STRENGTH_RECOVERY_DEF,
            PORTABLE_CRAFTING_DEF,
            GOODNIGHT_DEF,
            RECALL_DEF
    );

    private SkillCatalog() {}

    public static List<SkillDefinition> all() {
        return ALL;
    }

    public static List<SkillDefinition> byKind(SkillKind kind) {
        return ALL.stream().filter(def -> def.kind() == kind).toList();
    }

    public static List<SkillDefinition> byCategory(SkillCategory category) {
        return ALL.stream().filter(def -> def.category() == category).toList();
    }

    public static Optional<SkillDefinition> byId(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        return ALL.stream().filter(def -> def.id().equals(id)).findFirst();
    }
}
