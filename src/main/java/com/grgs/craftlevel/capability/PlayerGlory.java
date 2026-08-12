package com.grgs.craftlevel.capability;

/** Implementação mutável de Glory Kills / Skill Points. */
public class PlayerGlory implements IPlayerGlory {

    private int gloryKills;
    private int skillPoints;

    @Override
    public int getGloryKills() {
        return gloryKills;
    }

    @Override
    public void setGloryKills(int gloryKills) {
        this.gloryKills = Math.max(0, gloryKills);
    }

    @Override
    public int getSkillPoints() {
        return skillPoints;
    }

    @Override
    public void setSkillPoints(int skillPoints) {
        this.skillPoints = Math.max(0, skillPoints);
    }
}
