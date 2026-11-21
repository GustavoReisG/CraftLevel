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
 * Defaults canônicos ficam em {@code assets/craftlevel/defaults/} e são copiados
 * na primeira execução. Chamado no server start e em /craftlevel reload.
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
        AltarTradeParser.parse(altarOfDeath);
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
        config = loadOrCreate("config.json");
    }

    private static void loadPlayerStats() throws IOException {
        playerStats = loadOrCreate("player_stats.json");
    }

    private static void loadMobStats() throws IOException {
        mobStats = loadOrCreate("mob_stats.json");
    }

    private static void loadMobOverrides() throws IOException {
        mobOverrides = loadOrCreate("mob_overrides.json");
    }

    private static void loadMobLevels() throws IOException {
        mobLevels = loadOrCreate("mob_levels.json");
    }

    private static void loadEconomy() throws IOException {
        economy = loadOrCreate("economy.json");
    }

    private static void loadSouls() throws IOException {
        souls = loadOrCreate("souls.json");
    }

    private static void loadGloryKills() throws IOException {
        gloryKills = loadOrCreate("glory_kills.json");
    }

    private static void loadAltarConversion() throws IOException {
        altarConversion = loadOrCreate("altar_conversion.json");
    }

    private static void loadAltarOfDeath() throws IOException {
        altarOfDeath = loadOrCreate("altar_of_death.json");
    }

    private static void loadSkills() throws IOException {
        Path file = configDir.resolve("skills.json");
        ConfigDefaults.writeIfMissing(
                "skills.json",
                file,
                toPrettyJson(SkillsParser.toDefaultJson())
        );
        if (!Files.exists(file)) {
            Files.writeString(file, toPrettyJson(SkillsParser.toDefaultJson()));
        }
        skills = readJson(file);
    }

    private static void loadArena() {
        ArenaSettings.load(configDir.resolve("arena.json"), java.util.List.of());
    }

    private static void loadVillager() {
        try {
            Path file = configDir.resolve("villager.json");
            ConfigDefaults.copyIfMissing("villager.json", file);
            if (!Files.exists(file)) {
                Files.writeString(file, "{\"professions\":{},\"trades\":{}}");
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

    /** Cria o arquivo a partir do default em resources se não existir, depois lê. */
    private static JsonObject loadOrCreate(String fileName) throws IOException {
        Path file = configDir.resolve(fileName);
        ConfigDefaults.writeIfMissing(fileName, file, "{}");
        if (!Files.exists(file)) {
            Files.writeString(file, "{}");
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
        ConfigDefaults.writeIfMissing("client.json", file, "{\"hud\":{}}");
    }
}
