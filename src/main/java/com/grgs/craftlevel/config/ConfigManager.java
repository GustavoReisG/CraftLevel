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
        "mob_level_variation": 10,
        "debug": true
    }
    """;

    private static final String DEFAULT_LP = """
    {
        "lpBase": 100,
        "lpMultiplier": 1.06
    }
    """;

    private static final String DEFAULT_PLAYER_STATS = """
    {
        "attribute": {
            "minecraft:generic.max_health": 1.045,
            "irons_spellbooks:spell_power": 1.045
        },
        "hurt": {
            "minecraft:player_attack": "minecraft:generic.max_health",
            "minecraft:mob_attack": "minecraft:generic.max_health",
            "minecraft:arrow": "minecraft:generic.max_health",
            "minecraft:trident": "minecraft:generic.max_health",
            "minecraft:magic": "minecraft:generic.max_health",
            "minecraft:indirect_magic": "minecraft:generic.max_health",
            "minecraft:wither": "minecraft:generic.max_health",
            "minecraft:poison": "minecraft:generic.max_health",
            "attributeslib:current_hp_damage": 1.0,
            "*irons_spellbooks*": 1.0,
            "default": "minecraft:generic.max_health"
        },
        "heal": {
            "default": "minecraft:generic.max_health"
        }
    }
    """;

    private static final String DEFAULT_MOB_STATS = """
    {
        "attribute": {
            "minecraft:generic.max_health": 1.05
        },
        "hurt": {
            "default": "minecraft:generic.max_health"
        },
        "heal": {
            "default": "minecraft:generic.max_health"
        }
    }
    """;

    private static final String DEFAULT_MOB_OVERRIDES = """
    {
        "minecraft:cow": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:sheep": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:pig": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:chicken": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:rabbit": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:squid": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:bat": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:glow_squid": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:cod": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:salmon": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:tropical_fish": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
        },
        "minecraft:pufferfish": {
            "attribute": {
                "minecraft:generic.max_health": 1.0
            }
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

    // =========================================================
    // VALORES CARREGADOS
    // =========================================================

    public static JsonObject systemConfig = new JsonObject();
    public static JsonObject lpConfig = new JsonObject();
    public static JsonObject playerStats = new JsonObject();
    public static JsonObject mobStats = new JsonObject();
    public static JsonObject mobOverrides = new JsonObject();
    public static JsonObject mobLevels = new JsonObject();

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

        ConfigParser.parseSystem(systemConfig);
        ConfigParser.parseLp(lpConfig);
        ConfigParser.parsePlayerStats(playerStats);
        ConfigParser.parseMobStats(mobStats);
        ConfigParser.parseMobOverrides(mobOverrides);
        ConfigParser.parseMobLevels(mobLevels);

        CraftLevel.LOGGER.info("[CraftLevel] Config loaded from {}", configDir);
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