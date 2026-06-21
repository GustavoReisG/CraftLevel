package com.grgs.craftlevel.client;

import com.google.gson.*;
import com.grgs.craftlevel.CraftLevel;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import java.io.*;
import java.nio.file.*;

@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public class ClientConfig {

    public static int hudLpBarX = 0;
    public static int hudLpBarY = 0;
    public static float hudLpBarScale = 0.08f;

    public static int hudLevelCircleX = 0;
    public static int hudLevelCircleY = 0;
    public static float hudLevelCircleScale = 0.1f;
    public static float hudLevelCircleFontScale = 2.0f;
    public static String hudLpBarAnchor = "bottom_left";
    public static String hudLevelCircleAnchor = "top_left";

    private static final String DEFAULT = """
    {
        "hud": {
            "lp_bar": {
                "anchor": "bottom_left",
                "x": 2,
                "y": -5,
                "scale": 0.07
            },
            "level_circle": {
                "anchor": "top_left",
                "x": 2,
                "y": 2,
                "scale": 0.07,
                "font_scale": 2.5
            }
        }
    }
    """;

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        load();
    }

    public static void load() {
        try {
            Path file = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config/craftlevel/client.json");

            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT);
            }

            JsonObject obj = JsonParser.parseReader(
                    Files.newBufferedReader(file)
            ).getAsJsonObject();

            JsonObject hud = obj.getAsJsonObject("hud");
            JsonObject lpBar = hud.getAsJsonObject("lp_bar");
            hudLpBarX = lpBar.get("x").getAsInt();
            hudLpBarY = lpBar.get("y").getAsInt();
            hudLpBarScale = lpBar.get("scale").getAsFloat();
            hudLpBarAnchor = lpBar.get("anchor").getAsString();

            JsonObject levelCircle = hud.getAsJsonObject("level_circle");
            hudLevelCircleX = levelCircle.get("x").getAsInt();
            hudLevelCircleY = levelCircle.get("y").getAsInt();
            hudLevelCircleScale = levelCircle.get("scale").getAsFloat();
            hudLevelCircleFontScale = levelCircle.get("font_scale").getAsFloat();
            hudLevelCircleAnchor = levelCircle.get("anchor").getAsString();

        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] client.json error, using defaults");
            hudLpBarX = 2;
            hudLpBarY = -5;
            hudLpBarScale = 0.07f;
            hudLpBarAnchor = "bottom_left";

            hudLevelCircleX = 2;
            hudLevelCircleY = 2;
            hudLevelCircleScale = 0.07f;
            hudLevelCircleFontScale = 2.5f;
            hudLevelCircleAnchor = "top_left";
        }
    }
}