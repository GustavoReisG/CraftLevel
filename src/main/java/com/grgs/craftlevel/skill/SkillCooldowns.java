package com.grgs.craftlevel.skill;

/** Cooldown efetivo por skill/nível (código; JSON só custo/min lvl). */
public final class SkillCooldowns {

    private SkillCooldowns() {}

    public static int ticks(String skillId, int level) {
        if (SkillCatalog.GHOST_SATURATION.equals(skillId)) {
            return GhostSaturationSkill.cooldownTicks(level);
        }
        return SkillCatalog.byId(skillId)
                .map(SkillDefinition::cooldownTicks)
                .orElse(20);
    }

    public static int seconds(String skillId, int level) {
        return Math.max(1, ticks(skillId, level) / 20);
    }
}
