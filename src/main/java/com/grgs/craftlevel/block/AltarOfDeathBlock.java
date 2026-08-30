package com.grgs.craftlevel.block;

import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.config.GloryKillsParser;
import com.grgs.craftlevel.glory.GloryKillsService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Altar of Death — estrutura 3x3x2.
 * O bloco mestre (índice 4, centro do andar inferior) renderiza o modelo;
 * as demais partes são invisíveis e ocupam o volume.
 */
public class AltarOfDeathBlock extends BaseEntityBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 0–17: dy*9 + localZ*3 + localX; mestre = 4 (centro, y=0). */
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, 17);

    public static final int MASTER_INDEX = 4;
    private static final int SIZE_XZ = 3;
    private static final int SIZE_Y = 2;
    private static final int PART_COUNT = SIZE_XZ * SIZE_XZ * SIZE_Y;

    /**
     * Hitbox simplificada da base (piso), fatiada por parte do multibloco.
     * Cada linha: {minX, minY, minZ, maxX, maxY, maxZ} em pixels (16 = 1 bloco).
     */
    private static final double[][] MODEL_BOXES = {
            {16.0D, 0.0D, -16.0D, 32.0D, 3.0D, 0.0D},
            {0.0D, 0.0D, -12.0D, 16.0D, 2.0D, 0.0D},
            {-16.0D, 0.0D, -16.0D, 0.0D, 3.0D, 0.0D},
            {16.0D, 0.0D, 0.0D, 28.0D, 2.0D, 16.0D},
            {0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D},
            {-12.0D, 0.0D, 0.0D, 0.0D, 2.0D, 16.0D},
            {16.0D, 0.0D, 16.0D, 32.0D, 3.0D, 32.0D},
            {0.0D, 0.0D, 16.0D, 16.0D, 2.0D, 28.0D},
            {-16.0D, 0.0D, 16.0D, 0.0D, 3.0D, 32.0D},
            {16.0D, 3.0D, -12.0D, 28.0D, 6.0D, 0.0D},
            {-12.0D, 3.0D, -12.0D, 0.0D, 6.0D, 0.0D},
            {16.0D, 3.0D, 16.0D, 28.0D, 6.0D, 28.0D},
            {-12.0D, 3.0D, 16.0D, 0.0D, 6.0D, 28.0D},
            {16.0D, 6.0D, -6.0D, 22.0D, 16.0D, 0.0D},
            {-6.0D, 6.0D, -6.0D, 0.0D, 16.0D, 0.0D},
            {16.0D, 6.0D, 16.0D, 22.0D, 16.0D, 22.0D},
            {-6.0D, 6.0D, 16.0D, 0.0D, 16.0D, 22.0D},
            {16.0D, 16.0D, -7.0D, 23.0D, 19.0D, 0.0D},
            {0.0D, 16.0D, -4.0D, 16.0D, 19.0D, 0.0D},
            {-7.0D, 16.0D, -7.0D, 0.0D, 19.0D, 0.0D},
            {16.0D, 16.0D, 0.0D, 20.0D, 19.0D, 16.0D},
            {-4.0D, 16.0D, 0.0D, 0.0D, 19.0D, 16.0D},
            {16.0D, 16.0D, 16.0D, 23.0D, 19.0D, 23.0D},
            {0.0D, 16.0D, 16.0D, 16.0D, 19.0D, 20.0D},
            {-7.0D, 16.0D, 16.0D, 0.0D, 19.0D, 23.0D},
            {-8.0D, 19.0D, -8.0D, 24.0D, 22.0D, 24.0D},
            {23.0D, 5.0D, -11.0D, 27.0D, 8.0D, -7.0D},
            {24.0D, 8.0D, -10.0D, 26.0D, 14.0D, -8.0D},
            {-11.0D, 6.0D, -11.0D, -7.0D, 8.0D, -7.0D},
            {-10.0D, 8.0D, -10.0D, -8.0D, 14.0D, -8.0D},
            {23.0D, 6.0D, 23.0D, 27.0D, 8.0D, 27.0D},
            {24.0D, 8.0D, 24.0D, 26.0D, 14.0D, 26.0D},
            {-11.0D, 6.0D, 23.0D, -7.0D, 8.0D, 27.0D},
            {-10.0D, 8.0D, 24.0D, -8.0D, 14.0D, 26.0D},
            {17.0D, 22.0D, -5.0D, 21.0D, 24.0D, -1.0D},
            {18.0D, 24.0D, -4.0D, 20.0D, 30.0D, -2.0D},
            {-5.0D, 22.0D, -5.0D, -1.0D, 24.0D, -1.0D},
            {-4.0D, 24.0D, -4.0D, -2.0D, 30.0D, -2.0D},
            {17.0D, 22.0D, 17.0D, 21.0D, 24.0D, 21.0D},
            {18.0D, 24.0D, 18.0D, 20.0D, 30.0D, 20.0D},
            {-5.0D, 22.0D, 17.0D, -1.0D, 24.0D, 21.0D},
            {-4.0D, 24.0D, 18.0D, -2.0D, 30.0D, 20.0D}
    };

    /** Centro da chama de cada vela (modelo north, coords de bloco relativas ao mestre). */
    private static final double[][] CANDLE_FLAME_OFFSETS = {
            {1.1875D, 1.9062D, -0.1875D},
            {-0.1875D, 1.9062D, 1.1875D},
            {1.1875D, 1.9062D, 1.1875D},
            {-0.1875D, 1.9062D, -0.1875D},
            {-0.5625D, 0.9062D, 1.5625D},
            {1.5625D, 0.9062D, 1.5625D},
            {-0.5625D, 0.9062D, -0.5625D},
            {1.5625D, 0.9062D, -0.5625D},
    };

    /** +1 px acima do centro da chama no modelo (16 px = 1 bloco). */
    private static final double CANDLE_FLAME_Y_NUDGE = 1.0D / 16.0D;

    private static final int CRYING_OBSIDIAN_BREAK_ID =
            Block.getId(Blocks.CRYING_OBSIDIAN.defaultBlockState());

    private static final VoxelShape SOLID_NORTH = buildSolidNorth();
    private static final VoxelShape SOLID_SOUTH = rotateShapeY(SOLID_NORTH, 180);
    private static final VoxelShape SOLID_WEST = rotateShapeY(SOLID_NORTH, 270);
    private static final VoxelShape SOLID_EAST = rotateShapeY(SOLID_NORTH, 90);
    private static final VoxelShape[] SHAPES_NORTH = buildPartShapes(SOLID_NORTH, Direction.NORTH);
    private static final VoxelShape[] SHAPES_SOUTH = buildPartShapes(SOLID_SOUTH, Direction.SOUTH);
    private static final VoxelShape[] SHAPES_WEST = buildPartShapes(SOLID_WEST, Direction.WEST);
    private static final VoxelShape[] SHAPES_EAST = buildPartShapes(SOLID_EAST, Direction.EAST);

    /** Evita cascata ao remover as 18 partes da estrutura. */
    private static boolean clearingStructure;

    public AltarOfDeathBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(3.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(INDEX) == MASTER_INDEX ? 12 : 0)
                .noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(LIT, true)
                .setValue(FACING, Direction.NORTH)
                .setValue(INDEX, MASTER_INDEX));
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addDestroyEffects(
                    BlockState state,
                    Level level,
                    BlockPos pos,
                    net.minecraft.client.particle.ParticleEngine manager
            ) {
                manager.destroy(pos, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                return true;
            }
        });
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return isMaster(state) ? 12 : 0;
    }

    private static VoxelShape buildSolidNorth() {
        VoxelShape shape = Shapes.empty();
        for (double[] box : MODEL_BOXES) {
            double minX = Math.min(box[0], box[3]);
            double minY = Math.min(box[1], box[4]);
            double minZ = Math.min(box[2], box[5]);
            double maxX = Math.max(box[0], box[3]);
            double maxY = Math.max(box[1], box[4]);
            double maxZ = Math.max(box[2], box[5]);
            if (minX == maxX || minY == maxY || minZ == maxZ) {
                continue;
            }
            shape = Shapes.or(shape, Block.box(minX, minY, minZ, maxX, maxY, maxZ));
        }
        return shape;
    }

    private static VoxelShape[] buildPartShapes(VoxelShape solid, Direction facing) {
        VoxelShape[] shapes = new VoxelShape[PART_COUNT];
        for (int index = 0; index < PART_COUNT; index++) {
            BlockPos offset = indexToOffset(index, facing);
            VoxelShape moved = solid.move(-offset.getX(), -offset.getY(), -offset.getZ());
            shapes[index] = Shapes.join(moved, Shapes.block(), BooleanOp.AND).optimize();
        }
        return shapes;
    }

    private static VoxelShape shapeFor(BlockState state) {
        int index = state.getValue(INDEX);
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPES_SOUTH[index];
            case WEST -> SHAPES_WEST[index];
            case EAST -> SHAPES_EAST[index];
            default -> SHAPES_NORTH[index];
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING, INDEX);
    }

    public static boolean isMaster(BlockState state) {
        return state.getValue(INDEX) == MASTER_INDEX;
    }

    public static BlockPos getMasterPos(BlockPos pos, BlockState state) {
        if (isMaster(state)) {
            return pos;
        }
        return pos.subtract(indexToOffset(state.getValue(INDEX), state.getValue(FACING)));
    }

    /** Offset do mestre até a parte com o índice dado, já rotacionado pelo facing. */
    public static BlockPos indexToOffset(int index, Direction facing) {
        int localX = index % SIZE_XZ;
        int localZ = (index / SIZE_XZ) % SIZE_XZ;
        int localY = index / (SIZE_XZ * SIZE_XZ);
        return rotateOffset(localX - 1, localY, localZ - 1, facing);
    }

    private static BlockPos rotateOffset(int dx, int dy, int dz, Direction facing) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-dx, dy, -dz);
            case WEST -> new BlockPos(dz, dy, -dx);
            case EAST -> new BlockPos(-dz, dy, dx);
            default -> new BlockPos(dx, dy, dz);
        };
    }

    private static boolean canPlaceStructure(Level level, BlockPos masterPos, Direction facing, @Nullable Player player) {
        for (int index = 0; index < PART_COUNT; index++) {
            BlockPos partPos = masterPos.offset(indexToOffset(index, facing));
            BlockState existing = level.getBlockState(partPos);
            if (!existing.canBeReplaced()) {
                return false;
            }
            if (player != null && !level.mayInteract(player, partPos)) {
                return false;
            }
        }
        return true;
    }

    private void placeOtherParts(Level level, BlockPos masterPos, Direction facing) {
        for (int index = 0; index < PART_COUNT; index++) {
            if (index == MASTER_INDEX) {
                continue;
            }
            BlockPos partPos = masterPos.offset(indexToOffset(index, facing));
            BlockState partState = defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(LIT, true)
                    .setValue(INDEX, index);
            level.setBlock(partPos, partState, Block.UPDATE_ALL);
        }
    }

    private void clearOtherParts(
            Level level,
            BlockPos masterPos,
            Direction facing,
            @Nullable BlockPos keepPos,
            @Nullable Player player
    ) {
        if (clearingStructure) {
            return;
        }
        clearingStructure = true;
        try {
            for (int index = 0; index < PART_COUNT; index++) {
                BlockPos partPos = masterPos.offset(indexToOffset(index, facing));
                if (keepPos != null && keepPos.equals(partPos)) {
                    continue;
                }
                BlockState partState = level.getBlockState(partPos);
                if (!partState.is(this)) {
                    continue;
                }
                if (player != null) {
                    level.levelEvent(player, 2001, partPos, CRYING_OBSIDIAN_BREAK_ID);
                } else {
                    level.levelEvent(2001, partPos, CRYING_OBSIDIAN_BREAK_ID);
                }
                level.setBlock(partPos, Blocks.AIR.defaultBlockState(), 35);
            }
        } finally {
            clearingStructure = false;
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos masterPos = context.getClickedPos();
        if (!canPlaceStructure(context.getLevel(), masterPos, facing, context.getPlayer())) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(LIT, true)
                .setValue(INDEX, MASTER_INDEX);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide() && isMaster(state)) {
            placeOtherParts(level, pos, state.getValue(FACING));
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !clearingStructure) {
            BlockPos masterPos = getMasterPos(pos, state);
            // Remove as outras partes; o bloco clicado dropa pelo fluxo normal
            clearOtherParts(level, masterPos, state.getValue(FACING), pos, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !clearingStructure && !level.isClientSide()) {
            // Explosões / remoção sem playerWillDestroy: limpa o restante
            clearOtherParts(level, getMasterPos(pos, state), state.getValue(FACING), pos, null);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (clearingStructure) {
            return Collections.emptyList();
        }
        return super.getDrops(state, params);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.ALTAR_OF_DEATH.get());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return isMaster(state) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isMaster(state) ? new AltarOfDeathBlockEntity(pos, state) : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    private static double[] rotateModelPoint(double x, double y, double z, Direction facing) {
        int turns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        for (int i = 0; i < turns; i++) {
            double nx = 1.0D - z;
            double nz = x;
            x = nx;
            z = nz;
        }
        return new double[]{x, y, z};
    }

    private static VoxelShape rotateShapeY(VoxelShape shape, int degrees) {
        VoxelShape[] buffer = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double x1 = minX;
            double z1 = minZ;
            double x2 = maxX;
            double z2 = maxZ;
            for (int i = 0; i < degrees / 90; i++) {
                double nx1 = 1.0 - z2;
                double nz1 = x1;
                double nx2 = 1.0 - z1;
                double nz2 = x2;
                x1 = nx1;
                z1 = nz1;
                x2 = nx2;
                z2 = nz2;
            }
            buffer[0] = Shapes.or(buffer[0], Shapes.box(
                    Math.min(x1, x2), minY, Math.min(z1, z2),
                    Math.max(x1, x2), maxY, Math.max(z1, z2)
            ));
        });
        return buffer[0];
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        BlockPos masterPos = getMasterPos(pos, state);
        BlockState masterState = level.getBlockState(masterPos);
        if (!masterState.is(this)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(Items.AMETHYST_SHARD)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return tryExchangeAmethyst(player, stack, level, masterPos);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult tryExchangeAmethyst(Player player, ItemStack amethyst, Level level, BlockPos pos) {
        if (level.isDay()) {
            player.displayClientMessage(
                    Component.translatable("block.craftlevel.altar_of_death.need_night"),
                    true
            );
            return InteractionResult.FAIL;
        }

        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY).map(glory -> {
            int required = GloryKillsParser.killsPerShard;
            int gloryKills = glory.getGloryKills();
            int maxByKills = gloryKills / required;
            if (maxByKills <= 0) {
                player.displayClientMessage(
                        Component.translatable(
                                "block.craftlevel.altar_of_death.need_kills",
                                required,
                                gloryKills
                        ),
                        true
                );
                return InteractionResult.FAIL;
            }

            int conversions = player.isShiftKeyDown()
                    ? Math.min(amethyst.getCount(), maxByKills)
                    : 1;

            glory.setGloryKills(gloryKills - required * conversions);
            glory.setSkillPoints(glory.getSkillPoints() + conversions);
            if (!player.getAbilities().instabuild) {
                amethyst.shrink(conversions);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                GloryKillsService.sync(serverPlayer);
                Component message = conversions == 1
                        ? Component.translatable(
                                "block.craftlevel.altar_of_death.skill_point_gained",
                                glory.getSkillPoints()
                        )
                        : Component.translatable(
                                "block.craftlevel.altar_of_death.skill_points_gained",
                                conversions,
                                glory.getSkillPoints()
                        );
                serverPlayer.displayClientMessage(message, true);
            }

            AltarOfDeathBlockEntity.applyExchangeDarkness(level, pos);

            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.2F);
            level.playSound(
                    null,
                    pos,
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.BLOCKS,
                    0.8F,
                    1.0F + conversions * 0.05F
            );
            return InteractionResult.SUCCESS;
        }).orElse(InteractionResult.FAIL);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!isMaster(state)) {
            return;
        }

        Direction facing = state.getValue(FACING);
        for (double[] offset : CANDLE_FLAME_OFFSETS) {
            double[] local = rotateModelPoint(offset[0], offset[1], offset[2], facing);
            double x = pos.getX() + local[0];
            double y = pos.getY() + local[1] + CANDLE_FLAME_Y_NUDGE;
            double z = pos.getZ() + local[2];

            if (random.nextFloat() < 0.55F) {
                level.addParticle(
                        ParticleTypes.SMALL_FLAME,
                        x + (random.nextDouble() - 0.5D) * 0.06D,
                        y,
                        z + (random.nextDouble() - 0.5D) * 0.06D,
                        0.0D,
                        0.004D,
                        0.0D
                );
            }
        }
    }
}
