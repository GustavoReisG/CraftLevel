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
    public static int hudLpBarX = 2;
    public static int hudLpBarY = -2;
    public static float hudLpBarScale = 0.11f;
    public static float hudLpBarFontScale = 0.6f;

    // Posição e escala do círculo de level
    public static int hudLevelCircleX = 2;
    public static int hudLevelCircleY = 2;
    public static float hudLevelCircleScale = 0.15f;
    public static float hudLevelCircleFontScale = 1.0f;
    public static float hudLevelCircleFontYOffset = -2.0f;
    public static String hudLpBarAnchor = "bottom_left";
    public static String hudLevelCircleAnchor = "top_left";

    // Barra de HP customizada
    public static boolean hudHealthBarEnabled = true;
    public static int hudHealthBarX = -62;
    public static int hudHealthBarY = -31;
    public static float hudHealthBarScale = 0.1f;
    public static float hudHealthBarFontScale = 0.6f;
    public static String hudHealthBarAnchor = "bottom_center";
    public static int hudHealthBarArmorMargin = 10;

    // Barra de fome customizada
    public static boolean hudHungerBarEnabled = true;
    public static int hudHungerBarX = 60;
    public static int hudHungerBarY = -31;
    public static float hudHungerBarScale = 0.1f;
    public static float hudHungerBarFontScale = 0.6f;
    public static String hudHungerBarAnchor = "bottom_center";
    public static int hudHungerBarBreathMargin = 10;
    public static int hudHungerBarMountHealthMargin = 0;

    // HUD de esmeraldas da wallet
    public static boolean hudEmeraldsEnabled = true;
    public static int hudEmeraldsX = 2;
    public static int hudEmeraldsY = -20;
    public static float hudEmeraldsScale = 0.7f;
    public static float hudEmeraldsFontScale = 1.0f;
    public static String hudEmeraldsAnchor = "bottom_left";
    public static String hudEmeraldsIconSide = "left";

    // HUD de souls
    public static boolean hudSoulsEnabled = true;
    public static int hudSoulsX = 2;
    public static int hudSoulsY = 50;
    public static float hudSoulsScale = 0.7f;
    public static float hudSoulsFontScale = 1.0f;
    public static String hudSoulsAnchor = "top_left";
    public static String hudSoulsIconSide = "left";

    // HUD de Glory Kills
    public static boolean hudGloryKillsEnabled = true;
    public static int hudGloryKillsX = 2;
    public static int hudGloryKillsY = 70;
    public static float hudGloryKillsScale = 0.7f;
    public static float hudGloryKillsFontScale = 1.0f;
    public static String hudGloryKillsAnchor = "top_left";
    public static String hudGloryKillsIconSide = "left";

    // Card de Mob Focus (mirar + E)
    public static String hudMobFocusAnchor = "top_left";
    public static int hudMobFocusX = 50;
    public static int hudMobFocusY = 8;
    public static float hudMobFocusScale = 0.7f;
    public static float hudMobFocusFontScale = 0.6f;
    public static String hudMobFocusDirection = "portrait_first";
    public static float hudMobFocusMobYaw = 0.6f;
    public static float hudMobFocusHeadYaw = 0.0f;
    public static float hudMobFocusCameraPitch = -0.2f;

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
        CraftLevelTexturesPack.ensurePackSelected(Minecraft.getInstance());
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
        hudLpBarFontScale = lpBar.fontScale();

        ClientConfigEditorState.HudElementValues levelCircle = state.resolvedLevelCircle();
        hudLevelCircleAnchor = levelCircle.anchor();
        hudLevelCircleX = levelCircle.x();
        hudLevelCircleY = levelCircle.y();
        hudLevelCircleScale = levelCircle.scale();
        hudLevelCircleFontScale = levelCircle.fontScale();
        hudLevelCircleFontYOffset = levelCircle.fontYOffset();

        ClientConfigEditorState.HealthBarValues healthBar = state.resolvedHealthBar();
        ClientConfigEditorState.HungerBarValues hungerBar = state.resolvedHungerBar();
        hudHealthBarEnabled = healthBar.enabled();
        hudHealthBarAnchor = healthBar.anchor();
        hudHealthBarX = healthBar.x();
        hudHealthBarY = healthBar.y();
        hudHealthBarScale = healthBar.scale();
        hudHealthBarFontScale = healthBar.fontScale();
        hudHealthBarArmorMargin = healthBar.armorMargin();

        hudHungerBarEnabled = hungerBar.enabled();
        hudHungerBarAnchor = hungerBar.anchor();
        hudHungerBarX = hungerBar.x();
        hudHungerBarY = hungerBar.y();
        hudHungerBarScale = hungerBar.scale();
        hudHungerBarFontScale = hungerBar.fontScale();
        hudHungerBarBreathMargin = hungerBar.breathMargin();
        hudHungerBarMountHealthMargin = hungerBar.mountHealthMargin() != null
                ? hungerBar.mountHealthMargin()
                : hungerBar.breathMargin();

        ClientConfigEditorState.EmeraldHudValues emeralds = state.resolvedEmeralds();
        hudEmeraldsEnabled = emeralds.enabled();
        hudEmeraldsAnchor = emeralds.anchor();
        hudEmeraldsX = emeralds.x();
        hudEmeraldsY = emeralds.y();
        hudEmeraldsScale = emeralds.scale();
        hudEmeraldsFontScale = emeralds.fontScale();
        hudEmeraldsIconSide = emeralds.iconSide();

        ClientConfigEditorState.SoulsHudValues souls = state.resolvedSouls();
        hudSoulsEnabled = souls.enabled();
        hudSoulsAnchor = souls.anchor();
        hudSoulsX = souls.x();
        hudSoulsY = souls.y();
        hudSoulsScale = souls.scale();
        hudSoulsFontScale = souls.fontScale();
        hudSoulsIconSide = souls.iconSide();

        ClientConfigEditorState.GloryKillsHudValues gloryKills = state.resolvedGloryKills();
        hudGloryKillsEnabled = gloryKills.enabled();
        hudGloryKillsAnchor = gloryKills.anchor();
        hudGloryKillsX = gloryKills.x();
        hudGloryKillsY = gloryKills.y();
        hudGloryKillsScale = gloryKills.scale();
        hudGloryKillsFontScale = gloryKills.fontScale();
        hudGloryKillsIconSide = gloryKills.iconSide();

        ClientConfigEditorState.MobFocusValues mobFocus = state.resolvedMobFocus();
        hudMobFocusAnchor = mobFocus.anchor();
        hudMobFocusX = mobFocus.x();
        hudMobFocusY = mobFocus.y();
        hudMobFocusScale = mobFocus.scale();
        hudMobFocusFontScale = mobFocus.fontScale();
        hudMobFocusDirection = mobFocus.direction();
        hudMobFocusMobYaw = mobFocus.mobYaw();
        hudMobFocusHeadYaw = mobFocus.headYaw();
        hudMobFocusCameraPitch = mobFocus.cameraPitch();
    }

    /** Grava os valores atuais em config/craftlevel/client.json. */
    public static void save() {
        ClientConfigEditorState state = ClientConfigEditorState.createDefault();
        state.setLpBar(new ClientConfigEditorState.HudElementValues(
                hudLpBarAnchor, hudLpBarX, hudLpBarY, hudLpBarScale, hudLpBarFontScale, null
        ));
        state.setLevelCircle(new ClientConfigEditorState.HudElementValues(
                hudLevelCircleAnchor, hudLevelCircleX, hudLevelCircleY,
                hudLevelCircleScale, hudLevelCircleFontScale, hudLevelCircleFontYOffset
        ));
        state.setHealthBar(new ClientConfigEditorState.HealthBarValues(
                hudHealthBarEnabled, hudHealthBarAnchor, hudHealthBarX, hudHealthBarY,
                hudHealthBarScale, hudHealthBarFontScale, hudHealthBarArmorMargin
        ));
        state.setHungerBar(new ClientConfigEditorState.HungerBarValues(
                hudHungerBarEnabled, hudHungerBarAnchor, hudHungerBarX, hudHungerBarY,
                hudHungerBarScale, hudHungerBarFontScale, hudHungerBarBreathMargin,
                hudHungerBarMountHealthMargin
        ));
        state.setEmeralds(new ClientConfigEditorState.EmeraldHudValues(
                hudEmeraldsEnabled, hudEmeraldsAnchor, hudEmeraldsX, hudEmeraldsY,
                hudEmeraldsScale, hudEmeraldsFontScale, hudEmeraldsIconSide
        ));
        state.setSouls(new ClientConfigEditorState.SoulsHudValues(
                hudSoulsEnabled, hudSoulsAnchor, hudSoulsX, hudSoulsY,
                hudSoulsScale, hudSoulsFontScale, hudSoulsIconSide
        ));
        state.setGloryKills(new ClientConfigEditorState.GloryKillsHudValues(
                hudGloryKillsEnabled, hudGloryKillsAnchor, hudGloryKillsX, hudGloryKillsY,
                hudGloryKillsScale, hudGloryKillsFontScale, hudGloryKillsIconSide
        ));
        state.setMobFocus(new ClientConfigEditorState.MobFocusValues(
                hudMobFocusAnchor, hudMobFocusX, hudMobFocusY,
                hudMobFocusScale, hudMobFocusFontScale, hudMobFocusDirection,
                hudMobFocusMobYaw, hudMobFocusHeadYaw, hudMobFocusCameraPitch
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
