package com.grgs.craftlevel.villager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.grgs.craftlevel.CraftLevel;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Datapack dinâmico CraftLevel_Professions — recriado depois que as stations
 * já existem no block registry (não no FMLConstructModEvent).
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CraftLevelProfessionsDatapack {

    public static final String PACK_FOLDER = "CraftLevel_Professions";

    private CraftLevelProfessionsDatapack() {}

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        // Sempre registra o source; o pack pode ser escrito depois do RegisterEvent.
        event.addRepositorySource(consumer -> {
            Path packRoot = packRoot();
            if (!Files.isDirectory(packRoot) || !Files.isRegularFile(packRoot.resolve("pack.mcmeta"))) {
                return;
            }
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

    /** Reconstrói a tag acquirable_job_site a partir das profissões já carregadas. */
    public static void rebuildFromRegistered(Map<ResourceLocation, ResourceLocation> professions) {
        rebuild(poiIdsFor(professions.keySet()));
    }

    public static Path packRoot() {
        return FMLPaths.GAMEDIR.get().resolve("datapacks").resolve(PACK_FOLDER);
    }

    private static List<ResourceLocation> poiIdsFor(Collection<ResourceLocation> professionIds) {
        List<ResourceLocation> poiIds = new ArrayList<>(professionIds.size());
        for (ResourceLocation professionId : professionIds) {
            poiIds.add(poiIdFor(professionId));
        }
        return poiIds;
    }

    private static void rebuild(List<ResourceLocation> poiIds) {
        try {
            if (poiIds.isEmpty()) {
                DynamicPackWriter.deleteIfExists(packRoot());
                CraftLevel.LOGGER.info("[CraftLevel] No custom profession POIs; removed datapack {}", PACK_FOLDER);
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

    private static ResourceLocation poiIdFor(ResourceLocation professionId) {
        return ResourceLocation.fromNamespaceAndPath(CraftLevel.MODID, professionId.getPath() + "_poi");
    }
}
