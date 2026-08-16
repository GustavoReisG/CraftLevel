package com.grgs.craftlevel.capability;

import java.util.Map;

/** Glory Kills, Skill Points, níveis de skill, cooldowns e hotbar de feitiços. */
public interface IPlayerGlory {

    int getGloryKills();

    void setGloryKills(int gloryKills);

    int getSkillPoints();

    void setSkillPoints(int skillPoints);

    int getSkillLevel(String skillId);

    void setSkillLevel(String skillId, int level);

    Map<String, Integer> getSkillLevels();

    long getCooldownReadyAt(String skillId);

    void setCooldownReadyAt(String skillId, long gameTime);

    Map<String, Long> getCooldownReadyAt();

    String[] getSpellHotbar();

    void setSpellHotbarSlot(int slot, String skillId);

    void resetSkills();

    void copyFrom(IPlayerGlory other);
}
