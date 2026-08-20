package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.RequestMobLevelPacket;
import com.grgs.craftlevel.network.SetMobFocusPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
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

    private static final ResourceLocation CARD_BG =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/mob_circle_bg.png");
    private static final ResourceLocation HEALTH_BAR =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/mob_circle_bar.png");

    /** Faixa da barra de vida na textura (px da esquerda). */
    private static final int BAR_START_X = 308;
    private static final int BAR_END_X = 476;

    /** Fonte Immortal; tamanho final = font_scale da config × escala do card. */
    private static final int TEXT_COLOR = 0xFFFFFF;

    private static final int NAME_Y = 86;
    private static final int NAME_LEFT = 272;
    private static final int NAME_RIGHT = 472;

    private static final int TYPE_Y = 196;
    private static final int TYPE_LEFT = 272;
    private static final int TYPE_RIGHT = 472;

    private static final int HEALTH_Y = 146;
    private static final int HEALTH_LEFT = 312;
    private static final int HEALTH_RIGHT = 472;

    /** Centro do encaixe inferior do painel direito (não o centro da coluna de texto). */
    private static final int LEVEL_CENTER_X = 362;
    private static final int LEVEL_CENTER_Y = 254;
    /** Raio da bolinha de disposition (Hostile/Peaceful) no card em px de textura. */
    private static final int DISPOSITION_DOT_RADIUS = 14;
    private static final int HOSTILE_DOT_COLOR = 0xFFE53935;
    private static final int PEACEFUL_DOT_COLOR = 0xFF43A047;

    private static final double FOCUS_REACH = 128.0D;

    @Nullable
    private static PinnedMob pinned;

    private MobPinHud() {}

    /** EntityId do mob no Mob Focus, se ainda existir e estiver vivo; senão −1. */
    public static int pinnedEntityId() {
        if (pinned == null) {
            return -1;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return -1;
        }
        Entity entity = minecraft.level.getEntity(pinned.entityId);
        if (entity instanceof LivingEntity living && living.isAlive()) {
            return pinned.entityId;
        }
        return -1;
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

        // Remove o focus se o mob morreu ou descarregou.
        if (pinned != null && !isPinnedStillValid(minecraft)) {
            clearPin();
        }

        if (MobPinKeys.PIN_MOB == null || minecraft.screen != null) {
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

    private static boolean isPinnedStillValid(Minecraft minecraft) {
        if (pinned == null || minecraft.level == null) {
            return false;
        }
        Entity entity = minecraft.level.getEntity(pinned.entityId);
        return entity instanceof LivingEntity living && living.isAlive();
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
                isHostile(entity),
                preview
        );
        PacketHandler.CHANNEL.sendToServer(new SetMobFocusPacket(entity.getId()));
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

        // Preferir o alvo exato do crosshair vanilla (centro da tela).
        HitResult crosshair = minecraft.hitResult;
        if (crosshair instanceof EntityHitResult entityHit) {
            LivingEntity living = resolveLivingTarget(entityHit.getEntity());
            if (living != null && living.isAlive() && living != player) {
                return living;
            }
        }

        // Alcance estendido: raio da câmera (ponteiro/centro da tela), não da cabeça do player.
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 start = camera.getPosition();
        var lookVec = camera.getLookVector();
        Vec3 look = new Vec3(lookVec.x(), lookVec.y(), lookVec.z());
        Vec3 end = start.add(look.scale(FOCUS_REACH));

        double maxDistSq = FOCUS_REACH * FOCUS_REACH;
        HitResult blockHit = minecraft.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            maxDistSq = start.distanceToSqr(blockHit.getLocation());
        }

        AABB searchBox = new AABB(start, end).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBox,
                entity -> isFocusCandidate(entity, player),
                maxDistSq
        );

        if (hit != null) {
            LivingEntity living = resolveLivingTarget(hit.getEntity());
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

    /** Hostile = bolinha vermelha; Peaceful/Player = bolinha verde. */
    private static boolean isHostile(LivingEntity entity) {
        if (entity instanceof Player) {
            return false;
        }
        MobCategory category = entity.getType().getCategory();
        return !category.isFriendly();
    }

    private static void renderCard(GuiGraphics graphics, Minecraft minecraft) {
        if (pinned == null) {
            return;
        }

        float scale = ClientConfig.hudMobFocusScale;
        float textScale = ClientConfig.hudMobFocusFontScale * scale;

        int cardWidth = MobPortraitCapturer.displayCardWidth(scale);
        int cardHeight = MobPortraitCapturer.displayCardHeight(scale);

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

        float progress = pinned.maxHealth > 0f
                ? Math.min(1f, Math.max(0f, pinned.health / pinned.maxHealth))
                : 0f;

        // Fundo → barra de vida → busto → mob_circle → textos
        blitFullCardTexture(graphics, CARD_BG, cardX, cardY, scale);
        blitHealthBar(graphics, cardX, cardY, scale, progress);
        MobPortraitCapturer.renderMobOnly(graphics, cardX, cardY, scale, pinned.preview);
        MobPortraitCapturer.blitCard(graphics, cardX, cardY, scale);

        var font = minecraft.font;

        HudTextHelper.drawCenteredClipped(
                graphics,
                font,
                pinned.name.getString(),
                cardX + Math.round(NAME_LEFT * scale),
                cardX + Math.round(NAME_RIGHT * scale),
                cardY + Math.round(NAME_Y * scale),
                textScale,
                TEXT_COLOR,
                false
        );

        String levelText = pinned.level > 0 ? "Lvl " + pinned.level : "Lvl ...";
        HudTextHelper.drawCenteredClipped(
                graphics,
                font,
                levelText,
                cardX + Math.round(TYPE_LEFT * scale),
                cardX + Math.round(TYPE_RIGHT * scale),
                cardY + Math.round(TYPE_Y * scale),
                textScale,
                TEXT_COLOR,
                false
        );

        HudTextHelper.drawCenteredClipped(
                graphics,
                font,
                HudTextHelper.formatCompactNumber(pinned.health),
                cardX + Math.round(HEALTH_LEFT * scale),
                cardX + Math.round(HEALTH_RIGHT * scale),
                cardY + Math.round(HEALTH_Y * scale),
                textScale,
                TEXT_COLOR,
                false
        );

        drawDispositionDot(
                graphics,
                cardX + Math.round(LEVEL_CENTER_X * scale),
                cardY + Math.round(LEVEL_CENTER_Y * scale),
                Math.max(2, Math.round(DISPOSITION_DOT_RADIUS * scale)),
                pinned.hostile ? HOSTILE_DOT_COLOR : PEACEFUL_DOT_COLOR
        );
    }

    private static void drawDispositionDot(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        // Diâmetro ímpar para o centro geométrico cair exatamente em (centerX, centerY).
        int diameter = radius * 2 + 1;
        int top = centerY - radius;
        for (int row = 0; row < diameter; row++) {
            int dy = row - radius;
            int halfWidth = (int) Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            int y = top + row;
            graphics.fill(centerX - halfWidth, y, centerX + halfWidth + 1, y + 1, color);
        }
    }

    private static void blitFullCardTexture(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            float scale
    ) {
        int renderWidth = MobPortraitCapturer.displayCardWidth(scale);
        int renderHeight = MobPortraitCapturer.displayCardHeight(scale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(
                texture,
                x,
                y,
                renderWidth,
                renderHeight,
                0,
                0,
                MobPortraitCapturer.CARD_WIDTH,
                MobPortraitCapturer.CARD_HEIGHT,
                MobPortraitCapturer.CARD_WIDTH,
                MobPortraitCapturer.CARD_HEIGHT
        );
    }

    /**
     * Barra sob o card: corta {@code mob_circle_bar} da esquerda até
     * {@code 308 + (476-308) * progress}.
     */
    private static void blitHealthBar(GuiGraphics graphics, int x, int y, float scale, float progress) {
        int cutTex = BAR_START_X + Math.round((BAR_END_X - BAR_START_X) * progress);
        if (cutTex <= 0) {
            return;
        }

        int cutRender = Math.round(cutTex * scale);
        int renderHeight = MobPortraitCapturer.displayCardHeight(scale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(
                HEALTH_BAR,
                x,
                y,
                cutRender,
                renderHeight,
                0,
                0,
                cutTex,
                MobPortraitCapturer.CARD_HEIGHT,
                MobPortraitCapturer.CARD_WIDTH,
                MobPortraitCapturer.CARD_HEIGHT
        );
    }

    private static void clearPin() {
        if (pinned != null) {
            pinned.release(Minecraft.getInstance());
            pinned = null;
            PacketHandler.CHANNEL.sendToServer(new SetMobFocusPacket(-1));
        }
    }

    private static final class PinnedMob {
        private final int entityId;
        private final Component name;
        private int level;
        private float health;
        private float maxHealth;
        private final boolean hostile;
        @Nullable
        private final MobPortraitCapturer preview;

        private PinnedMob(
                int entityId,
                Component name,
                int level,
                float health,
                float maxHealth,
                boolean hostile,
                @Nullable MobPortraitCapturer preview
        ) {
            this.entityId = entityId;
            this.name = name;
            this.level = level;
            this.health = health;
            this.maxHealth = maxHealth;
            this.hostile = hostile;
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
