package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.lwjgl.opengl.GL11;

/**
 * Clone congelado do mob para desenhar busto no card.
 * Mob em {@link #MOB_RENDER_SIZE}px (circular), moldura em {@link #FRAME_SIZE}px; escala do HUD por último.
 */
@OnlyIn(Dist.CLIENT)
final class MobPortraitCapturer {

    static final int FRAME_SIZE = 292;
    static final int MOB_RENDER_SIZE = 204;
    private static final int MOB_OFFSET = (FRAME_SIZE - MOB_RENDER_SIZE) / 2;
    private static final int CIRCLE_CLIP_PADDING = 4;
    private static final int BUST_SCALE = 30;
    private static final float FOCUS_HEIGHT_RATIO = 2f / 3f;
    private static final int BACKGROUND_COLOR = 0xFF1A1A1A;

    private static final ResourceLocation FRAME_TEXTURE =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/mob_focus_frame.png");

    private static final Set<LivingEntity> PORTRAIT_PREVIEWS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    @Nullable
    private final LivingEntity preview;
    private final float entityHeight;

    private MobPortraitCapturer(@Nullable LivingEntity preview, float entityHeight) {
        this.preview = preview;
        this.entityHeight = entityHeight;
    }

    static boolean isPortraitPreview(LivingEntity entity) {
        return PORTRAIT_PREVIEWS.contains(entity);
    }

    @Nullable
    static MobPortraitCapturer capture(LivingEntity source) {
        var level = source.level();
        if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) {
            return null;
        }

