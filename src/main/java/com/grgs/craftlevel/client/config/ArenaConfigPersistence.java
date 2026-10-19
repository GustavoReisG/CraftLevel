package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.arena.ArenaSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Sessão de edição de {@code config/craftlevel/arena.json}. */
final class ArenaConfigPersistence {

    private static ArenaSettings session;

    private ArenaConfigPersistence() {}

    static ArenaSettings session() {
        if (session == null) {
            session = new ArenaSettings();
        }
        return session;
    }

    static void beginEditing(Minecraft minecraft) {
        session = ArenaSettings.load(configFile(minecraft), discover(minecraft));
    }

    static void ensureSession(Minecraft minecraft) {
        if (session == null) {
            beginEditing(minecraft);
        }
    }

    static void save(Minecraft minecraft) {
        try {
            session().write(configFile(minecraft));
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save arena.json", e);
        }
    }

    static List<String> discover(Minecraft minecraft) {
        List<String> files = new ArrayList<>();
        try {
            minecraft.getResourceManager().listResources("arena", location -> location.getPath().endsWith(".schem"))
                    .keySet()
                    .stream()
                    .map(ArenaConfigPersistence::fileName)
                    .filter(name -> !name.isBlank())
                    .distinct()
                    .sorted()
                    .forEach(files::add);
        } catch (Exception ignored) {
            files.clear();
        }
        return files;
    }

    private static String fileName(ResourceLocation location) {
        String path = location.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static Path configFile(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel/arena.json");
    }
}
