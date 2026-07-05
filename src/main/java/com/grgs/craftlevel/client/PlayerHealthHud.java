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
 * Barra de HP do jogador com texturas (empty / hp_full / hp_bar).
 * Controlada por client.json → hud.health_bar.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class PlayerHealthHud {

    private static final ResourceLocation HP_BAR = new ResourceLocation(CraftLevel.MODID, "textures/gui/hp_bar.png");
    private static final ResourceLocation EMPTY = new ResourceLocation(CraftLevel.MODID, "textures/gui/empty.png");
    private static final ResourceLocation HP_FULL = new ResourceLocation(CraftLevel.MODID, "textures/gui/hp_full.png");

    private static final int BASE_ABSORPTION_GAP = 3;
    private static final int ABSORPTION_COLOR = 0xFFFFD700;

    private PlayerHealthHud() {}

    @SubscribeEvent
    public static void onHideVanillaHearts(RenderGuiOverlayEvent.Pre event) {
        if (!ClientConfig.hudHealthBarEnabled) {
            return;
        }
        if (VanillaGuiOverlay.PLAYER_HEALTH.id().equals(event.getOverlay().id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!ClientConfig.hudHealthBarEnabled) {
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

        float scale = ClientConfig.hudHealthBarScale;
        int[] barSize = HudBarRenderer.barDimensions(scale);
        int renderWidth = barSize[0];
        int renderHeight = barSize[1];

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int[] anchor = ClientHudLayout.anchorCenter(
                ClientConfig.hudHealthBarAnchor,
                screenWidth,
                screenHeight,
                renderWidth,
                renderHeight
        );
        int x = anchor[0] - (renderWidth / 2) + ClientConfig.hudHealthBarX;
        int y = anchor[1] - (renderHeight / 2) + ClientConfig.hudHealthBarY;

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float progress = maxHealth > 0 ? Math.min(1f, health / maxHealth) : 0f;

        GuiGraphics graphics = event.getGuiGraphics();
        float fontScale = ClientConfig.hudHealthBarFontScale;
        int absorptionGap = Math.max(1, Math.round(BASE_ABSORPTION_GAP * scale));

        float absorption = player.getAbsorptionAmount();
        if (absorption > 0) {
            HudTextHelper.drawLeftOfBar(
                    graphics,
                    mc.font,
                    HudTextHelper.immortalText(HudTextHelper.formatCompactNumber(absorption)),
                    x, y, renderWidth, renderHeight,
                    absorptionGap,
                    fontScale,
                    ABSORPTION_COLOR,
                    true
            );
        }

        HudBarRenderer.draw(graphics, EMPTY, HP_FULL, HP_BAR, x, y, scale, progress);

        String text = HudTextHelper.formatCompactNumber(health) + " / " + HudTextHelper.formatCompactNumber(maxHealth);
        HudTextHelper.drawCenteredInBar(
                graphics,
                mc.font,
                HudTextHelper.immortalText(text),
                x, y, renderWidth, renderHeight,
                fontScale,
                0xFFFFFF,
                true
        );
    }
}
