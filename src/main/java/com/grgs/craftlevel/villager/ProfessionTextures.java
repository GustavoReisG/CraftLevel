package com.grgs.craftlevel.villager;

/** Normalização de nomes de textura de profissão (sempre PNG no resource pack). */
public final class ProfessionTextures {

    private static final String DEFAULT_NAME = "texture1";

    private ProfessionTextures() {}

    public static String normalizeName(String texture) {
        if (texture == null || texture.isBlank()) {
            return DEFAULT_NAME;
        }
        String name = texture.trim();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.isBlank() ? DEFAULT_NAME : name;
    }

    public static String fileName(String texture) {
        return normalizeName(texture) + ".png";
    }
}
