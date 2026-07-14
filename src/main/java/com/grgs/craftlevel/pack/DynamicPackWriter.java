package com.grgs.craftlevel.pack;

import com.grgs.craftlevel.CraftLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Utilitário para recriar packs dinâmicos do zero a cada boot. */
public final class DynamicPackWriter {

    private DynamicPackWriter() {}

    public static Path rebuildRoot(Path root) throws IOException {
        deleteIfExists(root);
        Files.createDirectories(root);
        return root;
    }

    public static void deleteIfExists(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try {
            try (Stream<Path> paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        CraftLevel.LOGGER.warn("[CraftLevel] Failed to delete {}", path, e);
                    }
                });
            }
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to clear pack folder {}", root, e);
        }
    }

    public static void writePackMeta(Path packRoot, String description) throws IOException {
        Files.writeString(packRoot.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "description": "%s",
                    "pack_format": 15
                  }
                }
                """.formatted(description));
    }

    public static void deleteLegacyFolders(Path parent, String... legacyNames) {
        for (String legacyName : legacyNames) {
            deleteIfExists(parent.resolve(legacyName));
        }
    }
}
