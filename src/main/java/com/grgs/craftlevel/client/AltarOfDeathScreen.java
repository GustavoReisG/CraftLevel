package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.altar.AltarImprovementService;
import com.grgs.craftlevel.altar.AltarImprovementService.EnchantEntry;
import com.grgs.craftlevel.altar.AltarPotionService;
import com.grgs.craftlevel.config.AltarConversionParser;
import com.grgs.craftlevel.client.AltarGuiIcons.Icon;
import com.grgs.craftlevel.menu.AltarOfDeathMenu;
import com.grgs.craftlevel.menu.AltarTab;
import com.grgs.craftlevel.network.AltarActionPacket;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncPlayerGloryPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Tela do Altar da Morte — abas quadradas, título Immortal, malha de baú A–C. */
@OnlyIn(Dist.CLIENT)
public class AltarOfDeathScreen extends AbstractContainerScreen<AltarOfDeathMenu> {

    private static final int TAB_BORDER = 0xFF2A2A2A;
    private static final int TAB_BORDER_SELECTED = 0xFFFFFFFF;
    private static final float TITLE_SCALE = 1.25f;
    private static final int TITLE_COLOR = 0xFFF0F0F0;
    private static final int IMPROVE_LIST_TEXT = 0xFFC8C8D0;
    private static final int IMPROVE_LIST_SELECTED_BG = 0x80A0A0A0;
    private static final int IMPROVE_LIST_HOVER_BG = 0x40686868;
    private static final int IMPROVE_ITEM_DIM = 0x80000000;
    private static final float IMPROVE_LIST_FONT_SCALE = 0.75f;

    private AltarTab lastTab = AltarTab.CONVERSION;
    private AltarTab hoveredTab;
    private Button convertButton;
    private Button potionTimeButton;
    private Button potionEffectButton;
    private Button improveButton;

    private int improveSelectedIndex = -1;
    private int improveScroll;
    private ItemStack lastImproveStack = ItemStack.EMPTY;

    public AltarOfDeathScreen(AltarOfDeathMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = AltarOfDeathMenu.GUI_WIDTH;
        this.imageHeight = AltarOfDeathMenu.GUI_HEIGHT;
        this.inventoryLabelY = -100;
        this.titleLabelY = -100;
    }

    @Override
    protected void init() {
        super.init();
        this.topPos = (this.height - this.imageHeight) / 2 + AltarOfDeathMenu.CHROME_HEIGHT / 2;
        rebuildAltarWidgets();
    }

    private void rebuildAltarWidgets() {
        clearWidgets();
        convertButton = null;
        potionTimeButton = null;
        potionEffectButton = null;
        improveButton = null;

        AltarTab tab = menu.getActiveTab();
        switch (tab) {
            case CONVERSION -> {
                convertButton = altarIconButton(
                        leftPos + AltarOfDeathMenu.CONVERT_BTN_X,
                        topPos + AltarOfDeathMenu.CONVERT_BTN_Y,
                        () -> sendAction(AltarActionPacket.Action.CONVERT),
                        Tooltip.create(Component.translatable("gui.craftlevel.altar.conversion.hint")));
                addRenderableWidget(convertButton);
                updateConvertButton();
            }
            case POTION -> {
                potionTimeButton = altarIconButton(
                        leftPos + AltarOfDeathMenu.POTION_BTN_X,
                        topPos + AltarOfDeathMenu.POTION_TIME_BTN_Y,
                        () -> sendAction(AltarActionPacket.Action.POTION_TIME),
                        Tooltip.create(Component.translatable("gui.craftlevel.altar.potion.duration_hint"))
                );
                potionEffectButton = altarIconButton(
                        leftPos + AltarOfDeathMenu.POTION_BTN_X,
                        topPos + AltarOfDeathMenu.POTION_EFFECT_BTN_Y,
                        () -> sendAction(AltarActionPacket.Action.POTION_EFFECT),
                        Tooltip.create(Component.translatable("gui.craftlevel.altar.potion.effect_hint"))
                );
                addRenderableWidget(potionTimeButton);
                addRenderableWidget(potionEffectButton);
                updatePotionButtons();
            }
            case IMPROVEMENT -> {
                improveButton = altarIconButton(
                        leftPos + AltarOfDeathMenu.IMPROVE_BTN_X,
                        topPos + AltarOfDeathMenu.IMPROVE_BTN_Y,
                        () -> sendAction(AltarActionPacket.Action.IMPROVE, improveSelectedIndex),
                        Tooltip.create(Component.translatable("gui.craftlevel.altar.improvement.hint"))
                );
                addRenderableWidget(improveButton);
                updateImproveButton();
            }
            case TRADE -> addRenderableWidget(altarIconButton(
                    leftPos + AltarOfDeathMenu.TRADE_BTN_X,
                    topPos + AltarOfDeathMenu.TRADE_BTN_Y,
                    () -> sendAction(AltarActionPacket.Action.TRADE_BUY),
                    null));
        }
    }

