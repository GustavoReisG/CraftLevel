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
            SkillCategory.UTILITY,
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

    private static final List<SkillDefinition> ALL = List.of(
            SENSE_OF_PREY_DEF,
            GHOST_SATURATION_DEF,
            SOUL_ANCHOR_DEF,
            SPECTRAL_CLIMB_DEF,
            IMBUE_MASTER_DEF
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
