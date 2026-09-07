package com.grgs.craftlevel.potion;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Vanilla / packs usam {@code potion.level.N} ou {@code potion.potency.N}.
 * Acima do nível 5 trocamos a chave crua por número arábico.
 */
public final class PotionLevelText {

    public static final int ROMAN_MAX = 5;
    private static final String[] LEVEL_PREFIXES = {
            "potion.level.",
            "potion.potency."
    };

    private PotionLevelText() {}

    /** Se a chave for nível &gt; {@link #ROMAN_MAX}, devolve o número; senão traduz normalmente. */
    public static MutableComponent remapTranslatable(String key) {
        Integer level = parseLevelKey(key);
        if (level != null && level > ROMAN_MAX) {
            return Component.literal(String.valueOf(level));
        }
        return Component.translatable(key);
    }

    public static Integer parseLevelKey(String key) {
        if (key == null) {
            return null;
        }
        for (String prefix : LEVEL_PREFIXES) {
            if (key.startsWith(prefix)) {
                try {
                    return Integer.parseInt(key.substring(prefix.length()));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Reescreve árvores de Component (tooltip) trocando chaves de nível altas por número. */
    public static Component remapDeep(Component component) {
        if (component == null) {
            return Component.empty();
        }

        if (component.getContents() instanceof TranslatableContents contents) {
            Integer level = parseLevelKey(contents.getKey());
            if (level != null && level > ROMAN_MAX) {
                MutableComponent literal = Component.literal(String.valueOf(level)).setStyle(component.getStyle());
                for (Component sibling : component.getSiblings()) {
                    literal.append(remapDeep(sibling));
                }
                return literal;
            }

            Object[] args = contents.getArgs();
            Object[] newArgs = new Object[args.length];
            boolean changed = false;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component nested) {
                    Component remapped = remapDeep(nested);
                    newArgs[i] = remapped;
                    if (remapped != nested) {
                        changed = true;
                    }
                } else {
                    newArgs[i] = args[i];
                }
            }

            if (changed) {
                MutableComponent rebuilt = Component.translatable(contents.getKey(), newArgs).setStyle(component.getStyle());
                for (Component sibling : component.getSiblings()) {
                    rebuilt.append(remapDeep(sibling));
                }
                return rebuilt;
            }
        }

        if (!component.getSiblings().isEmpty()) {
            MutableComponent copy = component.copy();
            copy.getSiblings().clear();
            for (Component sibling : component.getSiblings()) {
                copy.append(remapDeep(sibling));
            }
            return copy;
        }

        return component;
    }
}
