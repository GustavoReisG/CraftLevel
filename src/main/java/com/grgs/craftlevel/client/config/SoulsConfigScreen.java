package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Configuração de souls.json. */
@OnlyIn(Dist.CLIENT)
public class SoulsConfigScreen extends Screen {

    private static final int ROW_MAX_SOULS = 0;
    private static final int ROW_TIME_RESTORE = 1;
    private static final int ROW_EMERALD_TYPE = 2;
    private static final int ROW_EMERALD_QTY = 3;
    private static final int ROW_LOST_ITEMS = 4;
    private static final int ROW_GRAVESTONE = 5;

    private final Screen parent;

    private EditBox maxSoulsField;
    private EditBox timeRestoreField;
    private EditBox emeraldQtyField;
    private Button emeraldTypeButton;
    private Button lostItemsButton;
    private Button gravestoneButton;

    public SoulsConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Souls"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        SoulsPersistence.beginEditing(minecraft);
        SoulsEditorState state = SoulsPersistence.session();

        maxSoulsField = numericField(String.valueOf(state.maxSouls()));
        ConfigScreenLayout.positionField(this, ROW_MAX_SOULS, maxSoulsField);
        addRenderableWidget(maxSoulsField);

        timeRestoreField = numericField(String.valueOf(state.timeRestore()));
        ConfigScreenLayout.positionField(this, ROW_TIME_RESTORE, timeRestoreField);
        addRenderableWidget(timeRestoreField);

        emeraldTypeButton = Button.builder(
                        Component.literal(state.lostEmeraldsType().id()),
                        btn -> {
                            SoulsPersistence.session().cycleLostEmeraldsType();
                            btn.setMessage(Component.literal(SoulsPersistence.session().lostEmeraldsType().id()));
                        })
                .bounds(0, 0, ConfigScreenLayout.fieldWidth(), 20)
                .build();
        ConfigScreenLayout.positionField(this, ROW_EMERALD_TYPE, emeraldTypeButton);
        addRenderableWidget(emeraldTypeButton);

        emeraldQtyField = decimalField(String.valueOf(state.lostEmeraldsQuantity()));
        ConfigScreenLayout.positionField(this, ROW_EMERALD_QTY, emeraldQtyField);
        addRenderableWidget(emeraldQtyField);

        lostItemsButton = Button.builder(
                        Component.literal(state.lostItems().displayName()),
                        btn -> {
                            SoulsPersistence.session().cycleLostItems();
                            btn.setMessage(Component.literal(SoulsPersistence.session().lostItems().displayName()));
                        })
                .bounds(0, 0, ConfigScreenLayout.fieldWidth(), 20)
                .build();
        ConfigScreenLayout.positionField(this, ROW_LOST_ITEMS, lostItemsButton);
        addRenderableWidget(lostItemsButton);

        gravestoneButton = Button.builder(
                        Component.literal(state.gravestone() ? "True" : "False"),
                        btn -> {
                            SoulsPersistence.session().cycleGravestone();
                            btn.setMessage(Component.literal(
                                    SoulsPersistence.session().gravestone() ? "True" : "False"));
                        })
                .bounds(0, 0, ConfigScreenLayout.fieldWidth(), 20)
                .build();
        ConfigScreenLayout.positionField(this, ROW_GRAVESTONE, gravestoneButton);
        addRenderableWidget(gravestoneButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void save() {
        SoulsEditorState state = SoulsPersistence.session();
        state.setMaxSouls(parseInt(maxSoulsField.getValue(), state.maxSouls()));
        state.setTimeRestore(parseInt(timeRestoreField.getValue(), state.timeRestore()));
        state.setLostEmeraldsQuantity(parseDouble(emeraldQtyField.getValue(), state.lostEmeraldsQuantity()));
        SoulsPersistence.save(minecraft);
    }

    private EditBox numericField(String value) {
        EditBox field = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        field.setMaxLength(16);
        field.setFilter(text -> text.isEmpty() || text.matches("\\d*"));
        field.setValue(value);
        return field;
    }

    private EditBox decimalField(String value) {
        EditBox field = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        field.setMaxLength(16);
        field.setFilter(text -> text.isEmpty() || text.matches("\\d*\\.?\\d*"));
        field.setValue(value);
        return field;
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        ConfigScreenLayout.renderLabel(this, graphics, ROW_MAX_SOULS, "Max Souls");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_TIME_RESTORE, "Time Restore (s)");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_EMERALD_TYPE, "Lost Emeralds Type");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_EMERALD_QTY, "Lost Emeralds Qty");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_LOST_ITEMS, "Lost Items");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_GRAVESTONE, "Gravestone");
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
