package com.grgs.craftlevel.villager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.VillagerConfigFiles;
import com.grgs.craftlevel.config.VillagerParser;
import com.grgs.craftlevel.pack.DynamicPackWriter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Datapack dinâmico CraftLevel_Professions — recriado a cada boot. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CraftLevelProfessionsDatapack {

    public static final String PACK_FOLDER = "CraftLevel_Professions";

    private CraftLevelProfessionsDatapack() {}

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        Path packRoot = packRoot();
        if (!Files.isDirectory(packRoot)) {
            return;
        }
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    PACK_FOLDER,
                    Component.literal("CraftLevel Professions"),
                    false,
                    id -> new PathPackResources(PACK_FOLDER, packRoot, true),
                    PackType.SERVER_DATA,
                    Pack.Position.BOTTOM,
                    PackSource.BUILT_IN
            );
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    public static void rebuildFromConfig() {
        try {
            List<ResourceLocation> poiIds = collectPoiIds();
            if (poiIds.isEmpty()) {
                DynamicPackWriter.deleteIfExists(packRoot());
                return;
            }

            Path datapacksDir = FMLPaths.GAMEDIR.get().resolve("datapacks");
            DynamicPackWriter.deleteLegacyFolders(datapacksDir, "craftlevel_professions");

            Path packRoot = DynamicPackWriter.rebuildRoot(packRoot());
            DynamicPackWriter.writePackMeta(packRoot, "CraftLevel Professions");

            JsonArray values = new JsonArray();
            poiIds.forEach(poiId -> values.add(poiId.toString()));

            JsonObject tag = new JsonObject();
            tag.add("values", values);

            Path tagFile = packRoot.resolve("data/minecraft/tags/point_of_interest_type/acquirable_job_site.json");
            Files.createDirectories(tagFile.getParent());
            Files.writeString(tagFile, tag + "\n");

            CraftLevel.LOGGER.info(
                    "[CraftLevel] Rebuilt datapack {} with {} POI(s): {}",
                    PACK_FOLDER,
                    poiIds.size(),
                    poiIds
            );
        } catch (Exception e) {
            CraftLevel.LOGGER.warn("[CraftLevel] Failed to rebuild datapack {}", PACK_FOLDER, e);
        }
    }

    public static Path packRoot() {
        return FMLPaths.GAMEDIR.get().resolve("datapacks").resolve(PACK_FOLDER);
    }

    private static List<ResourceLocation> collectPoiIds() {
        List<ResourceLocation> poiIds = new ArrayList<>();
        VillagerParser.parseProfessions(VillagerConfigFiles.loadVillagerJson()).forEach((id, def) -> {
            if (!id.startsWith("craftlevel:")) {
                return;
            }
            ResourceLocation professionId = ResourceLocation.tryParse(id);
            if (professionId == null || !StationBlocks.exists(def.station())) {
                return;
            }
            poiIds.add(poiIdFor(professionId));
        });
        return poiIds;
    }

    private static ResourceLocation poiIdFor(ResourceLocation professionId) {
        return ResourceLocation.fromNamespaceAndPath(CraftLevel.MODID, professionId.getPath() + "_poi");
    }
}
