package com.grgs.craftlevel.client.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGlory;
import com.grgs.craftlevel.client.MobPinKeys;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
import com.grgs.craftlevel.network.UseSkillPacket;
import com.grgs.craftlevel.skill.SkillCatalog;
import com.grgs.craftlevel.skill.SkillDefinition;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/** Hotbar de feitiços enquanto Q está pressionado; tecla O abre o menu de skills. */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class SpellHotbarHud {

    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final int HOTBAR_W = 182;
    private static final int HOTBAR_H = 22;

    private static int selectedSlot;

    private SpellHotbarHud() {}

    public static boolean isSpellBarActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return MobPinKeys.SPELL_HOTBAR != null
                && MobPinKeys.SPELL_HOTBAR.isDown()
                && minecraft.screen == null
                && minecraft.player != null;
    }

    public static int selectedSlot() {
        return selectedSlot;
    }

    public static void cycleSelection(int delta) {
        selectedSlot = Math.floorMod(selectedSlot + delta, PlayerGlory.SPELL_HOTBAR_SIZE);
    }

    public static void tryCastSelected() {
        String skillId = SyncPlayerGloryPacket.ClientGloryState.spellHotbar[selectedSlot];
        if (skillId == null || skillId.isEmpty()) {
            return;
        }
        PacketHandler.CHANNEL.sendToServer(new UseSkillPacket(skillId));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (MobPinKeys.OPEN_SKILLS != null && MobPinKeys.OPEN_SKILLS.consumeClick()) {
            if (minecraft.screen instanceof SkillMenuScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new SkillMenuScreen());
            }
        }

        if (isSpellBarActive()) {
            while (minecraft.options.keyDrop.consumeClick()) {
                // Consome drop vanilla enquanto a hotbar de feitiços está ativa.
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!isSpellBarActive()) {
            return;
        }
        cycleSelection(event.getScrollDelta() > 0 ? -1 : 1);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isSpellBarActive() || !event.isUseItem()) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        tryCastSelected();
    }

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiOverlayEvent.Pre event) {
        if (!isSpellBarActive() || !VanillaGuiOverlay.HOTBAR.id().equals(event.getOverlay().id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        render(event.getGuiGraphics(), minecraft, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
        event.setCanceled(true);
    }

    public static void renderAssignBar(GuiGraphics graphics, Minecraft minecraft, int centerX, int y, String assigningSkillId) {
        int left = centerX - HOTBAR_W / 2;
        renderSlots(graphics, minecraft, left, y, assigningSkillId);
    }

    private static void render(GuiGraphics graphics, Minecraft minecraft, int screenWidth, int screenHeight) {
        int left = (screenWidth - HOTBAR_W) / 2;
        int top = screenHeight - HOTBAR_H;
        renderSlots(graphics, minecraft, left, top, null);
    }

    private static void renderSlots(GuiGraphics graphics, Minecraft minecraft, int left, int top, String assigningSkillId) {
        Font font = minecraft.font;
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();

        RenderSystem.enableBlend();
        graphics.blit(WIDGETS, left, top, 0, 0, HOTBAR_W, HOTBAR_H);
        graphics.blit(WIDGETS, left - 1 + selectedSlot * 20, top - 1, 0, 22, 24, 24);

        String[] hotbar = SyncPlayerGloryPacket.ClientGloryState.spellHotbar;
        for (int slot = 0; slot < PlayerGlory.SPELL_HOTBAR_SIZE; slot++) {
            int slotX = left + 3 + slot * 20;
            int slotY = top + 3;
            String skillId = hotbar[slot];
            if (skillId != null && !skillId.isEmpty()) {
                Optional<SkillDefinition> def = SkillCatalog.byId(skillId);
                if (def.isPresent()) {
                    graphics.blit(def.get().icon(), slotX, slotY, 16, 16, 0.0F, 0.0F, 32, 32, 32, 32);
                }
                int remaining = SyncPlayerGloryPacket.ClientGloryState.remainingCooldownTicks(skillId, gameTime);
                if (remaining > 0) {
                    int seconds = Math.max(1, (int) Math.ceil(remaining / 20.0));
                    String text = String.valueOf(seconds);
                    graphics.drawString(font, text, slotX + 16 - font.width(text), slotY + 8, 0xFFFFFF, true);
                }
            }
            if (assigningSkillId != null && assigningSkillId.equals(skillId)) {
                graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0x66FFFFFF);
            }
        }
        RenderSystem.disableBlend();
    }
}
