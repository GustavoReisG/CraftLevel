package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncProspectorHighlightsPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Prospector: destaca minérios no raio (outline colorido).
 * Raio: 8 / 12. Highlights: 2 min. CD: 3 min.
 */
public final class ProspectorSkill {

    public static final int COOLDOWN_TICKS = 3 * 60 * 20;
    public static final int HIGHLIGHT_DURATION_TICKS = 2 * 60 * 20;
    private static final int MAX_HIGHLIGHTS = 512;

    private static final TagKey<Block> FORGE_ORES =
            TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores"));
    private static final TagKey<Block> C_ORES =
            TagKey.create(Registries.BLOCK, new ResourceLocation("c", "ores"));

    public record Highlight(BlockPos pos, int rgb) {}

    private ProspectorSkill() {}

    /** Lvl 1: 8, lvl 2+: 12. */
    public static int radiusBlocks(int level) {
        return Math.max(1, level) >= 2 ? 12 : 8;
    }

    /** Highlights ficam 2 min em todos os níveis. */
    public static int durationSeconds(int level) {
        return HIGHLIGHT_DURATION_TICKS / 20;
    }

    public static int durationMinutes() {
        return HIGHLIGHT_DURATION_TICKS / (60 * 20);
    }

    public static int durationTicks(int level) {
        return HIGHLIGHT_DURATION_TICKS;
    }

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static void activate(ServerPlayer player, int level) {
        int radius = radiusBlocks(level);
        long until = player.serverLevel().getGameTime() + HIGHLIGHT_DURATION_TICKS;

        List<Highlight> highlights = scanOres(player.serverLevel(), player.blockPosition(), radius);
        PacketHandler.sendToPlayer(player, new SyncProspectorHighlightsPacket(until, highlights));
        ActiveSkillTracker.put(player, SkillCatalog.PROSPECTOR, until);

        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.SPYGLASS_USE,
                SoundSource.PLAYERS,
                0.9F,
                0.85F
        );
    }

    private static List<Highlight> scanOres(ServerLevel level, BlockPos center, int radius) {
        List<Highlight> found = new ArrayList<>();
        int r2 = radius * radius;
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (center.distSqr(pos) > r2) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            int color = colorFor(state, level, pos);
            if (color < 0) {
                continue;
            }
            found.add(new Highlight(pos.immutable(), color));
            if (found.size() >= MAX_HIGHLIGHTS) {
                break;
            }
        }
        return found;
    }

    /** @return RGB 0xRRGGBB, ou −1 se não for minério. */
    static int colorFor(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.is(BlockTags.IRON_ORES)) {
            return 0x9E9E9E;
        }
        if (state.is(BlockTags.GOLD_ORES)) {
            return 0xFFD700;
        }
        if (state.is(BlockTags.REDSTONE_ORES)) {
            return 0xE53935;
        }
        if (state.is(BlockTags.COPPER_ORES)) {
            return 0xFF8A3D;
        }
        if (state.is(BlockTags.DIAMOND_ORES)) {
            return 0x00E5FF;
        }
        if (state.is(BlockTags.LAPIS_ORES)) {
            return 0x1E88E5;
        }
        if (state.is(BlockTags.EMERALD_ORES)) {
            return 0x43A047;
        }
        if (isAmethyst(state)) {
            return 0x9B59B6;
        }
        if (state.is(FORGE_ORES) || state.is(C_ORES) || state.is(BlockTags.COAL_ORES)) {
            return mapColorOrWhite(state, level, pos);
        }
        // Tags de mods podem não carregar; heurística por id.
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id != null) {
            String path = id.getPath();
            if (path.contains("_ore") || path.contains("ore_") || path.endsWith("ore")) {
                return mapColorOrWhite(state, level, pos);
            }
        }
        return -1;
    }

    private static boolean isAmethyst(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.AMETHYST_BLOCK
                || block == Blocks.BUDDING_AMETHYST
                || block == Blocks.AMETHYST_CLUSTER
                || block == Blocks.LARGE_AMETHYST_BUD
                || block == Blocks.MEDIUM_AMETHYST_BUD
                || block == Blocks.SMALL_AMETHYST_BUD;
    }

    private static int mapColorOrWhite(BlockState state, ServerLevel level, BlockPos pos) {
        MapColor mapColor = state.getMapColor(level, pos);
        if (mapColor == null || mapColor == MapColor.NONE) {
            return 0xFFFFFF;
        }
        int rgb = mapColor.col;
        if (rgb == 0) {
            return 0xFFFFFF;
        }
        return rgb & 0xFFFFFF;
    }
}
