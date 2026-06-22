package com.grgs.craftlevel.config;

import com.google.gson.*;
import com.grgs.craftlevel.CraftLevel;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.*;
import java.nio.file.*;

@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
/** Carrega e cria os JSONs em config/craftlevel/ no servidor e no cliente. */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configDir;

    // =========================================================
    // DEFAULTS
    // =========================================================

    private static final String DEFAULT_SYSTEM = """
    {
        "enableSystem": true,
        "debug": false,
        "mob_level_variation": 10
    }
    """;

    private static final String DEFAULT_LP = """
    {
        "lpBase": 100,
        "lpMultiplier": {
            "T1": 1.05,
            "T2": 1.025,
            "T3": 1.0125,
            "T4": 1.0125
        },
        "playerBaseStats": {
           "minecraft:generic.max_health": 20.0,
           "minecraft:generic.attack_damage": 1.0,
           "minecraft:generic.attack_speed": 4.0,
           "minecraft:generic.movement_speed": 0.1,
           "minecraft:generic.armor_toughness": 1.0
        }
    }
    """;

    private static final String DEFAULT_PLAYER_STATS = """
    {
        "minecraft:generic.max_health": {
            "operation": "addition",
            "T1": 1.05,
            "T2": 1.025,
            "T3": 1.0125,
            "T4": 1.0125
        },
        "minecraft:generic.attack_damage": {
            "operation": "addition",
            "T1": 1.05,
            "T2": 1.025,
            "T3": 1.0125,
            "T4": 1.0125
        },
        "minecraft:generic.armor_toughness": {
            "operation": "addition",
            "T1": 1.05,
            "T2": 1.025,
            "T3": 1.0125,
            "T4": 1.0125
        },
        "minecraft:generic.knockback_resistance": {
            "operation": "addition",
            "T1": 1.02,
            "T2": 1.01,
            "T3": 1.01,
            "T4": 1.01
        }
    }
    """;

    private static final String DEFAULT_MOB_STATS = """
    {
        "minecraft:generic.max_health": {
            "operation": "addition",
            "T1": 1.05,
            "T2": 1.025,
            "T3": 1.0125,
            "T4": 1.0125
        },
        "minecraft:generic.attack_damage": {
            "operation": "addition",
            "T1": 1.04,
            "T2": 1.025,
            "T3": 1.0125,
            "T4": 1.0125
        },
        "minecraft:generic.armor_toughness": {
            "operation": "addition",
            "T1": 1.05,
            "T2": 1.025,
            "T3": 1.0125,
            "T4": 1.0125
        },
        "minecraft:generic.knockback_resistance": {
            "operation": "addition",
            "T1": 1.02,
            "T2": 1.01,
            "T3": 1.01,
            "T4": 1.01
        }
    }
    """;

    private static final String DEFAULT_MOB_OVERRIDES = """
    {
        "minecraft:cow": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:sheep": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:pig": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:chicken": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:rabbit": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:squid": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:bat": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:glow_squid": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:cod": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:salmon": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:tropical_fish": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        },
        "minecraft:pufferfish": {
            "minecraft:generic.max_health": { "operation": "addition", "T1": 1.0, "T2": 1.0, "T3": 1.0, "T4": 1.0 }
        }
    }
    """;

    private static final String DEFAULT_MOB_LEVELS = """
    {
        "minecraft:wither": { "lvl_min": 50, "lvl_max": 200 },
        "minecraft:warden": { "lvl_min": 50, "lvl_max": 200 },
        "minecraft:elder_guardian": { "lvl_min": 50, "lvl_max": 200 },
        "minecraft:ender_dragon": { "lvl_min": 100, "lvl_max": 200 },
        "minecraft:cow": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:sheep": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:pig": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:chicken": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:rabbit": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:squid": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:bat": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:glow_squid": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:cod": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:salmon": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:tropical_fish": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:pufferfish": { "lvl_min": 1, "lvl_max": 1 }
    }
    """;

    private static final String DEFAULT_ENCHANTMENTS = """
    {
        "minecraft:sharpness": {
            "entity_type": null,
            "base_damage": 0.5,
            "level_damage": 0.5,
            "attribute_scaling": "minecraft:generic.attack_damage"
        },
        "minecraft:smite": {
            "entity_type": "undead",
            "base_damage": 0,
            "level_damage": 2.5,
            "attribute_scaling": "minecraft:generic.attack_damage"
        },
        "minecraft:bane_of_arthropods": {
            "entity_type": "arthropod",
            "base_damage": 0,
            "level_damage": 2.5,
            "attribute_scaling": "minecraft:generic.attack_damage"
        }
    }
    """;

    // =========================================================
    // VALORES CARREGADOS
    // =========================================================

    public static JsonObject systemConfig = new JsonObject();
    public static JsonObject lpConfig = new JsonObject();
    public static JsonObject playerStats = new JsonObject();
    public static JsonObject mobStats = new JsonObject();
    public static JsonObject mobOverrides = new JsonObject();
    public static JsonObject mobLevels = new JsonObject();
    public static JsonObject enchantmentsConfig = new JsonObject();

    // =========================================================
    // INIT
    // =========================================================

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        try {
            loadAllConfigs(event.getServer().getServerDirectory().toPath().resolve("config/craftlevel"));
        } catch (IOException e) {
            CraftLevel.LOGGER.error("[CraftLevel] Failed to load config!", e);
        }
    }

    /** Carrega todos os JSONs de um diretório e popula o ConfigParser. */
    public static void loadAllConfigs(Path dir) throws IOException {
        configDir = dir;
        Files.createDirectories(configDir);

        loadSystem();
        loadLp();
        loadPlayerStats();
        loadMobStats();
        loadMobOverrides();
        loadMobLevels();
        loadEnchantments();

        ConfigParser.parseSystem(systemConfig);
        ConfigParser.parseLp(lpConfig);
        ConfigParser.parsePlayerStats(playerStats);
        ConfigParser.parseMobStats(mobStats);
        ConfigParser.parseMobOverrides(mobOverrides);
        ConfigParser.parseMobLevels(mobLevels);
        ConfigParser.parseEnchantments(enchantmentsConfig);

        if (ConfigParser.debug) {
            CraftLevel.LOGGER.info("[CraftLevel] Config loaded from {}", configDir);
        }
    }

    // =========================================================
    // LOADERS
    // =========================================================

    private static void loadSystem() throws IOException {
        systemConfig = loadOrCreate("system.json", DEFAULT_SYSTEM);
    }

    private static void loadLp() throws IOException {
        lpConfig = loadOrCreate("lp.json", DEFAULT_LP);
    }

    private static void loadPlayerStats() throws IOException {
        playerStats = loadOrCreate("player_stats.json", DEFAULT_PLAYER_STATS);
    }

    private static void loadMobStats() throws IOException {
        mobStats = loadOrCreate("mob_stats.json", DEFAULT_MOB_STATS);
    }

    private static void loadMobOverrides() throws IOException {
        mobOverrides = loadOrCreate("mob_overrides.json", DEFAULT_MOB_OVERRIDES);
    }

    private static void loadMobLevels() throws IOException {
        mobLevels = loadOrCreate("mob_levels.json", DEFAULT_MOB_LEVELS);
    }

    private static void loadEnchantments() throws IOException {
        enchantmentsConfig = loadOrCreate("enchantments.json", DEFAULT_ENCHANTMENTS);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /** Cria o arquivo com defaults se não existir, depois lê o JSON. */
    private static JsonObject loadOrCreate(String fileName, String defaultContent) throws IOException {
        Path file = configDir.resolve(fileName);
        if (!Files.exists(file)) {
            Files.writeString(file, defaultContent);
        }
        return readJson(file);
    }

    private static JsonObject readJson(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}