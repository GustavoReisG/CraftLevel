package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.RequestMobLevelPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/** Card fixo de mob (busto + stats) ao mirar e pressionar E. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class MobPinHud {

    private static final int BASE_TEXT_WIDTH = 108;
    private static final int BASE_BAR_WIDTH = 4;
    private static final int BASE_BAR_GAP = 6;
    private static final int BASE_PORTRAIT_GAP = 4;
    private static final int BASE_LINE_HEIGHT = 11;
    private static final float BASE_NAME_SCALE = 0.85f;
    private static final float BASE_LINE_SCALE = 0.72f;
    private static final int BAR_COLOR = 0xFFE8E8E8;
    private static final int BAR_SHADOW = 0x80000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFCCCCCC;
    private static final double FOCUS_REACH = 128.0D;

    @Nullable
    private static PinnedMob pinned;

    private MobPinHud() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MobPinKeys.PIN_MOB == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        if (!MobPinKeys.PIN_MOB.consumeClick()) {
            return;
        }

        LivingEntity target = findFocusedLiving(minecraft);
        if (target == null) {
            clearPin();
            return;
        }

        if (pinned != null && pinned.entityId == target.getId()) {
            clearPin();
            minecraft.options.keyInventory.consumeClick();
            return;
        }

        pinEntity(minecraft, target);
        minecraft.options.keyInventory.consumeClick();
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.CROSSHAIR.id().equals(event.getOverlay().id()) || pinned == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.renderDebug || minecraft.player == null) {
            return;
        }

        pinned.refreshLiveStats(minecraft);
        renderCard(event.getGuiGraphics(), minecraft);
    }

    private static void pinEntity(Minecraft minecraft, LivingEntity entity) {
        clearPin();

        int level = readLevel(entity);
        if (level <= 0) {
            PacketHandler.CHANNEL.sendToServer(new RequestMobLevelPacket(entity.getId()));
        }

        MobPortraitCapturer preview = MobPortraitCapturer.capture(entity);
        pinned = new PinnedMob(
                entity.getId(),
                entity.getDisplayName(),
                level,
                entity.getHealth(),
                entity.getMaxHealth(),
                disposition(entity),
                preview
        );
    }

    private static int readLevel(LivingEntity entity) {
        return entity.getCapability(MobLevelProvider.MOB_LEVEL)
                .map(cap -> cap.getLevel())
                .orElse(0);
    }

    @Nullable
    private static LivingEntity findFocusedLiving(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }

        float partialTick = minecraft.getFrameTime();
        Vec3 start = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 end = start.add(look.scale(FOCUS_REACH));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(FOCUS_REACH)).inflate(1.0D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBox,
                entity -> isFocusCandidate(entity, player),
                FOCUS_REACH * FOCUS_REACH
        );

        if (hit != null) {
            LivingEntity living = resolveLivingTarget(hit.getEntity());
            if (living != null && living.isAlive() && living != player) {
                return living;
            }
        }

        HitResult crosshair = minecraft.hitResult;
        if (crosshair instanceof EntityHitResult entityHit) {
            LivingEntity living = resolveLivingTarget(entityHit.getEntity());
            if (living != null && living.isAlive() && living != player) {
                return living;
            }
        }

        return null;
    }

    /** Aceita LivingEntity ou PartEntity cujo parent seja LivingEntity (ex.: Ender Dragon). */
    private static boolean isFocusCandidate(Entity entity, Player player) {
        if (!entity.isPickable() || !entity.isAlive()) {
            return false;
        }
        LivingEntity living = resolveLivingTarget(entity);
        return living != null && living != player && living.isAlive();
    }

    /**
     * Resolve partes multipart (Forge PartEntity / EnderDragonPart) para o LivingEntity pai.
     * Entidades LivingEntity normais retornam a si mesmas.
     */
    @Nullable
    private static LivingEntity resolveLivingTarget(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        if (entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity parent) {
            return parent;
        }
        return null;
    }

    private static String disposition(LivingEntity entity) {
        if (entity instanceof Player) {
            return "Player";
        }
        MobCategory category = entity.getType().getCategory();
        return category.isFriendly() ? "Peaceful" : "Hostile";
    }

    private static void renderCard(GuiGraphics graphics, Minecraft minecraft) {
        if (pinned == null) {
            return;
        }

        float scale = ClientConfig.hudMobFocusScale;
        float fontScale = ClientConfig.hudMobFocusFontScale;
        boolean portraitFirst = "portrait_first".equals(ClientConfig.hudMobFocusDirection);

        int textWidth = Math.round(BASE_TEXT_WIDTH * scale);
        int barWidth = Math.max(1, Math.round(BASE_BAR_WIDTH * scale));
        int portraitSize = MobPortraitCapturer.displaySize(scale);
        int barGap = Math.round(BASE_BAR_GAP * scale);
        int portraitGap = Math.round(BASE_PORTRAIT_GAP * scale);
        int lineHeight = Math.max(8, Math.round(BASE_LINE_HEIGHT * scale));
        float nameScale = BASE_NAME_SCALE * fontScale;
        float lineTextScale = BASE_LINE_SCALE * fontScale;

        int cardWidth = textWidth + barGap + barWidth + portraitGap + portraitSize;
        int cardHeight = portraitSize;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int[] anchor = ClientHudLayout.anchorCenter(
                ClientConfig.hudMobFocusAnchor,
                screenWidth,
                screenHeight,
                cardWidth,
                cardHeight
        );
        int cardX = anchor[0] - (cardWidth / 2) + ClientConfig.hudMobFocusX;
        int cardY = anchor[1] - (cardHeight / 2) + ClientConfig.hudMobFocusY;

        int textX;
        int barX;
        int portraitX;
        if (portraitFirst) {
            portraitX = cardX;
            barX = portraitX + portraitSize + portraitGap;
            textX = barX + barWidth + barGap;
        } else {
            textX = cardX;
            barX = textX + textWidth + barGap;
            portraitX = barX + barWidth + portraitGap;
        }

        graphics.fill(barX - 1, cardY - 1, portraitX + portraitSize + 1, cardY + cardHeight + 1, BAR_SHADOW);
        graphics.fill(barX, cardY, barX + barWidth, cardY + cardHeight, BAR_COLOR);

        if (pinned.preview != null) {
            pinned.preview.renderPortrait(graphics, portraitX, cardY, scale);
        } else {
            MobPortraitCapturer.renderEmptyPortrait(graphics, portraitX, cardY, scale);
        }

        int textY = cardY;
        HudTextHelper.drawScaledComponent(
                graphics,
                minecraft.font,
                HudTextHelper.immortalText(pinned.name.getString()),
                textX,
                textY,
                nameScale,
                TEXT_COLOR,
                true
        );
        textY += lineHeight;

        String levelLine = pinned.level > 0 ? "LVL " + pinned.level : "LVL ...";
        HudTextHelper.drawScaledString(graphics, minecraft.font, levelLine, textX, textY, lineTextScale, TEXT_COLOR, true);
        textY += lineHeight;

        String hpLine = Math.round(pinned.health) + " / " + Math.round(pinned.maxHealth);
        HudTextHelper.drawScaledString(graphics, minecraft.font, hpLine, textX, textY, lineTextScale, TEXT_COLOR, true);
        textY += lineHeight;

        HudTextHelper.drawScaledString(
                graphics,
                minecraft.font,
                pinned.disposition,
                textX,
                textY,
                lineTextScale,
                MUTED_COLOR,
                true
        );
    }

    private static void clearPin() {
        if (pinned != null) {
            pinned.release(Minecraft.getInstance());
            pinned = null;
        }
    }

    private static final class PinnedMob {
        private final int entityId;
        private final Component name;
        private int level;
        private float health;
        private float maxHealth;
        private final String disposition;
        @Nullable
        private final MobPortraitCapturer preview;

        private PinnedMob(
                int entityId,
                Component name,
                int level,
                float health,
                float maxHealth,
                String disposition,
                @Nullable MobPortraitCapturer preview
        ) {
            this.entityId = entityId;
            this.name = name;
            this.level = level;
            this.health = health;
            this.maxHealth = maxHealth;
            this.disposition = disposition;
            this.preview = preview;
        }

        private void refreshLiveStats(Minecraft minecraft) {
            if (minecraft.level == null) {
                return;
            }

            var entity = minecraft.level.getEntity(entityId);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                health = living.getHealth();
                maxHealth = living.getMaxHealth();
                int liveLevel = living.getCapability(MobLevelProvider.MOB_LEVEL)
                        .map(cap -> cap.getLevel())
                        .orElse(0);
                if (liveLevel > 0) {
                    level = liveLevel;
                } else if (level <= 0) {
                    PacketHandler.CHANNEL.sendToServer(new RequestMobLevelPacket(entityId));
                }
            }
        }

        private void release(Minecraft minecraft) {
            if (preview != null) {
                preview.release();
            }
        }
    }
}
