package com.grgs.craftlevel.block;

import com.grgs.craftlevel.arena.ArenaService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Portal da arena — estrutura 1×1×2 no estilo porta:
 * um item coloca/quebra a peça inteira; só a base renderiza o modelo.
 * Contorno de seleção fica oculto no client ({@code AltarOfDeathHighlight}).
 */
public class ArenaPortalBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 0 = base (mestre), 1 = topo. */
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, 1);

    public static final int MASTER_INDEX = 0;
    private static final int PART_COUNT = 2;

    /**
     * Colisão unificada por cubo do modelo (pixels; 16 = 1 bloco), com rotação aplicada.
     * Gerado por {@code tools/install_arena_portal_model.py}.
     */
    private static final double[][] MODEL_BOXES = {
            {3.0000D, 0.0000D, 3.5000D, 13.0000D, 7.0000D, 12.5000D},
            {2.0000D, 0.0000D, 2.0000D, 14.0000D, 3.0000D, 13.0000D},
            {10.0000D, 0.0000D, 10.0000D, 15.0000D, 5.0000D, 15.0000D},
            {0.0000D, 0.0000D, 0.0000D, 5.0000D, 5.0000D, 5.0000D},
            {1.0000D, 0.0000D, 10.0000D, 4.0000D, 5.0000D, 14.0000D},
            {11.0000D, 0.0000D, 1.0000D, 15.0000D, 4.0000D, 5.0000D},
            {4.0000D, 6.0000D, 5.0000D, 12.0000D, 8.0000D, 11.0000D},
            {5.0000D, 0.0000D, 1.0000D, 9.0000D, 4.0000D, 3.0000D},
            {4.0803D, 8.0000D, 4.0803D, 11.9197D, 14.0000D, 11.9197D},
            {5.8000D, 10.6000D, 6.2000D, 10.2000D, 11.3000D, 10.2000D},
            {5.8787D, 7.6005D, 7.5000D, 7.2929D, 9.0147D, 8.5000D},
            {6.5858D, 6.8934D, 7.5000D, 8.0000D, 8.3076D, 8.5000D},
            {7.2929D, 6.1863D, 7.5000D, 8.7071D, 7.6005D, 8.5000D},
            {5.8787D, 9.0147D, 7.5000D, 7.2929D, 10.4289D, 8.5000D},
            {6.5858D, 8.3076D, 7.5000D, 8.0000D, 9.7218D, 8.5000D},
            {7.2929D, 7.6005D, 7.5000D, 8.7071D, 9.0147D, 8.5000D},
            {8.0000D, 6.8934D, 7.5000D, 9.4142D, 8.3076D, 8.5000D},
            {5.8787D, 10.4289D, 7.5000D, 7.2929D, 11.8431D, 8.5000D},
            {6.5858D, 9.7218D, 7.5000D, 8.0000D, 11.1360D, 8.5000D},
            {7.2929D, 9.0147D, 7.5000D, 8.7071D, 10.4289D, 8.5000D},
            {8.0000D, 8.3076D, 7.5000D, 9.4142D, 9.7218D, 8.5000D},
            {8.7071D, 7.6005D, 7.5000D, 10.1213D, 9.0147D, 8.5000D},
            {5.8787D, 11.8431D, 7.5000D, 7.2929D, 13.2574D, 8.5000D},
            {6.5858D, 11.1360D, 7.5000D, 8.0000D, 12.5503D, 8.5000D},
            {7.2929D, 10.4289D, 7.5000D, 8.7071D, 11.8431D, 8.5000D},
            {8.0000D, 9.7218D, 7.5000D, 9.4142D, 11.1360D, 8.5000D},
            {8.7071D, 9.0147D, 7.5000D, 10.1213D, 10.4289D, 8.5000D},
            {5.8787D, 13.2574D, 7.5000D, 7.2929D, 14.6716D, 8.5000D},
            {6.5858D, 12.5503D, 7.5000D, 8.0000D, 13.9645D, 8.5000D},
            {7.2929D, 11.8431D, 7.5000D, 8.7071D, 13.2574D, 8.5000D},
            {8.0000D, 11.1360D, 7.5000D, 9.4142D, 12.5503D, 8.5000D},
            {8.7071D, 10.4289D, 7.5000D, 10.1213D, 11.8431D, 8.5000D},
            {5.8787D, 14.6716D, 7.5000D, 7.2929D, 16.0858D, 8.5000D},
            {6.5858D, 13.9645D, 7.5000D, 8.0000D, 15.3787D, 8.5000D},
            {7.2929D, 13.2574D, 7.5000D, 8.7071D, 14.6716D, 8.5000D},
            {8.0000D, 12.5503D, 7.5000D, 9.4142D, 13.9645D, 8.5000D},
            {8.7071D, 11.8431D, 7.5000D, 10.1213D, 13.2574D, 8.5000D},
            {2.3431D, 19.6213D, 7.5000D, 3.7574D, 21.0355D, 8.5000D},
            {3.0503D, 18.9142D, 7.5000D, 4.4645D, 20.3284D, 8.5000D},
            {5.8787D, 16.0858D, 7.5000D, 7.2929D, 17.5000D, 8.5000D},
            {6.5858D, 15.3787D, 7.5000D, 8.0000D, 16.7929D, 8.5000D},
            {7.2929D, 14.6716D, 7.5000D, 8.7071D, 16.0858D, 8.5000D},
            {8.0000D, 13.9645D, 7.5000D, 9.4142D, 15.3787D, 8.5000D},
            {8.7071D, 13.2574D, 7.5000D, 10.1213D, 14.6716D, 8.5000D},
            {3.0503D, 20.3284D, 7.5000D, 4.4645D, 21.7426D, 8.5000D},
            {3.7574D, 19.6213D, 7.5000D, 5.1716D, 21.0355D, 8.5000D},
            {4.4645D, 18.9142D, 7.5000D, 5.8787D, 20.3284D, 8.5000D},
            {5.8787D, 17.5000D, 7.5000D, 7.2929D, 18.9142D, 8.5000D},
            {6.5858D, 16.7929D, 7.5000D, 8.0000D, 18.2071D, 8.5000D},
            {7.2929D, 16.0858D, 7.5000D, 8.7071D, 17.5000D, 8.5000D},
            {8.0000D, 15.3787D, 7.5000D, 9.4142D, 16.7929D, 8.5000D},
            {8.7071D, 14.6716D, 7.5000D, 10.1213D, 16.0858D, 8.5000D},
            {4.4645D, 20.3284D, 7.5000D, 5.8787D, 21.7426D, 8.5000D},
            {5.1716D, 19.6213D, 7.5000D, 6.5858D, 21.0355D, 8.5000D},
            {5.8787D, 18.9142D, 7.5000D, 7.2929D, 20.3284D, 8.5000D},
            {6.5858D, 18.2071D, 7.5000D, 8.0000D, 19.6213D, 8.5000D},
            {7.2929D, 17.5000D, 7.5000D, 8.7071D, 18.9142D, 8.5000D},
            {8.0000D, 16.7929D, 7.5000D, 9.4142D, 18.2071D, 8.5000D},
            {8.7071D, 16.0858D, 7.5000D, 10.1213D, 17.5000D, 8.5000D},
            {5.1716D, 21.0355D, 7.5000D, 6.5858D, 22.4497D, 8.5000D},
            {5.8787D, 20.3284D, 7.5000D, 7.2929D, 21.7426D, 8.5000D},
            {6.5858D, 19.6213D, 7.5000D, 8.0000D, 21.0355D, 8.5000D},
            {7.2929D, 18.9142D, 7.5000D, 8.7071D, 20.3284D, 8.5000D},
            {8.0000D, 18.2071D, 7.5000D, 9.4142D, 19.6213D, 8.5000D},
            {8.7071D, 17.5000D, 7.5000D, 10.1213D, 18.9142D, 8.5000D},
            {6.5858D, 21.0355D, 7.5000D, 8.0000D, 22.4497D, 8.5000D},
            {7.2929D, 20.3284D, 7.5000D, 8.7071D, 21.7426D, 8.5000D},
            {8.0000D, 19.6213D, 7.5000D, 9.4142D, 21.0355D, 8.5000D},
            {8.7071D, 18.9142D, 7.5000D, 10.1213D, 20.3284D, 8.5000D},
            {6.5858D, 22.4497D, 7.5000D, 8.0000D, 23.8640D, 8.5000D},
            {7.2929D, 21.7426D, 7.5000D, 8.7071D, 23.1569D, 8.5000D},
            {8.0000D, 21.0355D, 7.5000D, 9.4142D, 22.4497D, 8.5000D},
            {8.7071D, 20.3284D, 7.5000D, 10.1213D, 21.7426D, 8.5000D},
            {9.4142D, 19.6213D, 7.5000D, 10.8284D, 21.0355D, 8.5000D},
            {10.1213D, 18.9142D, 7.5000D, 11.5355D, 20.3284D, 8.5000D},
            {6.5858D, 23.8640D, 7.5000D, 8.0000D, 25.2782D, 8.5000D},
            {7.2929D, 23.1569D, 7.5000D, 8.7071D, 24.5711D, 8.5000D},
            {8.0000D, 22.4497D, 7.5000D, 9.4142D, 23.8640D, 8.5000D},
            {9.4142D, 21.0355D, 7.5000D, 10.8284D, 22.4497D, 8.5000D},
            {10.1213D, 20.3284D, 7.5000D, 11.5355D, 21.7426D, 8.5000D},
            {10.8284D, 19.6213D, 7.5000D, 12.2426D, 21.0355D, 8.5000D},
            {11.5355D, 18.9142D, 7.5000D, 12.9497D, 20.3284D, 8.5000D},
            {5.8787D, 25.9853D, 7.5000D, 7.2929D, 27.3995D, 8.5000D},
            {6.5858D, 25.2782D, 7.5000D, 8.0000D, 26.6924D, 8.5000D},
            {7.2929D, 24.5711D, 7.5000D, 8.7071D, 25.9853D, 8.5000D},
            {8.0000D, 23.8640D, 7.5000D, 9.4142D, 25.2782D, 8.5000D},
            {11.5355D, 20.3284D, 7.5000D, 12.9497D, 21.7426D, 8.5000D},
            {12.2426D, 19.6213D, 7.5000D, 13.6569D, 21.0355D, 8.5000D},
            {6.5858D, 26.6924D, 7.5000D, 8.0000D, 28.1066D, 8.5000D},
            {7.2929D, 25.9853D, 7.5000D, 8.7071D, 27.3995D, 8.5000D},
            {8.0000D, 25.2782D, 7.5000D, 9.4142D, 26.6924D, 8.5000D},
            {7.2929D, 27.3995D, 7.5000D, 8.7071D, 28.8137D, 8.5000D},
            {8.0000D, 26.6924D, 7.5000D, 9.4142D, 28.1066D, 8.5000D},
            {8.7071D, 25.9853D, 7.5000D, 10.1213D, 27.3995D, 8.5000D},
    };

    private static final VoxelShape SOLID_NORTH = buildSolidNorth();
    private static final VoxelShape SOLID_SOUTH = rotateShapeY(SOLID_NORTH, 180);
    private static final VoxelShape SOLID_WEST = rotateShapeY(SOLID_NORTH, 270);
    private static final VoxelShape SOLID_EAST = rotateShapeY(SOLID_NORTH, 90);
    private static final VoxelShape[] SHAPES_NORTH = buildPartShapes(SOLID_NORTH);
    private static final VoxelShape[] SHAPES_SOUTH = buildPartShapes(SOLID_SOUTH);
    private static final VoxelShape[] SHAPES_WEST = buildPartShapes(SOLID_WEST);
    private static final VoxelShape[] SHAPES_EAST = buildPartShapes(SOLID_EAST);

    private static boolean clearingStructure;

    public ArenaPortalBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(3.0F, 6.0F) // ardosia: quebra relativamente rápido com a ferramenta certa
                .requiresCorrectToolForDrops()
                .sound(SoundType.DEEPSLATE)
                .noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(INDEX, MASTER_INDEX));
    }

    /** Só netherite (ou tier ≥ netherite) consegue minerar. */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!isNetheriteTierTool(player.getMainHandItem(), state)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    private static boolean isNetheriteTierTool(ItemStack stack, BlockState state) {
        if (stack.isEmpty() || !(stack.getItem() instanceof DiggerItem digger)) {
            return false;
        }
        if (!stack.isCorrectToolForDrops(state)) {
            return false;
        }
        Tier tier = digger.getTier();
        return tier.getLevel() >= Tiers.NETHERITE.getLevel();
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
        return shape.optimize();
    }

    private static VoxelShape[] buildPartShapes(VoxelShape solid) {
        VoxelShape[] shapes = new VoxelShape[PART_COUNT];
        for (int index = 0; index < PART_COUNT; index++) {
            BlockPos offset = indexToOffset(index);
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
        return buffer[0].optimize();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, INDEX);
    }

    public static boolean isMaster(BlockState state) {
        return state.getValue(INDEX) == MASTER_INDEX;
    }

    public static BlockPos getMasterPos(BlockPos pos, BlockState state) {
        if (isMaster(state)) {
            return pos;
        }
        return pos.subtract(indexToOffset(state.getValue(INDEX)));
    }

    public static BlockPos indexToOffset(int index) {
        return new BlockPos(0, index, 0);
    }

    private static boolean canPlaceStructure(Level level, BlockPos masterPos, @Nullable Player player) {
        for (int index = 0; index < PART_COUNT; index++) {
            BlockPos partPos = masterPos.offset(indexToOffset(index));
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
            BlockPos partPos = masterPos.offset(indexToOffset(index));
            level.setBlock(
                    partPos,
                    defaultBlockState().setValue(FACING, facing).setValue(INDEX, index),
                    Block.UPDATE_ALL
            );
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
                BlockPos partPos = masterPos.offset(indexToOffset(index));
                if (keepPos != null && keepPos.equals(partPos)) {
                    continue;
                }
                BlockState partState = level.getBlockState(partPos);
                if (!partState.is(this)) {
                    continue;
                }
                int breakId = Block.getId(Blocks.STONE.defaultBlockState());
                if (player != null) {
                    level.levelEvent(player, 2001, partPos, breakId);
                } else {
                    level.levelEvent(2001, partPos, breakId);
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
        if (!canPlaceStructure(context.getLevel(), masterPos, context.getPlayer())) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing).setValue(INDEX, MASTER_INDEX);
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
            clearOtherParts(level, getMasterPos(pos, state), state.getValue(FACING), pos, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !clearingStructure && !level.isClientSide()) {
            clearOtherParts(level, getMasterPos(pos, state), state.getValue(FACING), pos, null);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // Como a porta: só a peça quebrada pelo jogador pode dropar; exige Silk Touch.
        if (clearingStructure) {
            return Collections.emptyList();
        }
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && !tool.isEmpty() && EnchantmentHelper.hasSilkTouch(tool)) {
            return List.of(new ItemStack(ModBlocks.ARENA_PORTAL.get()));
        }
        return Collections.emptyList();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.ARENA_PORTAL.get());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return isMaster(state) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Mesma shape nas duas metades (porta): hit/colisão contínuos; outline some no client.
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
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
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
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ArenaService.enter(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }
}
