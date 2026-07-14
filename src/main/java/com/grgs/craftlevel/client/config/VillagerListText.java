package com.grgs.craftlevel.client.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Utilitários de serialização de listas na UI de villager. */
final class VillagerListText {

    private VillagerListText() {}

    static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(", ", values);
    }

    static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(result::add);
        return result;
    }
}
