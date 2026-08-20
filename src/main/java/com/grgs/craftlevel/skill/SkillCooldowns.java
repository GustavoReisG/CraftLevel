package com.grgs.craftlevel.skill;

/** Cooldown efetivo por skill/nível (código; JSON só custo/min lvl). */
public final class SkillCooldowns {

    private SkillCooldowns() {}

    public static int ticks(String skillId, int level) {
        if (SkillCatalog.GHOST_SATURATION.equals(skillId)) {
            return GhostSaturationSkill.cooldownTicks(level);
        }
        if (SkillCatalog.SPIRIT_RETURN.equals(skillId)) {
            return SpiritReturnSkill.cooldownTicks(level);
        }
        if (SkillCatalog.CARNAGE.equals(skillId)) {
            return CarnageSkill.cooldownTicks(level);
        }
        if (SkillCatalog.LIGHT_SPEED.equals(skillId)) {
            return LightSpeedSkill.cooldownTicks(level);
        }
        if (SkillCatalog.NETHER_RUSH.equals(skillId)) {
            return NetherRushSkill.cooldownTicks(level);
        }
        if (SkillCatalog.FROZEN_RUSH.equals(skillId)) {
            return FrozenRushSkill.cooldownTicks(level);
        }
        if (SkillCatalog.BLOOD_RUSH.equals(skillId)) {
            return BloodRushSkill.cooldownTicks(level);
        }
        return SkillCatalog.byId(skillId)
                .map(SkillDefinition::cooldownTicks)
                .orElse(20);
    }

    public static int seconds(String skillId, int level) {
        return Math.max(1, ticks(skillId, level) / 20);
    }
}
