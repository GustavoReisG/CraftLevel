package com.grgs.craftlevel.villager;

import com.grgs.craftlevel.CraftLevel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;
import java.util.Locale;

/**
 * Gera mapas de exploração apontando para uma structure aleatória do registry
 * (vanilla + mods). O item JSON {@code craftlevel:explorer_map} é só um marcador.
 */
public final class ExplorerMapService {

    public static final String MARKER_ID = "craftlevel:explorer_map";

    private static final int SEARCH_RADIUS = 100;
    private static final int MAX_ATTEMPTS = 12;
    private static final byte MAP_SCALE = 2;

    private ExplorerMapService() {}

    public static boolean isMarker(VillagerTradeSlotDef slot) {
        return slot != null && MARKER_ID.equals(slot.item());
    }

    public static boolean isMarkerSlot(ResolvedTradeSlot slot) {
        return slot != null && MARKER_ID.equals(slot.itemId());
    }

    public static ResolvedTradeSlot markerSlot() {
        return new ResolvedTradeSlot(MARKER_ID, 1, "");
    }

    public static ItemStack create(AbstractVillager villager, RandomSource random) {
        if (!(villager.level() instanceof ServerLevel origin)) {
            return ItemStack.EMPTY;
        }

        MinecraftServer server = origin.getServer();
        Registry<Structure> structures = origin.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder.Reference<Structure>> all = structures.holders().toList();
        if (all.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Holder.Reference<Structure> chosen = all.get(random.nextInt(all.size()));
            ResourceKey<Level> dim = guessDimension(chosen.value());
            ServerLevel searchLevel = server.getLevel(dim);
            if (searchLevel == null) {
                continue;
            }

            BlockPos originPos = convertPos(villager.blockPosition(), origin.dimension(), dim);
            Pair<BlockPos, Holder<Structure>> found = searchLevel.getChunkSource().getGenerator()
                    .findNearestMapStructure(
                            searchLevel,
                            HolderSet.direct(chosen),
                            originPos,
                            SEARCH_RADIUS,
                            true
                    );
            if (found == null) {
                continue;
            }

            BlockPos target = found.getFirst();
            ItemStack map = MapItem.create(
                    searchLevel,
                    target.getX(),
                    target.getZ(),
                    MAP_SCALE,
                    true,
                    true
            );
            MapItem.renderBiomePreviewMap(searchLevel, map);
            MapItemSavedData.addTargetDecoration(map, target, "+", decorationFor(chosen.key().location()));
            map.setHoverName(Component.translatable(
                    "item.craftlevel.explorer_map.named",
                    prettyName(chosen.key().location())
            ));
            return map;
        }

        CraftLevel.LOGGER.debug(
                "[CraftLevel] Explorer map: no structure found near {} after {} attempts",
                villager.blockPosition(),
                MAX_ATTEMPTS
        );
        return ItemStack.EMPTY;
    }

    private static ResourceKey<Level> guessDimension(Structure structure) {
        HolderSet<Biome> biomes = structure.biomes();
        for (Holder<Biome> biome : biomes) {
            if (biome.is(BiomeTags.IS_NETHER)) {
                return Level.NETHER;
            }
            if (biome.is(BiomeTags.IS_END)) {
                return Level.END;
            }
        }
        return Level.OVERWORLD;
    }

    private static BlockPos convertPos(BlockPos pos, ResourceKey<Level> from, ResourceKey<Level> to) {
        if (from.equals(to)) {
            return pos;
        }
        if (from.equals(Level.OVERWORLD) && to.equals(Level.NETHER)) {
            return new BlockPos(pos.getX() / 8, pos.getY(), pos.getZ() / 8);
        }
        if (from.equals(Level.NETHER) && to.equals(Level.OVERWORLD)) {
            return new BlockPos(pos.getX() * 8, pos.getY(), pos.getZ() * 8);
        }
        return pos;
    }

    private static MapDecoration.Type decorationFor(ResourceLocation id) {
        String path = id.getPath();
        if (path.contains("monument")) {
            return MapDecoration.Type.MONUMENT;
        }
        if (path.contains("mansion")) {
            return MapDecoration.Type.MANSION;
        }
        return MapDecoration.Type.RED_X;
    }

    private static Component prettyName(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < path.length()) {
            path = path.substring(slash + 1);
        }
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        String label = sb.toString();
        if (!"minecraft".equals(id.getNamespace())) {
            label = id.getNamespace() + ": " + label;
        }
        return Component.literal(label);
    }
}
