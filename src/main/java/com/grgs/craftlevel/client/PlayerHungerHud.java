package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Barra de fome do jogador com texturas (empty / hg_full / hg_bar).
 * Controlada por client.json → hud.hunger_bar.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class PlayerHungerHud {

    private static final ResourceLocation HG_BAR = new ResourceLocation(CraftLevel.MODID, "textures/gui/hg_bar.png");
    private static final ResourceLocation EMPTY = new ResourceLocation(CraftLevel.MODID, "textures/gui/empty.png");
    private static final ResourceLocation HG_FULL = new ResourceLocation(CraftLevel.MODID, "textures/gui/hg_full.png");

    private static final float MAX_FOOD = 20f;
    private static final int BASE_SATURATION_GAP = 3;
    private static final int SATURATION_COLOR = 0xFFFFD700;

    private PlayerHungerHud() {}

    @SubscribeEvent
    public static void onHideVanillaFood(RenderGuiOverlayEvent.Pre event) {
        if (!ClientConfig.hudHungerBarEnabled) {
            return;
        }
        if (VanillaGuiOverlay.FOOD_LEVEL.id().equals(event.getOverlay().id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!ClientConfig.hudHungerBarEnabled) {
            return;
        }
        if (!VanillaGuiOverlay.CROSSHAIR.id().equals(event.getOverlay().id())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.options.renderDebug || player.isSpectator()) {
            return;
        }

        float scale = ClientConfig.hudHungerBarScale;
        int[] barSize = HudBarRenderer.barDimensions(scale);
        int renderWidth = barSize[0];
        int renderHeight = barSize[1];

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int[] anchor = ClientHudLayout.anchorCenter(
                ClientConfig.hudHungerBarAnchor,
                screenWidth,
                screenHeight,
                renderWidth,
                renderHeight
        );
        int x = anchor[0] - (renderWidth / 2) + ClientConfig.hudHungerBarX;
        int y = anchor[1] - (renderHeight / 2) + ClientConfig.hudHungerBarY;

        float progress = Math.min(1f, player.getFoodData().getFoodLevel() / MAX_FOOD);
        float saturation = player.getFoodData().getSaturationLevel();

        GuiGraphics graphics = event.getGuiGraphics();
        HudBarRenderer.draw(graphics, EMPTY, HG_FULL, HG_BAR, x, y, scale, progress);

        if (saturation > 0) {
            float fontScale = ClientConfig.hudHungerBarFontScale;
            int saturationGap = Math.max(1, Math.round(BASE_SATURATION_GAP * scale));
            HudTextHelper.drawRightOfBar(
                    graphics,
                    mc.font,
                    HudTextHelper.immortalText(HudTextHelper.formatSaturation(saturation)),
                    x, y, renderWidth, renderHeight,
                    saturationGap,
                    fontScale,
                    SATURATION_COLOR,
                    true
            );
        }
    }
}
