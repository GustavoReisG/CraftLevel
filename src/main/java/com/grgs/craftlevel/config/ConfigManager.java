package com.grgs.craftlevel.config;

import com.google.gson.*;
import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.arena.ArenaSettings;
import com.grgs.craftlevel.util.DebugLog;
import com.grgs.craftlevel.villager.ProfessionLanguages;
import com.grgs.craftlevel.villager.VillagerTradeCatalog;
import com.grgs.craftlevel.villager.VillagerTradeService;
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
            "client",
            "config",
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
      "anvil": {
        "uncapped_cost": true
      },
      "lp_scaling": {
        "mob_level_variation": 10,
        "lvl_max": 200,
        "base_value": 200.0,
        "scaling": 1.06,
        "scaling_mode": "power"
      },
      "scaling": {
        "spell_power": {
          "base_value": 1.0,
          "scaling": 1.045,
          "scaling_mode": "power",
          "apply_mode": "multiply",
          "include_modifiers": false
        },
        "mob_health": {
          "base_value": 1.0,
          "scaling": 1.05,
          "scaling_mode": "power",
          "apply_mode": "multiply",
          "include_modifiers": false
        },
        "flat": {
          "base_value": 1.0,
          "scaling": 1.0,
          "scaling_mode": "power",
          "apply_mode": "multiply",
          "include_modifiers": false
        },
        "health": {
          "base_value": 1.0,
          "scaling": 1.045,
          "scaling_mode": "power",
          "apply_mode": "multiply",
          "include_modifiers": true
        },
        "melee_power": {
          "base_value": 1.0,
          "scaling": 1.045,
          "scaling_mode": "power",
          "apply_mode": "multiply",
          "include_modifiers": false
        },
        "none": {
          "base_value": 0.0,
          "scaling": 0.0,
          "scaling_mode": "linear",
          "apply_mode": "overwrite",
          "include_modifiers": false
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
          "default": "health",
          "minecraft:player_attack": "melee_power",
          "minecraft:arrow": "melee_power",
          "minecraft:trident": "melee_power",
          "minecraft:magic": "spell_power",
          "minecraft:indirect_magic": "spell_power",
          "attributeslib:current_hp_damage": "flat",
          "*irons_spellbooks*": "spell_power",
          "cataclysm:lightning": "flat",
          "minecraft:mob_projectile": "flat"
        },
        "hurt_self": {
          "default": "health"
        },
        "heal": {
          "default": "health",
          "craftlevel:vampire_attack_heal": "flat"
        }
      }
    """;

    private static final String DEFAULT_MOB_STATS = """
    {
      "attribute": {
        "minecraft:generic.max_health": "mob_health"
      },
      "hurt": {
        "default": "health"
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
      "cataclysm:ancient_remnant": {
        "attribute": {
          "minecraft:generic.max_health": "mob_health"
        },
        "hurt": {
          "default": "health"
        },
        "hurt_self": {
          "default": "mob_health",
          "attributeslib:current_hp_damage": "none"
        },
        "heal": {
          "default": "mob_health"
        }
      },
      "cataclysm:maledictus": {
        "attribute": {
          "minecraft:generic.max_health": "mob_health"
        },
        "hurt": {
          "default": "health"
        },
        "hurt_self": {
          "default": "mob_health",
          "attributeslib:current_hp_damage": "none"
        },
        "heal": {
          "default": "mob_health"
        }
      },
      "cataclysm:ender_guardian": {
        "attribute": {
          "minecraft:generic.max_health": "mob_health"
        },
        "hurt": {
          "default": "health"
        },
        "hurt_self": {
          "default": "mob_health",
          "attributeslib:current_hp_damage": "none"
        },
        "heal": {
          "default": "mob_health"
        }
      },
      "cataclysm:ignis": {
        "attribute": {
          "minecraft:generic.max_health": "mob_health"
        },
        "hurt": {
          "default": "health"
        },
        "hurt_self": {
          "default": "mob_health",
          "attributeslib:current_hp_damage": "none"
        },
        "heal": {
          "default": "mob_health"
        }
      },
      "cataclysm:scylla": {
        "attribute": {
          "minecraft:generic.max_health": "mob_health"
        },
        "hurt": {
          "default": "health"
        },
        "hurt_self": {
          "default": "mob_health",
          "attributeslib:current_hp_damage": "none"
        },
        "heal": {
          "default": "mob_health"
        }
      },
      "cataclysm:netherite_monstrosity": {
        "attribute": {
          "minecraft:generic.max_health": "mob_health"
        },
        "hurt": {
          "default": "health"
        },
        "hurt_self": {
          "default": "mob_health",
          "attributeslib:current_hp_damage": "none"
        },
        "heal": {
          "default": "mob_health"
        }
      },
      "cataclysm:the_harbinger": {
        "attribute": {
          "minecraft:generic.max_health": "mob_health"
        },
        "hurt": {
          "default": "health"
        },
        "hurt_self": {
          "default": "mob_health",
          "attributeslib:current_hp_damage": "none"
        },
        "heal": {
          "default": "mob_health"
        }
      },
      "cataclysm:the_leviathan": {
        "attribute": {
          "minecraft:generic.max_health": "mob_health"
        },
        "hurt": {
          "default": "health"
        },
        "hurt_self": {
          "default": "mob_health",
          "attributeslib:current_hp_damage": "none"
        },
        "heal": {
          "default": "mob_health"
        }
      },
      "irons_spellbooks:dead_king": {
        "attribute": {
          "minecraft:generic.max_health": "mob_health"
        },
        "hurt": {
          "default": "health"
        },
        "hurt_self": {
          "default": "mob_health",
          "attributeslib:current_hp_damage": "none"
        },
        "heal": {
          "default": "mob_health"
        }
      }
    }
    """;

    private static final String DEFAULT_MOB_LEVELS = """
    {
       "minecraft:wither": {
         "lvl_min": 50,
         "lvl_max": 200
       },
       "minecraft:warden": {
         "lvl_min": 50,
         "lvl_max": 200
       },
       "minecraft:elder_guardian": {
         "lvl_min": 50,
         "lvl_max": 200
       },
       "minecraft:ender_dragon": {
         "lvl_min": 100,
         "lvl_max": 200
       },
       "minecraft:allay": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:axolotl": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:bat": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:bee": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:camel": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:cat": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:chicken": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:cod": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:cow": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:dolphin": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:donkey": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:fox": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:frog": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:glow_squid": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:horse": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:llama": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:mooshroom": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:mule": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:ocelot": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:panda": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:parrot": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:pig": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:pufferfish": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:rabbit": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:salmon": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:sheep": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:sniffer": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:snow_golem": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:squid": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:strider": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:tadpole": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:trader_llama": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:tropical_fish": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "minecraft:turtle": {
         "lvl_min": 1,
         "lvl_max": 1
       },
       "cataclysm:the_leviathan": {
         "lvl_min": 100,
         "lvl_max": 200
       },
       "cataclysm:the_harbinger": {
         "lvl_min": 100,
         "lvl_max": 200
       },
       "cataclysm:netherite_monstrosity": {
         "lvl_min": 100,
         "lvl_max": 200
       },
       "cataclysm:scylla": {
         "lvl_min": 100,
         "lvl_max": 200
       },
       "cataclysm:ignis": {
         "lvl_min": 100,
         "lvl_max": 200
       },
       "cataclysm:ender_guardian": {
         "lvl_min": 100,
         "lvl_max": 200
       },
       "cataclysm:maledictus": {
         "lvl_min": 100,
         "lvl_max": 200
       },
       "cataclysm:ancient_remnant": {
         "lvl_min": 100,
         "lvl_max": 200
       },
       "irons_spellbooks:dead_king": {
         "lvl_min": 100,
         "lvl_max": 200
       }
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
          "font_scale": 0.7
        },
        "level_circle": {
          "anchor": "top_left",
          "x": 2,
          "y": 2,
          "scale": 0.15,
          "font_scale": 1.4,
          "font_y_offset": -0.0
        },
        "health_bar": {
          "enabled": true,
          "anchor": "bottom_center",
          "x": -62,
          "y": -31,
          "scale": 0.1,
          "font_scale": 0.6,
          "armor_margin": 10
        },
        "hunger_bar": {
          "enabled": true,
          "anchor": "bottom_center",
          "x": 60,
          "y": -31,
          "scale": 0.1,
          "font_scale": 0.6,
          "breath_margin": 10,
          "mount_health_margin": 0
        },
        "emeralds": {
          "enabled": true,
          "anchor": "bottom_left",
          "x": 2,
          "y": -20,
          "scale": 0.7,
          "font_scale": 1.0,
          "icon_side": "left"
        },
        "souls": {
          "enabled": true,
          "anchor": "top_left",
          "x": 2,
          "y": 50,
          "scale": 0.7,
          "font_scale": 1.0,
          "icon_side": "left"
        },
        "glory_kills": {
          "enabled": true,
          "anchor": "top_left",
          "x": 2,
          "y": 70,
          "scale": 0.7,
          "font_scale": 1.0,
          "icon_side": "left"
        },
        "mob_focus": {
          "anchor": "top_left",
          "x": 50,
          "y": 2,
          "scale": 0.15,
          "font_scale": 4.0,
          "direction": "portrait_first",
          "mob_yaw": 0.6,
          "head_yaw": 0.0,
          "camera_pitch": -0.2
        },
        "skills": {
          "enabled": true,
          "direction": "row",
          "fill_sense": "ascending",
          "text_sense": "after_icon",
          "anchor": "top_right",
          "x": -8,
          "y": 40,
          "scale": 0.85
        }
      }
    }
    """;

    public static final String DEFAULT_ECONOMY = """
    {
      "player": {
        "starting_balance": 0
      },
      "villager_balance": {
        "refresh_ticks": 24000,
        "default_starting": 1000,
        "per_profession": {
          "minecraft:librarian": {
            "balance": 1000,
            "refresh_ticks": 24000
          }
        }
      },
      "kill_rewards": {
        "same_level_emeralds": 1,
        "low_level_emeralds": 0,
        "high_level_bonus": 2,
        "level_gap": 5,
        "boss_bonus": 64,
        "bosses": [
          "minecraft:wither",
          "minecraft:warden",
          "minecraft:elder_guardian",
          "minecraft:ender_dragon",
          "cataclysm:the_leviathan",
          "cataclysm:the_harbinger",
          "cataclysm:netherite_monstrosity",
          "cataclysm:scylla",
          "cataclysm:ignis",
          "cataclysm:ender_guardian",
          "cataclysm:maledictus",
          "cataclysm:ancient_remnant",
          "irons_spellbooks:dead_king"
        ],
        "blacklist": [
          "minecraft:allay",
          "minecraft:axolotl",
          "minecraft:bat",
          "minecraft:bee",
          "minecraft:camel",
          "minecraft:cat",
          "minecraft:chicken",
          "minecraft:cod",
          "minecraft:cow",
          "minecraft:dolphin",
          "minecraft:donkey",
          "minecraft:fox",
          "minecraft:frog",
          "minecraft:glow_squid",
          "minecraft:goat",
          "minecraft:horse",
          "minecraft:llama",
          "minecraft:mooshroom",
          "minecraft:mule",
          "minecraft:ocelot",
          "minecraft:panda",
          "minecraft:parrot",
          "minecraft:pig",
          "minecraft:pufferfish",
          "minecraft:rabbit",
          "minecraft:salmon",
          "minecraft:sheep",
          "minecraft:sniffer",
          "minecraft:snow_golem",
          "minecraft:squid",
          "minecraft:strider",
          "minecraft:tadpole",
          "minecraft:trader_llama",
          "minecraft:tropical_fish",
          "minecraft:turtle",
          "minecraft:wolf"
        ]
      }
    }
    """;

    private static final String DEFAULT_SOULS = """
    {
      "max_souls": 3,
      "time_restore": 1200,
      "lost_emeralds_type": "percentage",
      "lost_emeralds_quantity": 10,
      "lost_items": "random",
      "gravestone": true
    }
    """;

    private static final String DEFAULT_GLORY_KILLS = """
    {
      "kills_per_shard": 100,
      "min_level_gap": 5
    }
    """;

    private static final String DEFAULT_ALTAR_CONVERSION = """
    {
      "conversions": [
        { "item": "minecraft:coal", "count": 1, "glory_cost": 200, "skill_points": 1 },
        { "item": "minecraft:copper_ingot", "count": 1, "glory_cost": 175, "skill_points": 1 },
        { "item": "minecraft:iron_ingot", "count": 1, "glory_cost": 150, "skill_points": 1 },
        { "item": "minecraft:gold_ingot", "count": 1, "glory_cost": 125, "skill_points": 1 },
        { "item": "minecraft:amethyst_shard", "count": 1, "glory_cost": 100, "skill_points": 1 },
        { "item": "minecraft:diamond", "count": 1, "glory_cost": 80, "skill_points": 1 }
      ]
    }
    """;

    public static final String DEFAULT_ALTAR_OF_DEATH = """
    {
      "potion": {
        "glory_cost_time": 10,
        "glory_cost_strength": 10,
        "max_duration_seconds": 600,
        "max_level": 10,
        "effect_limits": {
          "minecraft:regeneration": {
            "time": 600,
            "level": 3
          },
          "minecraft:instant_health": {
            "time": 1,
            "level": 5
          },
          "minecraft:instant_damage": {
            "time": 1,
            "level": 5
          },
          "irons_spellbooks:instant_mana": {
            "time": 1,
            "level": 5
          }
        }
      },
      "improvement": {
        "glory_cost": 20,
        "generic_enchantment_limit": 50,
        "enchantment_limits": {
          "minecraft:aqua_affinity": 1,
          "minecraft:binding_curse": 1,
          "minecraft:channeling": 1,
          "minecraft:flame": 1,
          "minecraft:infinity": 1,
          "minecraft:mending": 1,
          "minecraft:multishot": 1,
          "minecraft:silk_touch": 1,
          "minecraft:vanishing_curse": 1
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
    public static JsonObject economy = new JsonObject();
    public static JsonObject villager = new JsonObject();
    public static JsonObject souls = new JsonObject();
    public static JsonObject gloryKills = new JsonObject();
    public static JsonObject altarConversion = new JsonObject();
    public static JsonObject altarOfDeath = new JsonObject();
    public static JsonObject skills = new JsonObject();

    // =========================================================
    // INIT
    // =========================================================

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        try {
            loadAllConfigs(event.getServer().getServerDirectory().toPath().resolve("config/craftlevel"));
        } catch (Exception e) {
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
        loadEconomy();
        loadSouls();
        loadGloryKills();
        loadAltarConversion();
        loadAltarOfDeath();
        loadSkills();
        loadVillager();
        loadArena();

        ConfigParser.parseConfig(config);
        ConfigParser.parsePlayerStats(playerStats);
        ConfigParser.parseMobStats(mobStats);
        ConfigParser.parseMobOverrides(mobOverrides);
        ConfigParser.parseMobLevels(mobLevels);
        EconomyParser.parseEconomy(economy);
        SoulsParser.parse(souls);
        GloryKillsParser.parse(gloryKills);
        AltarConversionParser.parse(altarConversion);
        AltarPotionParser.parse(altarOfDeath);
        AltarImprovementParser.parse(altarOfDeath);
        SkillsParser.parse(skills);
        VillagerTradeCatalog.load(villager);
        VillagerTradeService.onReload();
        ProfessionLanguages.refresh();

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

    private static void loadEconomy() throws IOException {
        economy = loadOrCreate("economy.json", DEFAULT_ECONOMY);
    }

    private static void loadSouls() throws IOException {
        souls = loadOrCreate("souls.json", DEFAULT_SOULS);
    }

    private static void loadGloryKills() throws IOException {
        gloryKills = loadOrCreate("glory_kills.json", DEFAULT_GLORY_KILLS);
    }

    private static void loadAltarConversion() throws IOException {
        altarConversion = loadOrCreate("altar_conversion.json", DEFAULT_ALTAR_CONVERSION);
    }

    private static void loadAltarOfDeath() throws IOException {
        altarOfDeath = loadOrCreate("altar_of_death.json", DEFAULT_ALTAR_OF_DEATH);
    }

    private static void loadSkills() throws IOException {
        skills = loadOrCreate("skills.json", toPrettyJson(SkillsParser.toDefaultJson()));
    }

    private static void loadArena() {
        ArenaSettings.load(configDir.resolve("arena.json"), java.util.List.of());
    }

    private static void loadVillager() {
        try {
            Path file = configDir.resolve("villager.json");
            if (!Files.exists(file)) {
                copyDefaultVillager(file);
            }
            villager = readJson(file);
        } catch (Exception e) {
            CraftLevel.LOGGER.error("[CraftLevel] Failed to load villager.json, using empty defaults", e);
            villager = new JsonObject();
            villager.add("professions", new JsonObject());
            villager.add("trades", new JsonObject());
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private static void copyDefaultVillager(Path target) throws IOException {
        try (InputStream stream = ConfigManager.class.getClassLoader()
                .getResourceAsStream("assets/craftlevel/defaults/villager.json")) {
            if (stream == null) {
                Files.writeString(target, "{\"professions\":{},\"trades\":{}}");
                return;
            }
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

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
