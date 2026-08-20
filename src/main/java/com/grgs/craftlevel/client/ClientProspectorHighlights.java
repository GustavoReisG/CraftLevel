package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.skill.ProspectorSkill;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

/** Outline colorido dos minérios do Prospector (visível através de paredes). */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class ClientProspectorHighlights {

    private static long expireAtGameTime;
    private static List<ProspectorSkill.Highlight> highlights = List.of();

    private ClientProspectorHighlights() {}

    public static void apply(long expireAt, List<ProspectorSkill.Highlight> list) {
        expireAtGameTime = expireAt;
        highlights = list == null ? List.of() : List.copyOf(list);
    }

    public static void clear() {
        expireAtGameTime = 0L;
        highlights = List.of();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || highlights.isEmpty()) {
            return;
        }
        long now = mc.level.getGameTime();
        if (now >= expireAtGameTime) {
            clear();
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0F);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (ProspectorSkill.Highlight h : highlights) {
            drawBox(buffer, matrix, h.pos(), h.rgb());
        }

        Tesselator.getInstance().end();
        RenderSystem.lineWidth(1.0F);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        poseStack.popPose();
    }

    private static void drawBox(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float a = 0.95f;

        float x0 = pos.getX();
        float y0 = pos.getY();
        float z0 = pos.getZ();
        float x1 = x0 + 1.0f;
        float y1 = y0 + 1.0f;
        float z1 = z0 + 1.0f;

        // Bottom
        line(buffer, matrix, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y0, z0, r, g, b, a);
        // Top
        line(buffer, matrix, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // Vertical
        line(buffer, matrix, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void line(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float r, float g, float b, float a
    ) {
        buffer.vertex(matrix, x0, y0, z0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
    }
}
