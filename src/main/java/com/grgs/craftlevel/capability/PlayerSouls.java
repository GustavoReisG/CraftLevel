package com.grgs.craftlevel.capability;

/** Implementação mutável de souls do player. */
public class PlayerSouls implements IPlayerSouls {

    private int souls;
    private int restoreProgressTicks;
    private boolean initialized;

    @Override
    public int getSouls() {
        return souls;
    }

    @Override
    public void setSouls(int souls) {
        this.souls = Math.max(0, souls);
    }

    @Override
    public int getRestoreProgressTicks() {
        return restoreProgressTicks;
    }

    @Override
    public void setRestoreProgressTicks(int ticks) {
        this.restoreProgressTicks = Math.max(0, ticks);
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
