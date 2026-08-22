package com.grgs.craftlevel.client.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.client.MobPinHud;
import com.grgs.craftlevel.client.MobPinKeys;
import com.grgs.craftlevel.config.SkillsParser;
import com.grgs.craftlevel.network.AssignSpellHotbarPacket;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SpendSkillPointPacket;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
import com.grgs.craftlevel.network.UseSkillPacket;
import com.grgs.craftlevel.skill.BloodRushSkill;
import com.grgs.craftlevel.skill.BlinkStepSkill;
import com.grgs.craftlevel.skill.CarnageSkill;
import com.grgs.craftlevel.skill.CripplingAuraSkill;
import com.grgs.craftlevel.skill.DarknessLightSkill;
import com.grgs.craftlevel.skill.FrozenMindSkill;
import com.grgs.craftlevel.skill.FrozenRushSkill;
import com.grgs.craftlevel.skill.GhostSaturationSkill;
import com.grgs.craftlevel.skill.ImbueMasterSkill;
import com.grgs.craftlevel.skill.InfernoBoltSkill;
import com.grgs.craftlevel.skill.LastChanceSkill;
import com.grgs.craftlevel.skill.LightSpeedSkill;
import com.grgs.craftlevel.skill.MinerFrenzySkill;
import com.grgs.craftlevel.skill.MindFurySkill;
import com.grgs.craftlevel.skill.NetherElevationSkill;
import com.grgs.craftlevel.skill.NetherRushSkill;
import com.grgs.craftlevel.skill.ProspectorSkill;
import com.grgs.craftlevel.skill.SenseOfPreySkill;
import com.grgs.craftlevel.skill.SkillCatalog;
import com.grgs.craftlevel.skill.SkillCategory;
import com.grgs.craftlevel.skill.SkillCooldowns;
import com.grgs.craftlevel.skill.SkillDefinition;
import com.grgs.craftlevel.skill.SkillKind;
import com.grgs.craftlevel.skill.SoulAnchorSkill;
import com.grgs.craftlevel.skill.SoulLanceSkill;
import com.grgs.craftlevel.skill.SoulRenderSkill;
import com.grgs.craftlevel.skill.SpectralClimbSkill;
import com.grgs.craftlevel.skill.SpiritReturnSkill;
import com.grgs.craftlevel.skill.StrengthRecoverySkill;
import com.grgs.craftlevel.skill.SupernovaSkill;
import com.grgs.craftlevel.skill.ProtectiveShockwaveSkill;
import com.grgs.craftlevel.skill.QuickSurviveSkill;
import com.grgs.craftlevel.skill.RecallSkill;
import com.grgs.craftlevel.skill.ReinforcedBonesSkill;
import com.grgs.craftlevel.skill.TerrorPulseSkill;
import com.grgs.craftlevel.skill.VampireAttackSkill;
import com.grgs.craftlevel.skill.VampireBloodSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Menu de skills (tamanho de inventário, sem slots). */
@OnlyIn(Dist.CLIENT)
public class SkillMenuScreen extends Screen {

    public static final int WINDOW_W = 264;
    public static final int WINDOW_H = 166;
    private static final int TAB_COUNT = 4;
    private static final int TAB_W = WINDOW_W / TAB_COUNT;
    private static final int TAB_H = 32;
    private static final int TAB_ICON = 14;
    private static final int CARD_H = 52;
    private static final int ICON_SIZE = 24;
    private static final int COL2_W = 28;
    private static final int LOCK_SIZE = 12;
    private static final int PLUS_SIZE = 8;
    private static final int BORDER = 0xFFC6C6C6;
    private static final int INNER = 0xFF737373;
    private static final int INNER_PREVIEW = 0xFF5A5A5A;
    private static final int TAB_INACTIVE = 0xFF6E6E6E;
    private static final int TEXT = 0xF0F0F0;

