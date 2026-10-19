package com.grgs.craftlevel.block;

import com.grgs.craftlevel.arena.ArenaService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** Portal da arena. O uso cria uma cópia do schema e leva o jogador até ela. */
public class ArenaPortalBlock extends Block {

    public ArenaPortalBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.GOLD)
                .strength(5.0F, 6.0F)
                .sound(SoundType.ANVIL)
                .noOcclusion());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ArenaService.enter(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }
}
