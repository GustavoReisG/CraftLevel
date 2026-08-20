package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.item.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Overlay de portal nether em vermelho enquanto a Recall Rune carrega. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class RecallPortalOverlay {

    private RecallPortalOverlay() {}

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.PORTAL.id().equals(event.getOverlay().id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isUsingItem()) {
            return;
        }
        ItemStack using = minecraft.player.getUseItem();
        if (!using.is(ModItems.RECALL_RUNE.get())) {
            return;
        }

        int duration = using.getUseDuration();
        if (duration <= 0) {
            return;
        }
        float progress = 1.0F - minecraft.player.getUseItemRemainingTicks() / (float) duration;
        progress = Math.min(1.0F, Math.max(0.0F, progress));
        float alpha = progress * progress;
        alpha = alpha * 0.8F + 0.2F;
        renderRedPortal(event.getGuiGraphics(), minecraft, alpha);
    }

    private static void renderRedPortal(GuiGraphics graphics, Minecraft minecraft, float alpha) {
        TextureAtlasSprite sprite = minecraft.getBlockRenderer()
                .getBlockModelShaper()
                .getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 0.12F, 0.08F, alpha);
        graphics.blit(0, 0, -90, graphics.guiWidth(), graphics.guiHeight(), sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
    }
}