        LivingEntity preview = createFrozenPreview(source, clientLevel);
        return preview == null ? null : new MobPortraitCapturer(preview, source.getBbHeight());
    }

    static int displaySize(float hudScale) {
        return Math.round(FRAME_SIZE * hudScale);
    }

    static void renderEmptyPortrait(GuiGraphics graphics, int x, int y, float hudScale) {
        drawScaledPortrait(graphics, x, y, hudScale, null);
    }

    void renderPortrait(GuiGraphics graphics, int x, int y, float hudScale) {
        drawScaledPortrait(graphics, x, y, hudScale, this);
    }

    private static void drawScaledPortrait(
            GuiGraphics graphics,
            int x,
            int y,
            float hudScale,
            @Nullable MobPortraitCapturer capturer
    ) {
        int clipLeft = x + Math.round((MOB_OFFSET - CIRCLE_CLIP_PADDING) * hudScale);
        int clipTop = y + Math.round((MOB_OFFSET - CIRCLE_CLIP_PADDING) * hudScale);
        int clipRight = x + Math.round((MOB_OFFSET + MOB_RENDER_SIZE + CIRCLE_CLIP_PADDING) * hudScale);
        int clipBottom = y + Math.round((MOB_OFFSET + MOB_RENDER_SIZE + CIRCLE_CLIP_PADDING) * hudScale);

        graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(hudScale, hudScale, 1f);
        beginCircularStencil(graphics);
        if (capturer != null) {
            capturer.renderMobLayer(graphics);
        } else {
            renderPlaceholderMob(graphics);
        }
        endCircularStencil();
        graphics.pose().popPose();

        graphics.disableScissor();

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(hudScale, hudScale, 1f);
        blitFrame(graphics);
        graphics.pose().popPose();
    }

    private void renderMobLayer(GuiGraphics graphics) {
        graphics.fill(
                MOB_OFFSET - CIRCLE_CLIP_PADDING,
                MOB_OFFSET - CIRCLE_CLIP_PADDING,
                MOB_OFFSET + MOB_RENDER_SIZE + CIRCLE_CLIP_PADDING,
                MOB_OFFSET + MOB_RENDER_SIZE + CIRCLE_CLIP_PADDING,
                BACKGROUND_COLOR
        );

        float sizeRatio = MOB_RENDER_SIZE / 52f;
        float focusHeight = entityHeight * FOCUS_HEIGHT_RATIO;
        int bustScale = Math.max(
                12,
                Math.min(
                        Math.round(BUST_SCALE * sizeRatio),
                        Math.round((MOB_RENDER_SIZE * 0.86f) / Math.max(entityHeight, 0.35f))
                )
        );

        float scaledFocus = focusHeight * bustScale;
        int portraitCenterY = MOB_OFFSET + MOB_RENDER_SIZE / 2;
        int centerY = portraitCenterY + Math.round(scaledFocus);
        int centerX = MOB_OFFSET + MOB_RENDER_SIZE / 2;

        Lighting.setupForEntityInInventory();
        renderPortraitEntity(
                graphics,
                centerX,
                centerY,
                bustScale,
                ClientConfig.hudMobFocusMobYaw,
                ClientConfig.hudMobFocusHeadYaw,
                ClientConfig.hudMobFocusCameraPitch,
                preview
        );

    }

    private static void renderPlaceholderMob(GuiGraphics graphics) {
        graphics.fill(
                MOB_OFFSET - CIRCLE_CLIP_PADDING,
                MOB_OFFSET - CIRCLE_CLIP_PADDING,
                MOB_OFFSET + MOB_RENDER_SIZE + CIRCLE_CLIP_PADDING,
                MOB_OFFSET + MOB_RENDER_SIZE + CIRCLE_CLIP_PADDING,
                0xFF303030
        );
    }

    private static void beginCircularStencil(GuiGraphics graphics) {
        Minecraft.getInstance().getMainRenderTarget().enableStencil();

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilMask(0xFF);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(false, false, false, false);
        drawCircleStencil(graphics);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        GL11.glStencilMask(0x00);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    private static void drawCircleStencil(GuiGraphics graphics) {
        float radius = MOB_RENDER_SIZE / 2f + CIRCLE_CLIP_PADDING;
        float center = MOB_OFFSET + MOB_RENDER_SIZE / 2f;
        int diameter = Math.round(radius * 2f);
        int top = Math.round(center - radius);
        for (int row = 0; row < diameter; row++) {
            float dy = row + 0.5f - radius;
            int halfWidth = Math.round((float) Math.sqrt(Math.max(0.0f, radius * radius - dy * dy)));
            int y = top + row;
            int left = Math.round(center - halfWidth);
            int right = Math.round(center + halfWidth);
            graphics.fill(left, y, right, y + 1, 0xFFFFFFFF);
        }
    }

    private static void endCircularStencil() {
        GL11.glStencilMask(0xFF);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static void blitFrame(GuiGraphics graphics) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(
                FRAME_TEXTURE,
                0,
                0,
                FRAME_SIZE,
                FRAME_SIZE,
                0,
                0,
                FRAME_SIZE,
                FRAME_SIZE,
                FRAME_SIZE,
                FRAME_SIZE
        );
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    void release() {
        if (preview != null) {
            PORTRAIT_PREVIEWS.remove(preview);
            preview.discard();
        }
    }

    /**
     * Mesma lógica de {@link InventoryScreen#renderEntityInInventoryFollowsAngle},
     * mas com rotação independente da cabeça (head_yaw em graus sobre yRot).
     */
    private static void renderPortraitEntity(
            GuiGraphics graphics,
            int x,
            int y,
            int scale,
            float mobYaw,
            float headYaw,
            float cameraPitch,
            LivingEntity entity
    ) {
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX((float) Math.toRadians(cameraPitch * 20.0f));
        pose.mul(camera);

        float savedBody = entity.yBodyRot;
        float savedYRot = entity.getYRot();
        float savedXRot = entity.getXRot();
        float savedHead = entity.yHeadRot;
        float savedHeadO = entity.yHeadRotO;

        entity.yBodyRot = 180.0f + mobYaw * 20.0f;
        entity.setYRot(180.0f + mobYaw * 40.0f);
        entity.setXRot(-cameraPitch * 20.0f);
        entity.yHeadRot = entity.getYRot() + headYaw;
        entity.yHeadRotO = entity.yHeadRot;

        InventoryScreen.renderEntityInInventory(graphics, x, y, scale, pose, camera, entity);

        entity.yBodyRot = savedBody;
        entity.setYRot(savedYRot);
        entity.setXRot(savedXRot);
        entity.yHeadRot = savedHead;
        entity.yHeadRotO = savedHeadO;
    }

    private static LivingEntity createFrozenPreview(
            LivingEntity source,
            net.minecraft.client.multiplayer.ClientLevel level
    ) {
        LivingEntity preview = (LivingEntity) source.getType().create(level);
        if (preview == null) {
            return null;
        }

        CompoundTag tag = new CompoundTag();
        source.saveWithoutId(tag);
        preview.load(tag);

        preview.setYRot(0.0f);
        preview.setYHeadRot(0.0f);
        preview.setXRot(0.0f);
        preview.yBodyRot = 0.0f;
        preview.yBodyRotO = 0.0f;
        preview.yHeadRotO = 0.0f;
        preview.tickCount = 0;
        preview.setDeltaMovement(0.0, 0.0, 0.0);
        preview.setCustomNameVisible(false);

        PORTRAIT_PREVIEWS.add(preview);
        return preview;
    }
}
