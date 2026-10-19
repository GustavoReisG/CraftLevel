package com.grgs.craftlevel.arena;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;

/** Cola um schema Sponge v2 numa dimensão. */
public final class ArenaSchematic {

    /** A base do schema cola nesta altura. O jogador nasce {@link #PLAYER_Y} acima. */
    public static final int BASE_Y = 62;
    public static final int PLAYER_Y = 2;

    private ArenaSchematic() {}

    public record Size(int width, int height, int length) {}

    public static Size size(String resource) throws Exception {
        CompoundTag root = read(resource);
        int width = root.getShort("Width");
        int height = root.getShort("Height");
        int length = root.getShort("Length");
        if (width <= 0 || height <= 0 || length <= 0) {
            throw new IllegalStateException("Schematic vazio");
        }
        return new Size(width, height, length);
    }

    /**
     * Cola a arena com a base em {@code baseY}. O centro horizontal fica em {@code originX}/{@code originZ}.
     * O retângulo devolvido é o volume inteiro do schema, inclusive o ar.
     */
    public static Bounds paste(ServerLevel level, String resource, int originX, int baseY, int originZ) throws Exception {
        CompoundTag root = read(resource);
        int width = root.getShort("Width");
        int height = root.getShort("Height");
        int length = root.getShort("Length");
        if (width <= 0 || height <= 0 || length <= 0) {
            throw new IllegalStateException("Schematic vazio");
        }

        BlockState[] palette = palette(root);
        int[] blocks = decode(root.getByteArray("BlockData"), width, height, length);
        int centerX = width / 2;
        int centerZ = length / 2;
        int minX = originX - centerX;
        int minY = baseY;
        int minZ = originZ - centerZ;
        int maxX = minX + width - 1;
        int maxY = minY + height - 1;
        int maxZ = minZ + length - 1;
        int placed = 0;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                for (int y = 0; y < height; y++) {
                    BlockState state = palette[blocks[index(x, y, z, width, length)]];
                    if (state == null || state.isAir()) {
                        continue;
                    }
                    int wx = originX + (x - centerX);
                    int wy = baseY + y;
                    int wz = originZ + (z - centerZ);
                    level.setBlock(new BlockPos(wx, wy, wz), state, Block.UPDATE_CLIENTS);
                    placed++;
                }
            }
        }

        if (placed == 0) {
            throw new IllegalStateException("Schematic sem blocos");
        }
        CraftLevel.LOGGER.info("[CraftLevel] Arena colada: {} blocos em {}, {}, {}", placed, originX, baseY, originZ);
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void clear(ServerLevel level, Bounds bounds) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
        for (int y = bounds.maxY(); y >= bounds.minY(); y--) {
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
                    }
                }
            }
        }
    }

    private static CompoundTag read(String resource) throws Exception {
        try (InputStream in = ArenaSchematic.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Schema não encontrado");
            }
            CompoundTag root = NbtIo.readCompressed(in);
            if (root.getInt("Version") != 2) {
                throw new IllegalStateException("Schema precisa ser Sponge v2");
            }
            return root;
        }
    }

    private static BlockState[] palette(CompoundTag root) throws Exception {
        int max = Math.max(1, root.getInt("PaletteMax"));
        BlockState[] states = new BlockState[max];
        CompoundTag palette = root.getCompound("Palette");
        for (String key : palette.getAllKeys()) {
            int id = palette.getInt(key);
            if (id < 0 || id >= states.length) {
                continue;
            }
            states[id] = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), key, false).blockState();
        }
        return states;
    }

    /** Índices varint: {@code x + z * width + y * width * length}. */
    private static int[] decode(byte[] data, int width, int height, int length) {
        int volume = width * height * length;
        int[] blocks = new int[volume];
        int cursor = 0;
        for (int i = 0; i < volume; i++) {
            int value = 0;
            int shift = 0;
            while (true) {
                if (cursor >= data.length) {
                    throw new IllegalStateException("BlockData incompleto");
                }
                int bite = data[cursor++] & 0xFF;
                value |= (bite & 0x7F) << shift;
                if ((bite & 0x80) == 0) {
                    break;
                }
                shift += 7;
            }
            blocks[i] = value;
        }
        return blocks;
    }

    private static int index(int x, int y, int z, int width, int length) {
        return x + z * width + y * width * length;
    }

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}
}
