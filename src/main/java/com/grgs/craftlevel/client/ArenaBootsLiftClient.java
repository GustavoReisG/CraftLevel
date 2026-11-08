package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.item.ArenaBootsLift;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sobe o modelo do player em 1px quando as botas de arena estão equipadas. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ArenaBootsLiftClient {

    private ArenaBootsLiftClient() {}

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!ArenaBootsLift.isWearing(player)) {
            return;
        }

        Pose pose = player.getPose();
        if (pose != Pose.STANDING && pose != Pose.CROUCHING) {
            return;
        }

        event.getPoseStack().translate(0.0D, ArenaBootsLift.HEIGHT, 0.0D);
    }
}
