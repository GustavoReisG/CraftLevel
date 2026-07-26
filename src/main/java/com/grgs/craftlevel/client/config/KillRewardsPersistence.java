package com.grgs.craftlevel.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigManager;
import com.grgs.craftlevel.economy.EconomyService;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Carrega/salva a seção kill_rewards em economy.json sem apagar o resto. */
final class KillRewardsPersistence {

    private static KillRewardsEditorState session;

    private KillRewardsPersistence() {}

    static KillRewardsEditorState session() {
        if (session == null) {
            session = new KillRewardsEditorState();
        }
        return session;
    }

    /** Recarrega do disco (abrir Kill Rewards pelo menu). */
    static void beginEditing(Minecraft minecraft) {
        session = KillRewardsEditorState.loadFrom(configFile(minecraft));
    }

    /** Mantém a sessão em memória (voltar da blacklist sem perder edições). */
    static void ensureSession(Minecraft minecraft) {
        if (session == null) {
            beginEditing(minecraft);
        }
    }

    static void save(Minecraft minecraft) {
        try {
            Path dir = configDir(minecraft);
            Files.createDirectories(dir);
            Path file = configFile(minecraft);

            JsonObject root = loadRoot(file);
            root.add("kill_rewards", session().toKillRewardsJson());
            Files.writeString(file, ConfigManager.toPrettyJson(root));

            ConfigManager.loadAllConfigs(dir);
            MinecraftServer server = minecraft.getSingleplayerServer();
            if (server != null) {
                EconomyService.onEconomyConfigReload(server);
            }
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save kill_rewards in economy.json", e);
        }
    }

    private static JsonObject loadRoot(Path file) throws IOException {
        if (!Files.exists(file)) {
            return JsonParser.parseString(ConfigManager.DEFAULT_ECONOMY).getAsJsonObject();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return JsonParser.parseString(ConfigManager.DEFAULT_ECONOMY).getAsJsonObject();
        }
    }

    private static Path configDir(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config/craftlevel");
    }

    private static Path configFile(Minecraft minecraft) {
        return configDir(minecraft).resolve("economy.json");
    }
}
