package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.VillagerConfigFiles;
import com.grgs.craftlevel.config.VillagerParser;
import com.grgs.craftlevel.pack.DynamicPackWriter;
import com.grgs.craftlevel.villager.ProfessionIds;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Resource pack CraftLevel_Textures em resourcepacks/CraftLevel_Textures/.
 * O Minecraft descobre essa pasta automaticamente (file/CraftLevel_Textures).
 * PNGs reais no disco; profissões sem arquivo recebem cópia do wandering trader no boot.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CraftLevelTexturesPack {

    public static final String PACK_FOLDER = "CraftLevel_Textures";
    /** ID usado pelo scanner vanilla de resourcepacks/. */
    public static final String PACK_ID = "file/" + PACK_FOLDER;
    private static final String VANILLA_PROFESSION_DIR =
            "assets/craftlevel/textures/entity/villager/profession";
    private static final String DEFAULT_PACK_META = "/assets/craftlevel/defaults/CraftLevel_Textures/pack.mcmeta";
    private static final String DEFAULT_WANDERING_TRADER = "/assets/craftlevel/defaults/wandering_trader.png";

    private CraftLevelTexturesPack() {}

    @SubscribeEvent
    public static void onConstruct(FMLConstructModEvent event) {
        cleanupLegacyFolders();
        ensurePackStructure();
        seedProfessionTextures();
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                ensurePackSelected(minecraft);
            }
        });
    }

    static void ensurePackSelected(Minecraft minecraft) {
        PackRepository repository = minecraft.getResourcePackRepository();
        List<String> selected = new ArrayList<>(repository.getSelectedIds());
        boolean changed = selected.removeIf(id ->
                id.equals(PACK_FOLDER) || id.equals(PACK_FOLDER + ".zip"));

        if (!selected.contains(PACK_ID)) {
            selected.add(PACK_ID);
            changed = true;
        }

        if (changed) {
            repository.setSelected(selected);
            minecraft.options.resourcePacks = new ArrayList<>(selected);
            minecraft.options.save();
        }
    }

    public static Path packRoot() {
        return FMLPaths.GAMEDIR.get().resolve("resourcepacks").resolve(PACK_FOLDER);
    }

    public static Path professionsFolder() {
        return packRoot().resolve(VANILLA_PROFESSION_DIR);
    }

    public static Path packTexturePath(String professionName) {
        return professionsFolder().resolve(professionName + ".png");
    }

    public static void ensurePackStructure() {
        Path root = packRoot();
        try {
            Files.createDirectories(professionsFolder());
            Path meta = root.resolve("pack.mcmeta");
            if (!Files.exists(meta)) {
                try (InputStream in = CraftLevel.class.getResourceAsStream(DEFAULT_PACK_META)) {
                    if (in != null) {
                        Files.copy(in, meta, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        DynamicPackWriter.writePackMeta(root, "CraftLevel Textures");
                    }
                }
                CraftLevel.LOGGER.info("[CraftLevel] Created resource pack at {}", root);
            }
        } catch (IOException e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to create resource pack {}", PACK_FOLDER, e);
        }
    }

    public static void seedProfessionTextures() {
        ensurePackStructure();
        try {
            VillagerParser.parseProfessions(VillagerConfigFiles.loadVillagerJson()).forEach((id, def) -> {
                if (!id.startsWith("craftlevel:")) {
                    return;
                }
                String professionPath = ProfessionIds.pathFromFullId(id);
                Path target = packTexturePath(professionPath);
                if (Files.exists(target)) {
                    return;
                }
                try {
                    Files.createDirectories(target.getParent());
                    String defaultPath = "/assets/craftlevel/defaults/" + professionPath + ".png";
                    try (InputStream specific = CraftLevel.class.getResourceAsStream(defaultPath)) {
                        if (specific != null) {
                            Files.copy(specific, target, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            try (InputStream fallback = CraftLevel.class.getResourceAsStream(DEFAULT_WANDERING_TRADER)) {
                                if (fallback == null) {
                                    CraftLevel.LOGGER.warn("[CraftLevel] Missing default wandering_trader.png in mod jar");
                                    return;
                                }
                                Files.copy(fallback, target, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                    CraftLevel.LOGGER.info("[CraftLevel] Seeded profession texture {}", target);
                } catch (IOException e) {
                    CraftLevel.LOGGER.warn("[CraftLevel] Failed to seed texture for {}", id, e);
                }
            });
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to seed profession textures", e);
        }
    }

    private static void cleanupLegacyFolders() {
        Path gameDir = FMLPaths.GAMEDIR.get();
        DynamicPackWriter.deleteLegacyFolders(
                gameDir.resolve("resourcepacks"),
                "craftlevel_profession_textures"
        );
        DynamicPackWriter.deleteIfExists(
                gameDir.resolve("config/craftlevel/packs").resolve(PACK_FOLDER)
        );
    }
}
