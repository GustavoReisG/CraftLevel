package com.grgs.craftlevel.skill;

import net.minecraft.resources.ResourceLocation;

/** Definição imutável de uma skill (efeito/CD no código; custo/min lvl no JSON). */
public record SkillDefinition(
        String id,
        SkillKind kind,
        SkillCategory category,
        int maxLevel,
        int cooldownTicks,
        ResourceLocation icon,
        String titleKey,
        String descKey
) {
}
