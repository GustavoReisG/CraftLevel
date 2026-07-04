package com.grgs.craftlevel.client.config;

import net.minecraft.client.Minecraft;

/** Contexto de edição compartilhado entre telas de wiring. */
final class StatWiringEditor {

    private final StatWiringEditorState wiringState;
    private final Runnable saveAction;

    StatWiringEditor(StatWiringEditorState wiringState, Runnable saveAction) {
        this.wiringState = wiringState;
        this.saveAction = saveAction;
    }

    static StatWiringEditor forFile(String fileName, Minecraft minecraft) {
        StatWiringPersistence.beginEditing(minecraft, fileName);
        return new StatWiringEditor(
                StatWiringPersistence.session(),
                () -> StatWiringPersistence.save(minecraft)
        );
    }

    static StatWiringEditor forMobOverride(String mobId, Minecraft minecraft) {
        return new StatWiringEditor(
                MobOverridesPersistence.session().mob(mobId),
                () -> MobOverridesPersistence.save(minecraft)
        );
    }

    StatWiringEditorState state() {
        return wiringState;
    }

    void save() {
        saveAction.run();
    }
}
