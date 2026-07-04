package com.grgs.craftlevel.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.client.ClientConfig;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega e sobrescreve client.json a partir da sessão de edição. */
final class ClientConfigPersistence {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ClientConfigEditorState session;

    private ClientConfigPersistence() {}

    static ClientConfigEditorState session() {
        if (session == null) {
            session = ClientConfigEditorState.createDefault();
        }
        return session;
    }

    static void beginEditing(Minecraft minecraft) {
        session = ClientConfigEditorState.loadFrom(configFile(minecraft));
    }

    static void save(Minecraft minecraft) {
        try {
            Path file = configFile(minecraft);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(session().toJson()));
            ClientConfig.applyEditorState(session());
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save client.json", e);
        }
    }

    private static Path configFile(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel/client.json");
    }
}
