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

    /** Nomes dos arquivos JSON (sem extensão) usados na tela de config. */
    public static final String[] CONFIG_FILES = {
            "config",
            "client",
            "player_stats",
            "mob_stats",
            "mob_overrides",
            "mob_levels"
    };

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
            "base_value": 200,
            "scaling": 1.06,
            "scaling_mode": "power"
        },
        "scaling": {
            "health": {
                "base_value": 1.0,
                "scaling": 1.045,
                "scaling_mode": "power",
                "apply_mode": "multiply"
            },
            "spell_power": {
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
            "attributeslib:current_hp_damage": "flat",
            "*irons_spellbooks*": "spell",
            "default": "health"
        },
        "hurt_self": {
            "minecraft:wither": "health",
            "minecraft:poison": "health",
            "minecraft:in_fire": "health",
            "minecraft:on_fire": "health",
            "minecraft:lava": "health",
            "minecraft:hot_floor": "health",
            "minecraft:drown": "health",
            "minecraft:starve": "health",
            "minecraft:cactus": "health",
            "minecraft:fall": "health",
            "minecraft:fly_into_wall": "health",
            "minecraft:generic": "health",
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
        "hurt_self": {
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
        }
    }
    """;

    private static final String DEFAULT_MOB_LEVELS = """
    {
        "minecraft:wither": { "lvl_min": 50, "lvl_max": 200 },
        "minecraft:warden": { "lvl_min": 50, "lvl_max": 200 },
        "minecraft:elder_guardian": { "lvl_min": 50, "lvl_max": 200 },
        "minecraft:ender_dragon": { "lvl_min": 100, "lvl_max": 200 },
        "minecraft:allay": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:axolotl": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:bat": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:bee": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:camel": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:cat": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:chicken": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:cod": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:cow": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:dolphin": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:donkey": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:fox": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:frog": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:glow_squid": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:horse": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:llama": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:mooshroom": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:mule": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:ocelot": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:panda": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:parrot": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:pig": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:pufferfish": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:rabbit": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:salmon": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:sheep": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:sniffer": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:snow_golem": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:squid": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:strider": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:tadpole": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:trader_llama": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:tropical_fish": { "lvl_min": 1, "lvl_max": 1 },
        "minecraft:turtle": { "lvl_min": 1, "lvl_max": 1 }
    }
    """;

    /** Default de client.json (HUD visual — só cliente). */
    public static final String DEFAULT_CLIENT = """
    {
        "hud": {
          "lp_bar": {
            "anchor": "bottom_left",
            "x": 2,
            "y": -2,
            "scale": 0.11,
            "font_scale": 0.5
          },
          "level_circle": {
            "anchor": "top_left",
            "x": 2,
            "y": 2,
            "scale": 0.15,
            "font_scale": 1.0
          },
          "health_bar": {
            "enabled": true,
            "anchor": "bottom_center",
            "x": -62,
            "y": -31,
            "scale": 0.1,
            "font_scale": 0.5
          },
          "hunger_bar": {
            "enabled": true,
            "anchor": "bottom_center",
            "x": 60,
            "y": -31,
            "scale": 0.1,
            "font_scale": 0.5
          }
        }
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

    public static String toPrettyJson(JsonObject json) {
        return GSON.toJson(json);
    }

    /** Cria config/craftlevel/client.json com defaults se não existir. */
    public static void writeDefaultClient(Path configDirectory) throws IOException {
        Files.createDirectories(configDirectory);
        Path file = configDirectory.resolve("client.json");
        if (!Files.exists(file)) {
            Files.writeString(file, DEFAULT_CLIENT);
        }
    }
}
