package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Configuração de custos e limites de encantamento do altar. */
@OnlyIn(Dist.CLIENT)
public class AltarImprovementConfigScreen extends Screen {

    private static final int ROW_COST = 0;
    private static final int ROW_GENERIC = 1;
    private static final int ROW_LIST = 2;
    private static final int DOTS_WIDTH = 28;

    private final Screen parent;
    private EditBox enchantmentCostField;
    private EditBox genericLimitField;

    public AltarImprovementConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Altar Improvement"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AltarOfDeathPersistence.ensureSession(minecraft);
        AltarOfDeathEditorState state = AltarOfDeathPersistence.session();

        enchantmentCostField = numericField(String.valueOf(state.improvementGloryCost()));
        ConfigScreenLayout.positionField(this, ROW_COST, enchantmentCostField);
        addRenderableWidget(enchantmentCostField);

        genericLimitField = numericField(String.valueOf(state.genericEnchantmentLimit()));
        ConfigScreenLayout.positionField(this, ROW_GENERIC, genericLimitField);
        addRenderableWidget(genericLimitField);

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
        minecraft.setScreen(new AltarEnchantmentLimitListScreen(this));
    }

    private void flushFieldsToState() {
        AltarOfDeathEditorState state = AltarOfDeathPersistence.session();
        state.setImprovementGloryCost(parseInt(enchantmentCostField.getValue(), state.improvementGloryCost()));
        state.setGenericEnchantmentLimit(parseInt(genericLimitField.getValue(), state.genericEnchantmentLimit()));
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
        ConfigScreenLayout.renderLabel(this, graphics, ROW_COST, "Enchantment Cost");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_GENERIC, "Enchantments Limit");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_LIST, "Enchantment Limit List");
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
