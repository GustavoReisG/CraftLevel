package com.grgs.craftlevel.villager;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.config.VillagerConfigFiles;
import com.grgs.craftlevel.config.VillagerParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Registra profissões craftlevel:* com station block e POI (somente se a estação existir). */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VillagerProfessionRegistrar {

    private static final Map<ResourceLocation, ResourceLocation> CUSTOM_PROFESSIONS = new HashMap<>();
    private static boolean loaded;

    private VillagerProfessionRegistrar() {}

    @SubscribeEvent
    public static void onConstruct(FMLConstructModEvent event) {
        CraftLevelProfessionsDatapack.rebuildFromConfig();
        ProfessionLanguages.refresh();
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (isRegistry(event, Registries.POINT_OF_INTEREST_TYPE)) {
            ensureLoaded();
            registerPoiTypes(event);
        } else if (isRegistry(event, Registries.VILLAGER_PROFESSION)) {
            ensureLoaded();
            registerProfessions(event);
        }
    }

    public static Map<ResourceLocation, ResourceLocation> customProfessions() {
        ensureLoaded();
        return Map.copyOf(CUSTOM_PROFESSIONS);
    }

    private static boolean isRegistry(RegisterEvent event, ResourceKey<? extends Registry<?>> key) {
        return event.getRegistryKey().location().equals(key.location());
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        CUSTOM_PROFESSIONS.clear();

        JsonObject json = VillagerConfigFiles.loadVillagerJson();
        VillagerParser.parseProfessions(json).forEach((id, def) -> {
            if (!id.startsWith("craftlevel:")) {
                return;
            }
            ResourceLocation professionId = ResourceLocation.tryParse(id);
            ResourceLocation stationId = ResourceLocation.tryParse(def.station());
            if (professionId == null || stationId == null || stationId.toString().isBlank()) {
                return;
            }
            if (!StationBlocks.exists(stationId)) {
                CraftLevel.LOGGER.warn(
                        "[CraftLevel] Skipping profession {}: station {} not in block registry",
                        professionId,
                        stationId
                );
                return;
            }
            CUSTOM_PROFESSIONS.put(professionId, stationId);
        });

        CraftLevel.LOGGER.info(
                "[CraftLevel] Loaded {} custom profession(s) from villager.json: {}",
                CUSTOM_PROFESSIONS.size(),
                CUSTOM_PROFESSIONS.keySet()
        );
    }

    private static void registerPoiTypes(RegisterEvent event) {
        CUSTOM_PROFESSIONS.forEach((professionId, stationId) -> {
            Block block = BuiltInRegistries.BLOCK.get(stationId);
            Set<BlockState> states = new HashSet<>(block.getStateDefinition().getPossibleStates());
            ResourceLocation poiId = poiIdFor(professionId);
            event.register(Registries.POINT_OF_INTEREST_TYPE, poiId, () -> new PoiType(states, 1, 1));
            CraftLevel.LOGGER.info("[CraftLevel] Registered POI {} for profession {}", poiId, professionId);
        });
    }

    private static void registerProfessions(RegisterEvent event) {
        CUSTOM_PROFESSIONS.forEach((professionId, stationId) -> {
            ResourceKey<PoiType> poiKey = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, poiIdFor(professionId));

            event.register(Registries.VILLAGER_PROFESSION, professionId, () ->
                    new VillagerProfession(
                            professionId.getPath(),
                            holder -> holder.is(poiKey),
                            holder -> holder.is(poiKey),
                            ImmutableSet.of(),
                            ImmutableSet.of(),
                            SoundEvents.VILLAGER_WORK_ARMORER
                    ));
            CraftLevel.LOGGER.info(
                    "[CraftLevel] Registered villager profession {} with station {}",
                    professionId,
                    stationId
            );
        });
    }

    private static ResourceLocation poiIdFor(ResourceLocation professionId) {
        return ResourceLocation.fromNamespaceAndPath(CraftLevel.MODID, professionId.getPath() + "_poi");
    }
}
