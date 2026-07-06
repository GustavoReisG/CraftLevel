package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Edição de uma variável em scaling.{nome}. */
@OnlyIn(Dist.CLIENT)
public class ScalingVariableConfigScreen extends Screen {

    private static final int ROW_NAME = 0;
    private static final int ROW_SCALING_START = 1;

    private final Screen parent;
    private final String originalName;

    private EditBox nameField;
    private ConfigScalingForm scalingForm;

    protected ScalingVariableConfigScreen(Screen parent, String variableName) {
        super(Component.literal("CraftLevel - " + variableName));
        this.parent = parent;
        this.originalName = variableName;
    }

    @Override
    protected void init() {
        ConfigEditorState state = ConfigPersistence.session();
        ConfigEditorState.ScalerEntry entry = state.scaler(originalName);
        if (entry == null) {
            entry = ConfigEditorState.ScalerEntry.empty();
        }

        nameField = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        nameField.setValue(originalName);
        nameField.setMaxLength(64);
        ConfigScreenLayout.positionField(this, ROW_NAME, nameField);
        addRenderableWidget(nameField);

        scalingForm = new ConfigScalingForm(font, entry);
        scalingForm.positionRows(this, ROW_SCALING_START);
        addRenderableWidget(scalingForm.baseValueField());
        addRenderableWidget(scalingForm.scalingField());
        addRenderableWidget(scalingForm.scalingModeButton());
        addRenderableWidget(scalingForm.applyModeButton());
        addRenderableWidget(scalingForm.includeModifiersButton());

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void save() {
        String newName = nameField.getValue().trim();
        if (newName.isEmpty()) {
            return;
        }

        ConfigEditorState state = ConfigPersistence.session();
        ConfigEditorState.ScalerEntry entry = scalingForm.readScalerEntry();
        state.renameScaler(originalName, newName, entry);
        ConfigPersistence.save(minecraft);
        returnToList();
    }

    private void returnToList() {
        if (parent instanceof ScalingVariablesScreen variablesScreen) {
            minecraft.setScreen(new ScalingVariablesScreen(variablesScreen.getMenuParent()));
        } else {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        returnToList();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        ConfigScreenLayout.renderLabel(this, graphics, ROW_NAME, "Name");
        scalingForm.renderLabels(this, graphics, ROW_SCALING_START);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
