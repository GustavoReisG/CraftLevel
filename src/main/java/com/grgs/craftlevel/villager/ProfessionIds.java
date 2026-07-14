package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceLocation;

/** Normaliza IDs de profissão craftlevel:* (UI usa só o path). */
public final class ProfessionIds {

    private ProfessionIds() {}

    public static String pathFromInput(String input) {
        if (input == null || input.isBlank()) {
            return "new_profession";
        }
        String value = input.trim();
        int colon = value.indexOf(':');
        if (colon >= 0 && colon < value.length() - 1) {
            return value.substring(colon + 1);
        }
        return value;
    }

    public static String fullIdFromInput(String input) {
        return CraftLevel.MODID + ":" + pathFromInput(input);
    }

    public static String pathFromFullId(String fullId) {
        ResourceLocation id = ResourceLocation.tryParse(fullId);
        if (id == null || !CraftLevel.MODID.equals(id.getNamespace())) {
            return pathFromInput(fullId);
        }
        return id.getPath();
    }

    public static String textureForProfession(String fullId) {
        return pathFromFullId(fullId);
    }
}
