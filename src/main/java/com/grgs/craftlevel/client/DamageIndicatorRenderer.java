package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Renderiza números de dano flutuantes no mundo 3D quando o jogador ataca. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public class DamageIndicatorRenderer {

    private static class Indicator {
        double x, y, z;
        float damage;
        long startTime;
    }

    private static final List<Indicator> indicators = new ArrayList<>();
    private static final long DURATION_MS = 800; // tempo de vida do texto

    /** Chamado pelo DamageIndicatorPacket ao receber dano do servidor. */
    public static void addIndicator(double x, double y, double z, float damage) {
        Indicator ind = new Indicator();
        ind.x = x;
        ind.y = y + 0.15;
        ind.z = z;
        ind.damage = damage;
        ind.startTime = System.currentTimeMillis();
        indicators.add(ind);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        indicators.removeIf(ind -> (now - ind.startTime) > DURATION_MS);

        PoseStack poseStack = event.getPoseStack();
        var camera = mc.gameRenderer.getMainCamera().getPosition();

        for (Indicator ind : indicators) {
            float progress = (now - ind.startTime) / (float) DURATION_MS;
            double floatUp = progress * 0.7; // sobe enquanto desaparece

            poseStack.pushPose();
            poseStack.translate(
                    ind.x - camera.x,
                    ind.y - camera.y + floatUp,
                    ind.z - camera.z
            );
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(-0.025f, -0.025f, 0.025f);

            String text = String.valueOf(Math.round(ind.damage));
            int color = 0xFFFFFF;

            Font font = mc.font;
            int textWidth = font.width(text);

            font.drawInBatch(
                    text,
                    -textWidth / 2f, 0,
                    color,
                    false,
                    poseStack.last().pose(),
                    mc.renderBuffers().bufferSource(),
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880
            );

            poseStack.popPose();
        }
    }
}
