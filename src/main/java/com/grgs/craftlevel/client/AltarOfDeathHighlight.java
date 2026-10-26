package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.block.AltarOfDeathBlock;
import com.grgs.craftlevel.block.ArenaPortalBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sem outline de seleção em multiblocos com VoxelShape composta
 * (arestas internas / duas caixas). Cancela o highlight vanilla.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AltarOfDeathHighlight {

    private AltarOfDeathHighlight() {}

    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockHitResult hit = event.getTarget();
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof AltarOfDeathBlock || block instanceof ArenaPortalBlock) {
            event.setCanceled(true);
        }
    }
}
