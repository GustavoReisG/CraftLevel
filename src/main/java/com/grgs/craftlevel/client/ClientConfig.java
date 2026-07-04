package com.grgs.craftlevel.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.ConfigManager;
import com.grgs.craftlevel.client.config.ClientConfigEditorState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import java.io.*;
import java.nio.file.*;

/** Configurações visuais do cliente (client.json) + reload dos JSONs de gameplay no login. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public class ClientConfig {

    // Posição e escala da barra de LP
    public static int hudLpBarX = 0;
    public static int hudLpBarY = 0;
    public static float hudLpBarScale = 0.08f;

    // Posição e escala do círculo de level
    public static int hudLevelCircleX = 0;
    public static int hudLevelCircleY = 0;
    public static float hudLevelCircleScale = 0.1f;
    public static float hudLevelCircleFontScale = 2.0f;
    public static String hudLpBarAnchor = "bottom_left";
    public static String hudLevelCircleAnchor = "top_left";

    // Barra de HP customizada
    public static boolean hudHealthBarEnabled = true;
    public static int hudHealthBarX = -50;
    public static int hudHealthBarY = -48;
    public static float hudHealthBarScale = 0.08f;
    public static float hudHealthBarFontScale = 0.65f;
    public static String hudHealthBarAnchor = "bottom_center";

    public static final String[] ANCHORS = {
            "top_left",
            "top_center",
            "top_right",
            "center_left",
            "center_right",
            "bottom_left",
            "bottom_center",
            "bottom_right"
    };

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Ao entrar no mundo: carrega client.json e os JSONs de gameplay para tooltips funcionarem. */
    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        load();
        loadGameConfig();
    }

    /** Carrega configs de gameplay no cliente (necessário para scaling local). */
    public static void loadGameConfig() {
        try {
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("config/craftlevel");
            ConfigManager.loadAllConfigs(dir);
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to load game config on client", e);
        }
    }

    private static Path getConfigPath() {
        return getConfigDirectory().resolve("client.json");
    }

    private static Path getConfigDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/craftlevel");
    }

    /** Lê config/craftlevel/client.json — cria defaults na primeira execução. */
    public static void load() {
        try {
            Path dir = getConfigDirectory();
            ConfigManager.writeDefaultClient(dir);
            applyEditorState(ClientConfigEditorState.loadFrom(dir.resolve("client.json")));
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] client.json error, using runtime defaults");
            applyEditorState(ClientConfigEditorState.createDefault());
        }
    }

    /** Aplica valores da sessão de edição ao runtime do HUD. */
    public static void applyEditorState(ClientConfigEditorState state) {
        ClientConfigEditorState.HudElementValues lpBar = state.resolvedLpBar();
        hudLpBarAnchor = lpBar.anchor();
        hudLpBarX = lpBar.x();
        hudLpBarY = lpBar.y();
        hudLpBarScale = lpBar.scale();

        ClientConfigEditorState.HudElementValues levelCircle = state.resolvedLevelCircle();
        hudLevelCircleAnchor = levelCircle.anchor();
        hudLevelCircleX = levelCircle.x();
        hudLevelCircleY = levelCircle.y();
        hudLevelCircleScale = levelCircle.scale();
        hudLevelCircleFontScale = levelCircle.fontScale();

        ClientConfigEditorState.HealthBarValues healthBar = state.resolvedHealthBar();
        hudHealthBarEnabled = healthBar.enabled();
        hudHealthBarAnchor = healthBar.anchor();
        hudHealthBarX = healthBar.x();
        hudHealthBarY = healthBar.y();
        hudHealthBarScale = healthBar.scale();
        hudHealthBarFontScale = healthBar.fontScale();
    }

    /** Grava os valores atuais em config/craftlevel/client.json. */
    public static void save() {
        ClientConfigEditorState state = ClientConfigEditorState.createDefault();
        state.setLpBar(new ClientConfigEditorState.HudElementValues(
                hudLpBarAnchor, hudLpBarX, hudLpBarY, hudLpBarScale, null
        ));
        state.setLevelCircle(new ClientConfigEditorState.HudElementValues(
                hudLevelCircleAnchor, hudLevelCircleX, hudLevelCircleY,
                hudLevelCircleScale, hudLevelCircleFontScale
        ));
        state.setHealthBar(new ClientConfigEditorState.HealthBarValues(
                hudHealthBarEnabled, hudHealthBarAnchor, hudHealthBarX, hudHealthBarY,
                hudHealthBarScale, hudHealthBarFontScale
        ));
        try {
            Path file = getConfigPath();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(state.toJson()));
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to save client.json", e);
        }
    }
}
