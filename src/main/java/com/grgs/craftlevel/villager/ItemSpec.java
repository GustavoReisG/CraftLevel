package com.grgs.craftlevel.villager;

/**
 * Parseia specs de item no formato {@code namespace:path} ou {@code namespace:path{snbt}},
 * também usado em whitelist/blacklist.
 */
public final class ItemSpec {

    public record Parsed(String id, String nbt) {
        public boolean hasNbt() {
            return nbt != null && !nbt.isBlank();
        }
    }

    private ItemSpec() {}

    public static Parsed parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Parsed("", "");
        }
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        if (start < 0) {
            return new Parsed(trimmed, "");
        }

        int depth = 0;
        for (int i = start; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return new Parsed(
                            trimmed.substring(0, start).trim(),
                            trimmed.substring(start, i + 1)
                    );
                }
            }
        }
        return new Parsed(trimmed, "");
    }

    public static String idOnly(String raw) {
        return parse(raw).id();
    }
}
