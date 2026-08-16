package com.grgs.craftlevel.capability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Implementação mutável de Glory Kills / skills. */
public class PlayerGlory implements IPlayerGlory {

    public static final int SPELL_HOTBAR_SIZE = 9;

    private int gloryKills;
    private int skillPoints;
    private final Map<String, Integer> skillLevels = new LinkedHashMap<>();
    private final Map<String, Long> cooldownReadyAt = new LinkedHashMap<>();
    private final String[] spellHotbar = new String[SPELL_HOTBAR_SIZE];

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

    @Override
    public int getSkillLevel(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return 0;
        }
        return Math.max(0, skillLevels.getOrDefault(skillId, 0));
    }

    @Override
    public void setSkillLevel(String skillId, int level) {
        if (skillId == null || skillId.isEmpty()) {
            return;
        }
        int clamped = Math.max(0, level);
        if (clamped == 0) {
            skillLevels.remove(skillId);
        } else {
            skillLevels.put(skillId, clamped);
        }
    }

    @Override
    public Map<String, Integer> getSkillLevels() {
        return Collections.unmodifiableMap(skillLevels);
    }

    @Override
    public long getCooldownReadyAt(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return 0L;
        }
        return cooldownReadyAt.getOrDefault(skillId, 0L);
    }

    @Override
    public void setCooldownReadyAt(String skillId, long gameTime) {
        if (skillId == null || skillId.isEmpty()) {
            return;
        }
        if (gameTime <= 0L) {
            cooldownReadyAt.remove(skillId);
        } else {
            cooldownReadyAt.put(skillId, gameTime);
        }
    }

    @Override
    public Map<String, Long> getCooldownReadyAt() {
        return Collections.unmodifiableMap(cooldownReadyAt);
    }

    @Override
    public String[] getSpellHotbar() {
        String[] copy = new String[SPELL_HOTBAR_SIZE];
        System.arraycopy(spellHotbar, 0, copy, 0, SPELL_HOTBAR_SIZE);
        return copy;
    }

    @Override
    public void setSpellHotbarSlot(int slot, String skillId) {
        if (slot < 0 || slot >= SPELL_HOTBAR_SIZE) {
            return;
        }
        spellHotbar[slot] = skillId == null || skillId.isEmpty() ? null : skillId;
    }

    @Override
    public void resetSkills() {
        skillLevels.clear();
        cooldownReadyAt.clear();
        for (int i = 0; i < SPELL_HOTBAR_SIZE; i++) {
            spellHotbar[i] = null;
        }
    }

    @Override
    public void copyFrom(IPlayerGlory other) {
        setGloryKills(other.getGloryKills());
        setSkillPoints(other.getSkillPoints());
        skillLevels.clear();
        skillLevels.putAll(other.getSkillLevels());
        cooldownReadyAt.clear();
        cooldownReadyAt.putAll(other.getCooldownReadyAt());
        String[] hotbar = other.getSpellHotbar();
        for (int i = 0; i < SPELL_HOTBAR_SIZE; i++) {
            spellHotbar[i] = i < hotbar.length ? hotbar[i] : null;
        }
    }
}
