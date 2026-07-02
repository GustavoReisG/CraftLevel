package com.grgs.craftlevel.config;

import com.google.gson.*;
import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.util.DebugLog;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.*;
import java.nio.file.*;

@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
/**
 * Carrega e persiste os JSONs em {@code config/craftlevel/}.
 * <p>
 * Cria arquivos default na primeira execução. Chamado no server start e em /craftlevel reload.
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configDir;

    // =========================================================
    // DEFAULTS
    // =========================================================

    private static final String DEFAULT_CONFIG = """
    {
        "debug": true,
        "lp_scaling": {
            "mob_level_variation": 10,
            "lvl_max": 200,
            "base_value": 100,
            "scaling": 1.06,
            "scaling_mode": "power",
            "apply_mode": "overwrite"
        },
        "scaling": {
            "health": {
                "base_value": 1.0,
                "scaling": 1.045,
                "scaling_mode": "power",
                "apply_mode": "multiply"
            },
            "spell": {
                "base_value": 1.0,
                "scaling": 1.045,
                "scaling_mode": "power",
                "apply_mode": "multiply"
            },
            "melee_power": {
                "base_value": 1.0,
                "scaling": 1.045,
                "scaling_mode": "power",
                "apply_mode": "multiply"
            },
            "mob_health": {
                "base_value": 1.0,
                "scaling": 1.05,
                "scaling_mode": "power",
                "apply_mode": "multiply"
            },
            "neutral_health": {
                "base_value": 1.0,
                "scaling": 1.0,
                "scaling_mode": "power",
                "apply_mode": "multiply"
            },
            "flat": {
                "base_value": 1.0,
                "scaling": 1.0,
                "scaling_mode": "power",
                "apply_mode": "multiply"
            }
        }
    }
    """;

    private static final String DEFAULT_PLAYER_STATS = """
    {
        "attribute": {
            "minecraft:generic.max_health": "health"
        },
        "hurt": {
            "minecraft:player_attack": "melee_power",
            "minecraft:arrow": "melee_power",
            "minecraft:trident": "melee_power",
            "minecraft:magic": "spell",
            "minecraft:indirect_magic": "spell",
            "minecraft:wither": "health",
            "minecraft:poison": "health",
            "attributeslib:current_hp_damage": "flat",
            "*irons_spellbooks*": "spell",
            "default": "health"
        },
        "heal": {
            "default": "health"
        }
    }
    """;

    private static final String DEFAULT_MOB_STATS = """
    {
        "attribute": {
            "minecraft:generic.max_health": "mob_health"
        },
        "hurt": {
            "default": "mob_health"
        },
        "heal": {
            "default": "mob_health"
        }
    }
    """;

    private static final String DEFAULT_MOB_OVERRIDES = """
    {
        "minecraft:cow": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:sheep": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:pig": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:chicken": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:rabbit": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:squid": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:bat": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:glow_squid": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:cod": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:salmon": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:tropical_fish": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
            }
        },
        "minecraft:pufferfish": {
            "attribute": {
                "minecraft:generic.max_health": "flat"
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

    public static JsonObject config = new JsonObject();
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

        loadConfig();
        loadPlayerStats();
        loadMobStats();
        loadMobOverrides();
        loadMobLevels();

        ConfigParser.parseConfig(config);
        ConfigParser.parsePlayerStats(playerStats);
        ConfigParser.parseMobStats(mobStats);
        ConfigParser.parseMobOverrides(mobOverrides);
        ConfigParser.parseMobLevels(mobLevels);

        CraftLevel.LOGGER.info("[CraftLevel] Config loaded from {}", configDir);
        DebugLog.info(
                "Configs carregados: playerStats={} attrs, mobOverrides={}, mobLevels={}",
                ConfigParser.playerStats.attribute().size(),
                ConfigParser.mobOverrides.size(),
                ConfigParser.mobLevels.size()
        );
    }

    /** Recarrega JSONs do diretório já inicializado ou do servidor informado. */
    public static void reload(MinecraftServer server) throws IOException {
        Path dir = configDir != null
                ? configDir
                : server.getServerDirectory().toPath().resolve("config/craftlevel");
        loadAllConfigs(dir);
    }

    // =========================================================
    // LOADERS
    // =========================================================

    private static void loadConfig() throws IOException {
        config = loadOrCreate("config.json", DEFAULT_CONFIG);
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
