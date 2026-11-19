package com.grgs.craftlevel.item;

import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;
import java.util.Locale;

/** Tipos de runa aplicáveis em armas/armaduras via smithing. */
public enum RuneKind implements StringRepresentable {
    UNBREAKABLE("unbreakable"),
    VELOCE("veloce"),
    REACH("reach"),
    FORTUNE("fortune"),
    RETURN("return"),
    SOULBOUND("soulbound");

    private final String id;

    RuneKind(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public String translationKey() {
        return "item.craftlevel.rune_of_" + id;
    }

    public String tooltipKey() {
        return translationKey() + ".tooltip";
    }

    @Nullable
    public static RuneKind byId(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String key = id.toLowerCase(Locale.ROOT);
        for (RuneKind kind : values()) {
            if (kind.id.equals(key)) {
                return kind;
            }
        }
        return null;
    }
}
