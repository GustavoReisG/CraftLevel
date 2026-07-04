package com.grgs.craftlevel.client.config;

/** Labels exibidos nos botões das telas de config. */
final class ConfigScreenLabels {

    private ConfigScreenLabels() {}

    static String fileButtonLabel(String fileName) {
        return switch (fileName) {
            case "client" -> "Client";
            case "mob_levels" -> "Mob Levels";
            default -> titleCase(fileName.replace('_', ' '));
        };
    }

    private static String titleCase(String text) {
        if (text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
