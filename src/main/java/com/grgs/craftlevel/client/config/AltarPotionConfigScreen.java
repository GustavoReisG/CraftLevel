package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Configuração de custos e limites da aba Potion. */
@OnlyIn(Dist.CLIENT)
public class AltarPotionConfigScreen extends Screen {

    private static final int ROW_TIME = 0;
    private static final int ROW_STRENGTH = 1;
    private static final int ROW_MAX_TIME = 2;
    private static final int ROW_MAX_STRENGTH = 3;
    private static final int ROW_LIST = 4;
    private static final int DOTS_WIDTH = 28;

    private final Screen parent;

    private EditBox gloryTimeField;
    private EditBox gloryStrengthField;
    private EditBox maxTimeField;
    private EditBox maxStrengthField;

    public AltarPotionConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Altar Potion"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AltarOfDeathPersistence.ensureSession(minecraft);
        AltarOfDeathEditorState state = AltarOfDeathPersistence.session();

        gloryTimeField = numericField(String.valueOf(state.gloryCostTime()));
        ConfigScreenLayout.positionField(this, ROW_TIME, gloryTimeField);
        addRenderableWidget(gloryTimeField);

        gloryStrengthField = numericField(String.valueOf(state.gloryCostStrength()));
        ConfigScreenLayout.positionField(this, ROW_STRENGTH, gloryStrengthField);
        addRenderableWidget(gloryStrengthField);

        maxTimeField = numericField(String.valueOf(state.maxUpgradesTime()));
        ConfigScreenLayout.positionField(this, ROW_MAX_TIME, maxTimeField);
        addRenderableWidget(maxTimeField);

        maxStrengthField = numericField(String.valueOf(state.maxUpgradesStrength()));
        ConfigScreenLayout.positionField(this, ROW_MAX_STRENGTH, maxStrengthField);
        addRenderableWidget(maxStrengthField);

        Button listButton = Button.builder(Component.literal("..."), btn -> openList())
                .bounds(0, 0, DOTS_WIDTH, 20)
                .build();
        ConfigScreenLayout.positionField(this, ROW_LIST, listButton);
        listButton.setWidth(DOTS_WIDTH);
        addRenderableWidget(listButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void openList() {
        flushFieldsToState();
        minecraft.setScreen(new AltarPotionEffectLimitListScreen(this));
    }

    private void flushFieldsToState() {
        AltarOfDeathEditorState state = AltarOfDeathPersistence.session();
        state.setGloryCostTime(parseInt(gloryTimeField.getValue(), state.gloryCostTime()));
        state.setGloryCostStrength(parseInt(gloryStrengthField.getValue(), state.gloryCostStrength()));
        state.setMaxUpgradesTime(parseInt(maxTimeField.getValue(), state.maxUpgradesTime()));
        state.setMaxUpgradesStrength(parseInt(maxStrengthField.getValue(), state.maxUpgradesStrength()));
    }

    private void save() {
        flushFieldsToState();
        AltarOfDeathPersistence.save(minecraft);
    }

    private EditBox numericField(String value) {
        EditBox field = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        field.setMaxLength(16);
        field.setFilter(text -> text.isEmpty() || text.matches("\\d*"));
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

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        ConfigScreenLayout.renderLabel(this, graphics, ROW_TIME, "Glory Cost (Time)");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_STRENGTH, "Glory Cost (Strength)");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_MAX_TIME, "Max Time Upgrades");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_MAX_STRENGTH, "Max Strength Upgrades");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_LIST, "Potions List");
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
