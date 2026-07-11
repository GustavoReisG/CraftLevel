package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.MobLevelProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderNameTagEvent;
import com.grgs.craftlevel.network.RequestMobLevelPacket;
import com.grgs.craftlevel.network.PacketHandler;

/**
 * Renderiza barra de HP e level acima dos mobs.
 * Cor do level indica dificuldade relativa ao jogador (verde/branco/vermelho).
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public class MobHUD {

    private static final float BAR_WIDTH    = 1.0f;
    private static final float BAR_HEIGHT   = 0.08f;
    private static final float BAR_Y_OFFSET = 0.3f;
    private static final float TEXT_SCALE   = 0.025f;

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!mob.getPassengers().isEmpty()) {
            return;
        }

        mob.getCapability(MobLevelProvider.MOB_LEVEL).ifPresent(mobCap -> {
            int mobLevel = mobCap.getLevel();
            int playerLevel = CraftLevelHUD.cachedLevel;

            // Level ainda não sincronizado — pede ao servidor
            if (mobLevel <= 0) {
                PacketHandler.CHANNEL.sendToServer(new RequestMobLevelPacket(mob.getId()));
                return; // espera o próximo frame com o dado sincronizado
            }

            // Verde = fácil, branco = similar, vermelho = perigoso
            int levelColor;
            int diff = mobLevel - playerLevel;
            if (diff <= -5) {
                levelColor = 0x00FF00;
            } else if (diff >= 5) {
                levelColor = 0xFF0000;
            } else {
                levelColor = 0xFFFFFF;
            }

            float health    = mob.getHealth();
            float maxHealth = mob.getMaxHealth();
            float progress  = health / maxHealth;

            float yOffset = event.getContent() != null ? 0.3f : 0.0f;

            var poseStack = event.getPoseStack();
            poseStack.pushPose();

            poseStack.translate(0, mob.getBbHeight() + yOffset + 0.3f, 0);
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(-1.0f, -1.0f, 1.0f);

            RenderSystem.enableDepthTest();

            // Fundo cinza
            drawRect(poseStack, -BAR_WIDTH / 2, -BAR_HEIGHT / 2, BAR_WIDTH / 2, BAR_HEIGHT / 2, 0x80000000);

            // Barra de HP — cor muda conforme % de vida restante
            int barColor;
            if (progress > 0.5f) {
                barColor = 0xFF70AD47;
            } else if (progress > 0.25f) {
                barColor = 0xFFFFD966;
            } else {
                barColor = 0xFFFF7070;
            }
            drawRect(poseStack, -BAR_WIDTH / 2, -BAR_HEIGHT / 2, -BAR_WIDTH / 2 + BAR_WIDTH * progress, BAR_HEIGHT / 2, barColor);

            // Texto do nível com fonte customizada do mod
            Component levelComponent = Component.literal("Lv." + mobLevel)
                    .withStyle(s -> s.withFont(new ResourceLocation("craftlevel", "immortal")));

            poseStack.pushPose();
            poseStack.scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);

            float textX = -(mc.font.width(levelComponent) / 2.0f);
            float textY = -(BAR_HEIGHT / 2 / TEXT_SCALE) - mc.font.lineHeight - 2;

            mc.font.drawInBatch(
                    levelComponent,
                    textX, textY,
                    levelColor,
                    false,
                    poseStack.last().pose(),
                    event.getMultiBufferSource(),
                    Font.DisplayMode.NORMAL,
                    0,
                    15728880
            );

            poseStack.popPose();
            poseStack.popPose();
        });
    }

    /** Desenha um retângulo colorido em coordenadas locais do name tag. */
    private static void drawRect(PoseStack poseStack, float x1, float y1, float x2, float y2, int color) {
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        var matrix = poseStack.last().pose();
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8  & 0xFF) / 255f;
        float b = (color       & 0xFF) / 255f;

        buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y2, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y1, 0).color(r, g, b, a).endVertex();

        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }
}
