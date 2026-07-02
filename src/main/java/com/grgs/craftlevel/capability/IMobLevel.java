package com.grgs.craftlevel.capability;

/** Contrato do level de um mob (sem LP). Persistido em NBT enquanto o mob existir. */
public interface IMobLevel {
    int getLevel();
    void setLevel(int level);
}