    private static final ResourceLocation LOCK =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/skill_lock.png");
    private static final ResourceLocation UNLOCK =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/skill_unlock.png");
    private static final ResourceLocation PLUS =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/skill_plus.png");
    private static final ResourceLocation HOURGLASS =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/skill_hourglass.png");
    private static final ResourceLocation TAB_DAMAGE =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/tab_damage.png");
    private static final ResourceLocation TAB_DEFENSE =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/tab_defense.png");
    private static final ResourceLocation TAB_UTILITY =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/tab_utility.png");
    private static final ResourceLocation TAB_PASSIVE =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/skills/tab_passive.png");

    private static final SkillCategory[] TAB_ORDER = {
            SkillCategory.DAMAGE,
            SkillCategory.DEFENSE,
            SkillCategory.UTILITY,
            SkillCategory.PASSIVE
    };

    private SkillCategory page = SkillCategory.DAMAGE;
    private String assigningSkillId;
    private int scrollY;
    private int leftPos;
    private int topPos;

    public SkillMenuScreen() {
        super(Component.translatable("gui.craftlevel.skills.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        leftPos = (width - WINDOW_W) / 2;
        topPos = (height - WINDOW_H) / 2 + TAB_H / 2;
        scrollY = 0;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MobPinKeys.OPEN_SKILLS != null && MobPinKeys.OPEN_SKILLS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (assigningSkillId != null && keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int slot = keyCode - GLFW.GLFW_KEY_1;
            PacketHandler.CHANNEL.sendToServer(new AssignSpellHotbarPacket(assigningSkillId, slot));
            assigningSkillId = null;
            return true;
        }
        if (assigningSkillId != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            assigningSkillId = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInsideInner(mouseX, mouseY)) {
            int max = maxScroll();
            if (max > 0) {
                scrollY = Mth.clamp(scrollY - (int) Math.round(delta * 12), 0, max);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickTab(mouseX, mouseY)) {
            return true;
        }
        if (handleCardClick(mouseX, mouseY, button)) {
            return true;
        }
        if (assigningSkillId != null && button == 0 && !isInsideWindow(mouseX, mouseY)) {
            assigningSkillId = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickTab(double mouseX, double mouseY) {
        int tabTop = topPos - TAB_H + 2;
        if (mouseY < tabTop || mouseY >= topPos + 2) {
            return false;
        }
        for (int i = 0; i < TAB_ORDER.length; i++) {
            int tabLeft = leftPos + i * TAB_W;
            if (mouseX >= tabLeft && mouseX < tabLeft + TAB_W) {
                page = TAB_ORDER[i];
                scrollY = 0;
                assigningSkillId = null;
                return true;
            }
        }
        return false;
    }

    private boolean handleCardClick(double mouseX, double mouseY, int button) {
        List<SkillDefinition> skills = SkillCatalog.byCategory(page);
        int innerLeft = leftPos + 5;
        int innerTop = topPos + 18;
        int innerRight = leftPos + WINDOW_W - 5;
        int innerBottom = topPos + WINDOW_H - 5;
        if (mouseX < innerLeft || mouseX >= innerRight || mouseY < innerTop || mouseY >= innerBottom) {
            return false;
        }

        int y = innerTop - scrollY;
        for (SkillDefinition def : skills) {
            int cardTop = y;
            int cardBottom = y + CARD_H;
            if (mouseY >= Math.max(innerTop, cardTop) && mouseY < Math.min(innerBottom, cardBottom)) {
                return handleCardClick(def, mouseX, mouseY, button, innerLeft, cardTop);
            }
            y += CARD_H + 2;
        }
        return false;
    }

    private boolean handleCardClick(
            SkillDefinition def,
            double mouseX,
            double mouseY,
            int button,
            int cardLeft,
            int cardTop
    ) {
        CardLayout layout = layoutFor(cardLeft, cardTop);
        int level = SyncPlayerGloryPacket.ClientGloryState.skillLevel(def.id());

        if (level <= 0) {
            if (button == 0 && contains(mouseX, mouseY, layout.lockX, layout.lockY, LOCK_SIZE, LOCK_SIZE)) {
                PacketHandler.CHANNEL.sendToServer(new SpendSkillPointPacket(def.id()));
                return true;
            }
            return true;
        }

        if (contains(mouseX, mouseY, layout.iconX, layout.iconY, ICON_SIZE, ICON_SIZE)) {
            if (button == 0 && def.kind() == SkillKind.ACTIVE) {
                assigningSkillId = assigningSkillId != null && assigningSkillId.equals(def.id()) ? null : def.id();
                return true;
            }
            if (button == 1 && def.kind() == SkillKind.ACTIVE) {
                int targetId = SkillCatalog.NETHER_ELEVATION.equals(def.id())
                        || SkillCatalog.FROZEN_MIND.equals(def.id())
                        ? MobPinHud.pinnedEntityId()
                        : -1;
                PacketHandler.CHANNEL.sendToServer(new UseSkillPacket(def.id(), targetId));
                return true;
            }
        }

        if (level < def.maxLevel()
                && button == 0
                && contains(mouseX, mouseY, layout.plusX, layout.plusY, PLUS_SIZE, PLUS_SIZE)) {
            PacketHandler.CHANNEL.sendToServer(new SpendSkillPointPacket(def.id()));
            return true;
        }
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        drawTabs(graphics);
        drawWindow(graphics);
        drawCards(graphics, mouseX, mouseY);
        if (assigningSkillId != null) {
            SpellHotbarHud.renderAssignBar(
                    graphics,
                    minecraft,
                    width / 2,
                    topPos + WINDOW_H + 8,
                    assigningSkillId
            );
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.craftlevel.skill.assign_hint"),
                    width / 2,
                    topPos + WINDOW_H + 34,
                    0xFFFFFF
            );
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTabs(GuiGraphics graphics) {
        for (int i = 0; i < TAB_ORDER.length; i++) {
            SkillCategory category = TAB_ORDER[i];
            drawTab(
                    graphics,
                    leftPos + i * TAB_W,
                    page == category,
                    tabLabel(category),
                    tabIcon(category)
            );
        }
    }

    private void drawTab(
            GuiGraphics graphics,
            int x,
            boolean selected,
            Component label,
            ResourceLocation icon
    ) {
        int top = selected ? topPos - TAB_H + 2 : topPos - TAB_H + 5;
        int color = selected ? BORDER : TAB_INACTIVE;
        graphics.fill(x, top, x + TAB_W, topPos + 2, color);

        int textColor = selected ? 0x303030 : 0x9A9A9A;
        int textY = top + 3;
        int textX = x + (TAB_W - font.width(label)) / 2;
        graphics.drawString(font, label, textX, textY, textColor, false);

        int iconX = x + (TAB_W - TAB_ICON) / 2;
        int iconY = topPos - TAB_ICON - 3;
        graphics.blit(icon, iconX, iconY, TAB_ICON, TAB_ICON, 0.0F, 0.0F, 32, 32, 32, 32);
    }

    private static Component tabLabel(SkillCategory category) {
        return switch (category) {
            case DAMAGE -> Component.translatable("gui.craftlevel.skills.tab.damage");
            case DEFENSE -> Component.translatable("gui.craftlevel.skills.tab.defense");
            case UTILITY -> Component.translatable("gui.craftlevel.skills.tab.utility");
            case PASSIVE -> Component.translatable("gui.craftlevel.skills.tab.passive");
        };
    }

    private static ResourceLocation tabIcon(SkillCategory category) {
        return switch (category) {
            case DAMAGE -> TAB_DAMAGE;
            case DEFENSE -> TAB_DEFENSE;
            case UTILITY -> TAB_UTILITY;
            case PASSIVE -> TAB_PASSIVE;
        };
    }

    private void drawWindow(GuiGraphics graphics) {
        graphics.fill(leftPos, topPos, leftPos + WINDOW_W, topPos + WINDOW_H, BORDER);
        graphics.fill(leftPos + 3, topPos + 14, leftPos + WINDOW_W - 3, topPos + WINDOW_H - 3, INNER);

        graphics.drawString(font, title, leftPos + 8, topPos + 4, 0x303030, false);
        Component points = skillPointsLabel(SyncPlayerGloryPacket.ClientGloryState.skillPoints);
        int pointsX = leftPos + WINDOW_W - 6 - font.width(points);
        graphics.drawString(font, points, pointsX, topPos + 4, 0x303030, false);
    }

    private void drawCards(GuiGraphics graphics, int mouseX, int mouseY) {
        List<SkillDefinition> skills = SkillCatalog.byCategory(page);
        int innerLeft = leftPos + 5;
        int innerTop = topPos + 18;
        int innerRight = leftPos + WINDOW_W - 5;
        int innerBottom = topPos + WINDOW_H - 5;
        graphics.enableScissor(innerLeft, innerTop, innerRight, innerBottom);

        int y = innerTop - scrollY;
        List<Component> tooltip = null;
        int leftTooltipX = -1;
        int leftTooltipY = -1;
        for (SkillDefinition def : skills) {
            CardTooltipResult result = renderCard(graphics, def, innerLeft, y, innerRight - innerLeft, mouseX, mouseY);
            if (result.tooltip() != null) {
                tooltip = result.tooltip();
                leftTooltipX = result.leftAnchorX();
                leftTooltipY = result.leftAnchorY();
            }
            y += CARD_H + 2;
        }
        graphics.disableScissor();
        if (tooltip != null && !tooltip.isEmpty()) {
            if (leftTooltipX >= 0) {
                renderTooltipLeftOf(graphics, tooltip, leftTooltipX, leftTooltipY);
            } else {
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }

    private CardTooltipResult renderCard(
            GuiGraphics graphics,
            SkillDefinition def,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY
    ) {
        CardLayout layout = layoutFor(x, y);
        int level = SyncPlayerGloryPacket.ClientGloryState.skillLevel(def.id());
        int nextLevel = Math.min(def.maxLevel(), Math.max(1, level + 1));
        SkillsParser.LevelConfig nextConfig = SkillsParser.forLevel(def.id(), nextLevel);
        boolean lockHover = level <= 0 && contains(mouseX, mouseY, layout.lockX, layout.lockY, LOCK_SIZE, LOCK_SIZE);
        boolean plusHover = level > 0
                && level < def.maxLevel()
                && contains(mouseX, mouseY, layout.plusX, layout.plusY, PLUS_SIZE, PLUS_SIZE);
        boolean minHover = level > 0
                && level < def.maxLevel()
                && contains(mouseX, mouseY, layout.col2Left, y + 2, COL2_W, 10);

        int bg = plusHover ? INNER_PREVIEW : 0xFF686868;
        graphics.fill(x, y, x + width, y + CARD_H, bg);
        graphics.blit(def.icon(), layout.iconX, layout.iconY, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, 32, 32, 32, 32);

        List<Component> tooltip = null;
        int leftAnchorX = -1;
        int leftAnchorY = -1;
        if (level <= 0) {
            graphics.blit(
                    lockHover ? UNLOCK : LOCK,
                    layout.lockX,
                    layout.lockY,
                    LOCK_SIZE,
                    LOCK_SIZE,
                    0.0F,
                    0.0F,
                    16,
                    16,
                    16,
                    16
            );
            if (lockHover) {
                tooltip = List.of(
                        costTooltip(nextConfig.skillPointCost()),
                        Component.translatable("gui.craftlevel.skill.tooltip.required_level", nextConfig.minPlayerLevel())
                );
            }
        } else if (level >= def.maxLevel()) {
            drawCentered(graphics, level + "/" + def.maxLevel(), layout.col2Left, y + (CARD_H - 8) / 2, COL2_W, TEXT);
        } else {
            drawCentered(graphics, String.valueOf(nextConfig.minPlayerLevel()), layout.col2Left, y + 3, COL2_W, TEXT);
            drawCentered(graphics, level + "/" + def.maxLevel(), layout.col2Left, y + (CARD_H - 8) / 2, COL2_W, TEXT);
            graphics.blit(PLUS, layout.plusX, layout.plusY, PLUS_SIZE, PLUS_SIZE, 0.0F, 0.0F, 16, 16, 16, 16);
            if (plusHover) {
                tooltip = List.of(costTooltip(nextConfig.skillPointCost()));
                leftAnchorX = layout.plusX;
                leftAnchorY = layout.plusY + PLUS_SIZE / 2;
            } else if (minHover) {
                tooltip = List.of(Component.translatable("gui.craftlevel.skill.tooltip.required_upgrade"));
            }
        }

        int displayLevel = plusHover ? nextLevel : Math.max(1, level);
        boolean preview = plusHover && level > 0 && level < def.maxLevel();
        MutableComponent title = Component.translatable(def.titleKey()).withStyle(ChatFormatting.BOLD);
        graphics.drawString(font, title, layout.col3Left, y + 3, TEXT, false);

        Component desc = description(def, displayLevel);
        int descMaxW = Math.max(20, x + width - layout.col3Left - (def.kind() == SkillKind.ACTIVE ? 36 : 6));
        List<FormattedCharSequence> lines = font.split(desc, descMaxW);
        int textY = y + 14;
        int shownLines = Math.min(3, lines.size());
        for (int i = 0; i < shownLines; i++) {
            graphics.drawString(font, lines.get(i), layout.col3Left, textY, preview ? 0xFFE0E0 : TEXT, false);
            textY += 9;
        }

        int descTop = y + 14;
        int descH = Math.max(9, shownLines * 9);
        if (tooltip == null
                && contains(mouseX, mouseY, layout.col3Left, descTop, descMaxW, descH)) {
            tooltip = wrapDescriptionTooltip(desc);
        }

        int cdSeconds = displayedCooldownSeconds(def, displayLevel);
        if (def.kind() == SkillKind.ACTIVE) {
            String cdText = String.valueOf(cdSeconds);
            int cdW = font.width(cdText);
            int right = x + width - 2;
            int textX = right - cdW;
            int hgX = textX - 9;
            int hgY = y + CARD_H - 12;
            graphics.blit(HOURGLASS, hgX, hgY, 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
            graphics.drawString(font, cdText, textX, hgY, TEXT, false);
        }
        return new CardTooltipResult(tooltip, leftAnchorX, leftAnchorY);
    }

    private List<Component> wrapDescriptionTooltip(Component desc) {
        List<Component> out = new ArrayList<>();
        for (FormattedCharSequence line : font.split(desc, 200)) {
            StringBuilder sb = new StringBuilder();
            line.accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
            out.add(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
        }
        if (out.isEmpty()) {
            out.add(desc.copy().withStyle(ChatFormatting.GRAY));
        }
        return out;
    }

    private void renderTooltipLeftOf(GuiGraphics graphics, List<Component> lines, int anchorX, int anchorY) {
        int padX = 4;
        int padY = 3;
        int lineH = 10;
        int boxW = 0;
        for (Component line : lines) {
            boxW = Math.max(boxW, font.width(line));
        }
        boxW += padX * 2;
        int boxH = lines.size() * lineH + padY * 2 - 2;
        int boxX = Math.max(2, anchorX - boxW - 6);
        int boxY = Mth.clamp(anchorY - boxH / 2, 2, height - boxH - 2);

        graphics.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF110011);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xF0100010);
        int textY = boxY + padY;
        for (Component line : lines) {
            graphics.drawString(font, line, boxX + padX, textY, 0xFFFFFF, false);
            textY += lineH;
        }
    }

    private record CardTooltipResult(List<Component> tooltip, int leftAnchorX, int leftAnchorY) {}

    private CardLayout layoutFor(int cardLeft, int cardTop) {
        int iconX = cardLeft + 2;
        int iconY = cardTop + (CARD_H - ICON_SIZE) / 2;
        int col2Left = iconX + ICON_SIZE + 4;
        int col3Left = col2Left + COL2_W + 4;
        int lockX = col2Left + (COL2_W - LOCK_SIZE) / 2;
        int lockY = cardTop + (CARD_H - LOCK_SIZE) / 2;
        int plusX = col2Left + (COL2_W - PLUS_SIZE) / 2;
        int plusY = cardTop + CARD_H - PLUS_SIZE - 3;
        return new CardLayout(iconX, iconY, col2Left, col3Left, lockX, lockY, plusX, plusY);
    }

    private record CardLayout(
            int iconX,
            int iconY,
            int col2Left,
            int col3Left,
            int lockX,
            int lockY,
            int plusX,
            int plusY
    ) {}

    private Component description(SkillDefinition def, int level) {
        if (SkillCatalog.SENSE_OF_PREY.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    SenseOfPreySkill.radiusBlocks(level),
                    SenseOfPreySkill.durationSeconds(level)
            );
        }
        if (SkillCatalog.GHOST_SATURATION.equals(def.id())) {
            return Component.translatable(def.descKey(), GhostSaturationSkill.durationSeconds(level));
        }
        if (SkillCatalog.SOUL_ANCHOR.equals(def.id())) {
            return Component.translatable(def.descKey(), SoulAnchorSkill.saveChancePercent(level));
        }
        if (SkillCatalog.SPECTRAL_CLIMB.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    (int) SpectralClimbSkill.maxClimbBlocks(level)
            );
        }
        if (SkillCatalog.IMBUE_MASTER.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    ImbueMasterSkill.bonusPercent(level)
            );
        }
        if (SkillCatalog.VAMPIRE_ATTACK.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    (int) (VampireAttackSkill.LIFESTEAL_FRACTION * 100),
                    VampireAttackSkill.durationSeconds(level)
            );
        }
        if (SkillCatalog.SPIRIT_RETURN.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    SpiritReturnSkill.auraDurationSeconds() / 60
            );
        }
        if (SkillCatalog.CARNAGE.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    CarnageSkill.huntDurationSeconds() / 60,
                    CarnageSkill.bonusPercentPerStack(),
                    CarnageSkill.maxStacks()
            );
        }
        if (SkillCatalog.SUPERNOVA.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    SupernovaSkill.radiusBlocks(level),
                    SupernovaSkill.pullSeconds(level)
            );
        }
        if (SkillCatalog.PROTECTIVE_SHOCKWAVE.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    ProtectiveShockwaveSkill.radiusBlocks(),
                    ProtectiveShockwaveSkill.invulnSeconds()
            );
        }
        if (SkillCatalog.SOUL_RENDER.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    SoulRenderSkill.bonusPercent(level)
            );
        }
        if (SkillCatalog.LIGHT_SPEED.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    LightSpeedSkill.speedBonusPercent(),
                    LightSpeedSkill.DURATION_SECONDS
            );
        }
        if (SkillCatalog.DARKNESS_LIGHT.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    DarknessLightSkill.durationSeconds(level)
            );
        }
        if (SkillCatalog.MINER_FRENZY.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    MinerFrenzySkill.bonusPercent(level),
                    MinerFrenzySkill.durationSeconds(level)
            );
        }
        if (SkillCatalog.LAST_CHANCE.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    LastChanceSkill.cooldownSeconds() / 60
            );
        }
        if (SkillCatalog.REINFORCED_BONES.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    ReinforcedBonesSkill.reductionPercent(level)
            );
        }
        if (SkillCatalog.MIND_FURY.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    MindFurySkill.radiusBlocks(),
                    MindFurySkill.durationSeconds(),
                    MindFurySkill.maxMobs(level)
            );
        }
        if (SkillCatalog.NETHER_ELEVATION.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    NetherElevationSkill.levitationLevel(),
                    NetherElevationSkill.witherLevel(),
                    NetherElevationSkill.durationSeconds(level)
            );
        }
        if (SkillCatalog.NETHER_RUSH.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    NetherRushSkill.dashBlocks(level),
                    NetherRushSkill.explosionCount(level)
            );
        }
        if (SkillCatalog.PROSPECTOR.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    ProspectorSkill.radiusBlocks(level),
                    ProspectorSkill.durationMinutes()
            );
        }
        if (SkillCatalog.FROZEN_MIND.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    FrozenMindSkill.durationSeconds(level)
            );
        }
        if (SkillCatalog.FROZEN_RUSH.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    FrozenRushSkill.dashBlocks(level),
                    FrozenRushSkill.freezeSeconds(),
                    FrozenRushSkill.radiusBlocks()
            );
        }
        if (SkillCatalog.VAMPIRE_BLOOD.equals(def.id())) {
            int bonus = VampireBloodSkill.bonusPercent(level);
            return Component.translatable(def.descKey(), bonus, bonus);
        }
        if (SkillCatalog.QUICK_SURVIVE.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    QuickSurviveSkill.healthThresholdPercent(),
                    QuickSurviveSkill.speedBonusPercent()
            );
        }
        if (SkillCatalog.BLOOD_RUSH.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    BloodRushSkill.bonusPercent(level),
                    BloodRushSkill.BUFF_DURATION_SECONDS,
                    BloodRushSkill.cooldownSeconds(level)
            );
        }
        if (SkillCatalog.STRENGTH_RECOVERY.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    StrengthRecoverySkill.healthThresholdPercent(level),
                    StrengthRecoverySkill.absorptionPercent(level),
                    StrengthRecoverySkill.cooldownSeconds() / 60
            );
        }
        if (SkillCatalog.RECALL.equals(def.id())) {
            return Component.translatable(def.descKey(), RecallSkill.cooldownMinutes());
        }
        if (SkillCatalog.SKY_WALKER.equals(def.id())) {
            return Component.translatable(def.descKey());
        }
        if (SkillCatalog.INFERNO_BOLT.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    InfernoBoltSkill.blastRadius(level),
                    InfernoBoltSkill.boltCount(level)
            );
        }
        if (SkillCatalog.SOUL_LANCE.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    (int) (SoulLanceSkill.damageMultiplier(level) * 100),
                    SoulLanceSkill.lightningCount(level)
            );
        }
        if (SkillCatalog.BLINK_STEP.equals(def.id())) {
            return Component.translatable(def.descKey(), BlinkStepSkill.distanceBlocks(level));
        }
        if (SkillCatalog.CRIPPLING_AURA.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    CripplingAuraSkill.RADIUS_BLOCKS,
                    CripplingAuraSkill.slownessLevel(level),
                    CripplingAuraSkill.weaknessLevel(level),
                    CripplingAuraSkill.DURATION_SECONDS
            );
        }
        if (SkillCatalog.TERROR_PULSE.equals(def.id())) {
            return Component.translatable(
                    def.descKey(),
                    TerrorPulseSkill.RADIUS_BLOCKS,
                    TerrorPulseSkill.durationSeconds()
            );
        }
        return Component.translatable(def.descKey());
    }

    private int displayedCooldownSeconds(SkillDefinition def, int displayLevel) {
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        int remaining = SyncPlayerGloryPacket.ClientGloryState.remainingCooldownTicks(def.id(), gameTime);
        if (remaining > 0) {
            return Math.max(1, (int) Math.ceil(remaining / 20.0));
        }
        return SkillCooldowns.seconds(def.id(), displayLevel);
    }

    private int maxScroll() {
        int count = SkillCatalog.byCategory(page).size();
        int content = Math.max(0, count * (CARD_H + 2) - 2);
        int view = WINDOW_H - 23;
        return Math.max(0, content - view);
    }

    private boolean isInsideInner(double mouseX, double mouseY) {
        return mouseX >= leftPos + 5
                && mouseX < leftPos + WINDOW_W - 5
                && mouseY >= topPos + 18
                && mouseY < topPos + WINDOW_H - 5;
    }

    private boolean isInsideWindow(double mouseX, double mouseY) {
        return mouseX >= leftPos && mouseX < leftPos + WINDOW_W && mouseY >= topPos && mouseY < topPos + WINDOW_H;
    }

    private void drawCentered(GuiGraphics graphics, String text, int x, int y, int width, int color) {
        graphics.drawString(font, text, x + (width - font.width(text)) / 2, y, color, false);
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static Component skillPointsLabel(int points) {
        if (points == 1) {
            return Component.translatable("gui.craftlevel.skills.point.one", points);
        }
        return Component.translatable("gui.craftlevel.skills.point.many", points);
    }

    private static Component costTooltip(int cost) {
        if (cost == 1) {
            return Component.translatable("gui.craftlevel.skill.tooltip.cost.one", cost);
        }
        return Component.translatable("gui.craftlevel.skill.tooltip.cost.many", cost);
    }
}