    private void sendAction(AltarActionPacket.Action action) {
        sendAction(action, 0);
    }

    private void sendAction(AltarActionPacket.Action action, int data) {
        PacketHandler.CHANNEL.sendToServer(
                new AltarActionPacket(action, menu.getActiveTab().id(), hasShiftDown(), data)
        );
    }

    private Button altarIconButton(int x, int y, Runnable onPress, @javax.annotation.Nullable Tooltip tooltip) {
        AltarIconButton button = new AltarIconButton(x, y, btn -> onPress.run());
        if (tooltip != null) {
            button.setTooltip(tooltip);
        }
        return button;
    }

    private static final class AltarIconButton extends Button {

        private boolean held;

        AltarIconButton(int x, int y, OnPress onPress) {
            super(
                    x,
                    y,
                    AltarGuiIcons.WIDGET_SIZE,
                    AltarGuiIcons.WIDGET_SIZE,
                    Component.empty(),
                    onPress,
                    supplier -> Component.empty()
            );
        }

        void clearHeld() {
            held = false;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (this.active) {
                held = true;
            }
            super.onClick(mouseX, mouseY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            held = false;
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            AltarGuiIcons.blitButton(graphics, getX(), getY(), isHovered(), this.active, this.active && this.held);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        AltarTab current = menu.getActiveTab();
        if (current != lastTab) {
            lastTab = current;
            improveSelectedIndex = -1;
            improveScroll = 0;
            lastImproveStack = ItemStack.EMPTY;
            rebuildAltarWidgets();
        } else if (current == AltarTab.CONVERSION) {
            updateConvertButton();
        } else if (current == AltarTab.POTION) {
            updatePotionButtons();
        } else if (current == AltarTab.IMPROVEMENT) {
            syncImproveSelection();
            updateImproveButton();
        }
    }

    private void updateConvertButton() {
        if (convertButton == null) {
            return;
        }
        ItemStack input = menu.getConversionStack();
        convertButton.active = AltarConversionParser.findMatch(input).isPresent();
    }

    private void syncImproveSelection() {
        ItemStack stack = menu.getImprovementStack();
        if (!ItemStack.isSameItemSameTags(stack, lastImproveStack)) {
            lastImproveStack = stack.copy();
            improveSelectedIndex = -1;
            improveScroll = 0;
        }
        List<EnchantEntry> entries = AltarImprovementService.listEnchantments(stack);
        if (improveSelectedIndex >= entries.size()) {
            improveSelectedIndex = -1;
        }
        improveScroll = Mth.clamp(improveScroll, 0, maxImproveScroll(entries.size()));
    }

    private void updatePotionButtons() {
        if (potionTimeButton == null || potionEffectButton == null) {
            return;
        }
        ItemStack input = menu.getPotionStack();
        boolean outputEmpty = menu.getPotionOutputStack().isEmpty();
        int glory = SyncPlayerGloryPacket.ClientGloryState.gloryKills;

        potionTimeButton.active = outputEmpty
                && glory >= AltarPotionService.gloryCostTime()
                && AltarPotionService.canExtendDuration(input);
        potionEffectButton.active = outputEmpty
                && glory >= AltarPotionService.gloryCostStrength()
                && AltarPotionService.canUpgradeEffect(input);
    }

    private void updateImproveButton() {
        if (improveButton == null) {
            return;
        }
        ItemStack input = menu.getImprovementStack();
        List<EnchantEntry> entries = AltarImprovementService.listEnchantments(input);
        boolean hasGlory = SyncPlayerGloryPacket.ClientGloryState.gloryKills >= AltarImprovementService.gloryCost();
        boolean selectionValid = improveSelectedIndex >= 0 && improveSelectedIndex < entries.size();
        boolean canUpgrade = selectionValid
                && AltarImprovementService.canUpgrade(
                        entries.get(improveSelectedIndex).enchantment(),
                        entries.get(improveSelectedIndex).level()
                );
        improveButton.active = hasGlory && canUpgrade;
    }

    private boolean isPotionClockIconEnabled() {
        ItemStack input = menu.getPotionStack();
        return !AltarPotionService.hasEffects(input) || !AltarPotionService.isInstantOnlyPotion(input);
    }

    private boolean isPotionStrengthIconEnabled() {
        ItemStack input = menu.getPotionStack();
        return !AltarPotionService.hasEffects(input)
                || AltarPotionService.hasAmplifierUpgradeableEffects(input);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        clearAltarButtonHeld();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void clearAltarButtonHeld() {
        for (var child : children()) {
            if (child instanceof AltarIconButton b) {
                b.clearHeld();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        AltarTab clicked = tabAt(mouseX, mouseY);
        if (clicked != null) {
            if (menu.getActiveTab() != clicked) {
                PacketHandler.CHANNEL.sendToServer(
                        new AltarActionPacket(AltarActionPacket.Action.SWITCH_TAB, clicked.id(), false));
            }
            return true;
        }
        if (menu.getActiveTab() == AltarTab.IMPROVEMENT && button == 0 && clickImproveList(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (menu.getActiveTab() == AltarTab.IMPROVEMENT && isInsideImproveList(mouseX, mouseY)) {
            List<EnchantEntry> entries = AltarImprovementService.listEnchantments(menu.getImprovementStack());
            int max = maxImproveScroll(entries.size());
            if (max > 0) {
                improveScroll = Mth.clamp(improveScroll - (int) Math.signum(delta), 0, max);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean clickImproveList(double mouseX, double mouseY) {
        if (!isInsideImproveList(mouseX, mouseY)) {
            return false;
        }
        List<EnchantEntry> entries = AltarImprovementService.listEnchantments(menu.getImprovementStack());
        if (entries.isEmpty()) {
            return true;
        }
        int listY = topPos + AltarOfDeathMenu.IMPROVE_LIST_Y;
        int localY = (int) mouseY - listY + improveScroll * AltarOfDeathMenu.IMPROVE_LIST_ROW_H;
        int index = localY / AltarOfDeathMenu.IMPROVE_LIST_ROW_H;
        if (index >= 0 && index < entries.size()) {
            improveSelectedIndex = index;
            updateImproveButton();
        }
        return true;
    }

    private boolean isInsideImproveList(double mouseX, double mouseY) {
        int x = leftPos + AltarOfDeathMenu.IMPROVE_LIST_X;
        int y = topPos + AltarOfDeathMenu.IMPROVE_LIST_Y;
        return mouseX >= x
                && mouseX < x + AltarOfDeathMenu.IMPROVE_LIST_W
                && mouseY >= y
                && mouseY < y + AltarOfDeathMenu.IMPROVE_LIST_H;
    }

    private static int maxImproveScroll(int entryCount) {
        int visible = AltarOfDeathMenu.IMPROVE_LIST_H / AltarOfDeathMenu.IMPROVE_LIST_ROW_H;
        return Math.max(0, entryCount - visible);
    }

    private AltarTab tabAt(double mouseX, double mouseY) {
        int tabY = AltarOfDeathMenu.tabButtonScreenY(topPos);
        int size = AltarOfDeathMenu.TAB_BUTTON_SIZE;
        for (AltarTab tab : AltarTab.values()) {
            int tabX = AltarOfDeathMenu.tabButtonScreenX(leftPos, tab);
            if (mouseX >= tabX && mouseX < tabX + size && mouseY >= tabY && mouseY < tabY + size) {
                return tab;
            }
        }
        return null;
    }

    private static ResourceLocation textureFor(AltarTab tab) {
        return new ResourceLocation(CraftLevel.MODID, "textures/gui/" + tab.textureName() + ".png");
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(graphics);
        ResourceLocation texture = textureFor(menu.getActiveTab());
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        hoveredTab = tabAt(mouseX, mouseY);
        drawTabButtons(graphics);
        drawScreenTitle(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderActionIcons(graphics);
        renderConversionCost(graphics);
        renderPotionGloryCost(graphics);
        renderImprovementGloryCost(graphics);
        renderImprovementList(graphics, mouseX, mouseY);
        renderImprovementItemDim(graphics);
        renderTradeStub(graphics);
        AltarGloryOverlay.renderTopRight(graphics, font, width);
        renderTabTooltip(graphics);
        renderTooltip(graphics, mouseX, mouseY);
        renderImprovementEnchantTooltip(graphics, mouseX, mouseY);
    }

    private void drawTabButtons(GuiGraphics graphics) {
        AltarTab active = menu.getActiveTab();
        int tabY = AltarOfDeathMenu.tabButtonScreenY(topPos);
        int size = AltarOfDeathMenu.TAB_BUTTON_SIZE;
        int iconInset = 7;
        int iconSize = size - iconInset * 2;

        for (AltarTab tab : AltarTab.values()) {
            int tabX = AltarOfDeathMenu.tabButtonScreenX(leftPos, tab);
            boolean selected = tab == active;
            int accent = tabAccent(tab);
            int bg = selected ? blend(0xFF3A3A48, accent, 0.45f) : blend(0xFF2C2C36, accent, 0.28f);
            int border = selected ? TAB_BORDER_SELECTED : TAB_BORDER;

            graphics.fill(tabX - 2, tabY - 2, tabX + size + 2, tabY + size + 2, border);
            graphics.fill(tabX - 1, tabY - 1, tabX + size + 1, tabY + size + 1, accent);
            graphics.fill(tabX, tabY, tabX + size, tabY + size, bg);
            graphics.fill(tabX + 2, tabY + size - 3, tabX + size - 2, tabY + size - 1, accent);
            AltarTabIcons.blitTabIcon(graphics, tabX + iconInset, tabY + iconInset, iconSize, tab);
        }
    }

    private static int tabAccent(AltarTab tab) {
        return AltarScrambleTooltip.tabColor(tab);
    }

    private static int blend(int baseRgb, int accentRgb, float accentWeight) {
        float baseWeight = 1.0f - accentWeight;
        int br = (baseRgb >> 16) & 0xFF;
        int bg = (baseRgb >> 8) & 0xFF;
        int bb = baseRgb & 0xFF;
        int ar = (accentRgb >> 16) & 0xFF;
        int ag = (accentRgb >> 8) & 0xFF;
        int ab = accentRgb & 0xFF;
        int r = (int) (br * baseWeight + ar * accentWeight);
        int g = (int) (bg * baseWeight + ag * accentWeight);
        int b = (int) (bb * baseWeight + ab * accentWeight);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void drawScreenTitle(GuiGraphics graphics) {
        if (hoveredTab != null) {
            return;
        }
        String title = Component.translatable(menu.getActiveTab().titleKey()).getString();
        int centerX = leftPos + imageWidth / 2;
        int centerY = AltarOfDeathMenu.titleScreenY(topPos) + AltarOfDeathMenu.TITLE_HEIGHT / 2;
        HudTextHelper.drawCenteredAt(graphics, font, title, centerX, centerY, TITLE_SCALE, TITLE_COLOR, true);
    }

    private void renderTabTooltip(GuiGraphics graphics) {
        if (hoveredTab == null) {
            return;
        }
        int tabY = AltarOfDeathMenu.tabButtonScreenY(topPos);
        int size = AltarOfDeathMenu.TAB_BUTTON_SIZE;
        int tabX = AltarOfDeathMenu.tabButtonScreenX(leftPos, hoveredTab);
        int centerX = tabX + size / 2;
        int belowY = tabY + size + 4;
        AltarScrambleTooltip.renderBelow(
                graphics,
                font,
                Component.translatable(hoveredTab.titleKey()),
                centerX,
                belowY,
                tabAccent(hoveredTab)
        );
    }

    /** Altura visual usada pelo vanilla para centralizar texto de botão. */
    private static final int GLORY_TEXT_HEIGHT = 8;

    private void renderConversionCost(GuiGraphics graphics) {
        if (menu.getActiveTab() != AltarTab.CONVERSION) {
            return;
        }
        ItemStack input = menu.getConversionStack();
        if (input.isEmpty()) {
            return;
        }
        AltarConversionParser.findMatch(input).ifPresent(rule -> drawCenteredGloryCost(
                graphics,
                Component.translatable("gui.craftlevel.altar.glory_cost", rule.gloryCost()),
                leftPos + AltarOfDeathMenu.CONVERSION_COST_X,
                topPos + AltarOfDeathMenu.CONVERSION_COST_Y,
                AltarOfDeathMenu.CONVERSION_COST_W,
                AltarOfDeathMenu.CONVERSION_COST_H,
                0x606060
        ));
    }

    private void renderPotionGloryCost(GuiGraphics graphics) {
        if (menu.getActiveTab() != AltarTab.POTION) {
            return;
        }
        drawCenteredGloryCost(
                graphics,
                Component.translatable(
                        "gui.craftlevel.altar.glory_cost",
                        AltarPotionService.gloryCostTime()
                ),
                leftPos + AltarOfDeathMenu.POTION_COST_X,
                topPos + AltarOfDeathMenu.POTION_COST_Y,
                AltarOfDeathMenu.POTION_COST_W,
                AltarOfDeathMenu.POTION_COST_H,
                0xFFFFFF
        );
    }

    private void renderImprovementGloryCost(GuiGraphics graphics) {
        if (menu.getActiveTab() != AltarTab.IMPROVEMENT) {
            return;
        }
        drawCenteredGloryCost(
                graphics,
                Component.translatable(
                        "gui.craftlevel.altar.glory_cost",
                        AltarImprovementService.gloryCost()
                ),
                leftPos + AltarOfDeathMenu.IMPROVE_COST_X,
                topPos + AltarOfDeathMenu.IMPROVE_COST_Y,
                AltarOfDeathMenu.IMPROVE_COST_W,
                AltarOfDeathMenu.IMPROVE_COST_H,
                0xFFFFFF
        );
    }

    private void drawCenteredGloryCost(
            GuiGraphics graphics,
            Component cost,
            int boxX,
            int boxY,
            int boxW,
            int boxH,
            int color
    ) {
        int textW = font.width(cost);
        int textX = boxX + (boxW - textW) / 2;
        int textY = boxY + (boxH - GLORY_TEXT_HEIGHT) / 2;
        graphics.drawString(font, cost, textX, textY, color, false);
    }

    private void renderImprovementList(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.getActiveTab() != AltarTab.IMPROVEMENT) {
            return;
        }

        List<EnchantEntry> entries = AltarImprovementService.listEnchantments(menu.getImprovementStack());
        int listX = leftPos + AltarOfDeathMenu.IMPROVE_LIST_X;
        int listY = topPos + AltarOfDeathMenu.IMPROVE_LIST_Y;
        int listW = AltarOfDeathMenu.IMPROVE_LIST_W;
        int listH = AltarOfDeathMenu.IMPROVE_LIST_H;
        int rowH = AltarOfDeathMenu.IMPROVE_LIST_ROW_H;
        float fontScale = IMPROVE_LIST_FONT_SCALE;
        int scaledFontH = Math.round(font.lineHeight * fontScale);

        if (entries.isEmpty()) {
            if (!menu.getImprovementStack().isEmpty()) {
                drawImproveListString(
                        graphics,
                        Component.translatable("gui.craftlevel.altar.improvement.empty"),
                        listX + 4,
                        listY + (listH - scaledFontH) / 2,
                        0x808080
                );
            }
            return;
        }

        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = 0; i < entries.size(); i++) {
            int rowY = listY + (i - improveScroll) * rowH;
            if (rowY + rowH < listY || rowY >= listY + listH) {
                continue;
            }
            boolean selected = i == improveSelectedIndex;
            boolean hovered = mouseX >= listX
                    && mouseX < listX + listW
                    && mouseY >= Math.max(rowY, listY)
                    && mouseY < Math.min(rowY + rowH, listY + listH);
            if (selected) {
                graphics.fill(listX, rowY, listX + listW, rowY + rowH, IMPROVE_LIST_SELECTED_BG);
            } else if (hovered) {
                graphics.fill(listX, rowY, listX + listW, rowY + rowH, IMPROVE_LIST_HOVER_BG);
            }
            drawImproveListString(
                    graphics,
                    entries.get(i).displayName(),
                    listX + 3,
                    rowY + (rowH - scaledFontH) / 2,
                    IMPROVE_LIST_TEXT
            );
        }
        graphics.disableScissor();
    }

    private void renderImprovementEnchantTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = hoveredImproveIndex(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        List<EnchantEntry> entries = AltarImprovementService.listEnchantments(menu.getImprovementStack());
        if (index >= entries.size()) {
            return;
        }
        graphics.renderTooltip(font, entries.get(index).displayName(), mouseX, mouseY);
    }

    private int hoveredImproveIndex(int mouseX, int mouseY) {
        if (menu.getActiveTab() != AltarTab.IMPROVEMENT || !isInsideImproveList(mouseX, mouseY)) {
            return -1;
        }
        List<EnchantEntry> entries = AltarImprovementService.listEnchantments(menu.getImprovementStack());
        if (entries.isEmpty()) {
            return -1;
        }
        int listY = topPos + AltarOfDeathMenu.IMPROVE_LIST_Y;
        int rowH = AltarOfDeathMenu.IMPROVE_LIST_ROW_H;
        int index = (mouseY - listY) / rowH + improveScroll;
        if (index < 0 || index >= entries.size()) {
            return -1;
        }
        int rowY = listY + (index - improveScroll) * rowH;
        if (rowY + rowH < listY || rowY >= listY + AltarOfDeathMenu.IMPROVE_LIST_H) {
            return -1;
        }
        return index;
    }

    private void drawImproveListString(GuiGraphics graphics, Component text, int x, int y, int color) {
        float scale = IMPROVE_LIST_FONT_SCALE;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void renderImprovementItemDim(GuiGraphics graphics) {
        if (menu.getActiveTab() != AltarTab.IMPROVEMENT || improveSelectedIndex < 0) {
            return;
        }
        if (menu.getImprovementStack().isEmpty()) {
            return;
        }
        int x = leftPos + AltarOfDeathMenu.IMPROVE_SLOT_X;
        int y = topPos + AltarOfDeathMenu.IMPROVE_SLOT_Y;
        graphics.fill(x, y, x + 16, y + 16, IMPROVE_ITEM_DIM);
    }

    private void renderTradeStub(GuiGraphics graphics) {
        if (menu.getActiveTab() != AltarTab.TRADE) {
            return;
        }
        graphics.drawString(font, Component.translatable("gui.craftlevel.altar.trade.list_stub"),
                leftPos + 28, topPos + 36, 0x808080, false);
    }

    private void renderActionIcons(GuiGraphics graphics) {
        switch (menu.getActiveTab()) {
            case CONVERSION -> AltarGuiIcons.blitIcon(
                    graphics,
                    leftPos + AltarOfDeathMenu.CONVERT_BTN_X,
                    topPos + AltarOfDeathMenu.CONVERT_BTN_Y,
                    Icon.CONVERSION);
            case POTION -> {
                AltarGuiIcons.blitIcon(
                        graphics,
                        leftPos + AltarOfDeathMenu.POTION_BTN_X,
                        topPos + AltarOfDeathMenu.POTION_TIME_BTN_Y,
                        Icon.POTION_TIME,
                        isPotionClockIconEnabled());
                AltarGuiIcons.blitIcon(
                        graphics,
                        leftPos + AltarOfDeathMenu.POTION_BTN_X,
                        topPos + AltarOfDeathMenu.POTION_EFFECT_BTN_Y,
                        Icon.POTION_STRENGTH,
                        isPotionStrengthIconEnabled());
            }
            case IMPROVEMENT -> AltarGuiIcons.blitIcon(
                    graphics,
                    leftPos + AltarOfDeathMenu.IMPROVE_BTN_X,
                    topPos + AltarOfDeathMenu.IMPROVE_BTN_Y,
                    Icon.IMPROVEMENT);
            default -> {
            }
        }
    }

}
