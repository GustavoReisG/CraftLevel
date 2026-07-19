package com.grgs.craftlevel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Lápide que guarda itens perdidos na morte (souls == 0). */
public class PlayerGravestoneBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(0, 0, 5, 16, 2, 11),
            Block.box(1, 0, 7, 15, 16, 9),
            Block.box(2, 4, 6, 14, 8, 10)
    );
    private static final VoxelShape SHAPE_SOUTH = rotateShapeY(SHAPE_NORTH, 180);
    private static final VoxelShape SHAPE_WEST = rotateShapeY(SHAPE_NORTH, 270);
    private static final VoxelShape SHAPE_EAST = rotateShapeY(SHAPE_NORTH, 90);

    public PlayerGravestoneBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(Blocks.STONE.defaultDestroyTime(), Blocks.STONE.getExplosionResistance())
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlayerGravestoneBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer != null) {
            level.setBlock(pos, state.setValue(FACING, placer.getDirection().getOpposite()), 3);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        boolean silk = tool != null
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0;

        if (be instanceof PlayerGravestoneBlockEntity grave) {
            if (silk) {
                // Silk Touch: dropa lápide vazia; itens guardados são destruídos.
                grave.clearStoredItems();
                drops.add(new ItemStack(this));
            } else {
                drops.addAll(grave.takeStoredItems());
            }
        } else if (silk) {
            drops.add(new ItemStack(this));
        }
        return drops;
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
}
