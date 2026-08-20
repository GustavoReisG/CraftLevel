package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.client.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Configuração do HUD de skills ativas (carrossel). */
public class SkillsHudConfigScreen extends Screen {

    private final Screen parent;

    private CycleButton<Boolean> enabledButton;
    private CycleButton<String> directionButton;
    private CycleButton<String> fillSenseButton;
    private CycleButton<String> textSenseButton;
    private CycleButton<String> anchorButton;
    private EditBox xField;
    private EditBox yField;
    private EditBox scaleField;

    public SkillsHudConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Skills HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ClientConfigPersistence.beginEditing(minecraft);
        ClientConfigEditorState state = ClientConfigPersistence.session();
        ClientConfigEditorState.SkillsHudValues values = state.resolvedSkills();

        int row = 0;
        int fieldW = ConfigScreenLayout.fieldWidth();

        enabledButton = CycleButton.onOffBuilder()
                .withInitialValue(Boolean.TRUE.equals(values.enabled()))
                .displayOnlyValue()
                .create(0, 0, fieldW, 20, Component.literal("Enable Skills HUD"));
        ConfigScreenLayout.positionField(this, row++, enabledButton);
        addRenderableWidget(enabledButton);

        directionButton = CycleButton.<String>builder(Component::literal)
                .withValues(ClientConfigEditorState.SKILLS_DIRECTIONS)
                .withInitialValue(find(ClientConfigEditorState.SKILLS_DIRECTIONS, values.direction(), "row"))
                .displayOnlyValue()
                .create(0, 0, fieldW, 20, Component.empty());
        ConfigScreenLayout.positionField(this, row++, directionButton);
        addRenderableWidget(directionButton);

        fillSenseButton = CycleButton.<String>builder(Component::literal)
                .withValues(ClientConfigEditorState.SKILLS_FILL_SENSES)
                .withInitialValue(find(ClientConfigEditorState.SKILLS_FILL_SENSES, values.fillSense(), "ascending"))
                .displayOnlyValue()
                .create(0, 0, fieldW, 20, Component.empty());
        ConfigScreenLayout.positionField(this, row++, fillSenseButton);
        addRenderableWidget(fillSenseButton);

        textSenseButton = CycleButton.<String>builder(Component::literal)
                .withValues(ClientConfigEditorState.SKILLS_TEXT_SENSES)
                .withInitialValue(find(ClientConfigEditorState.SKILLS_TEXT_SENSES, values.textSense(), "after_icon"))
                .displayOnlyValue()
                .create(0, 0, fieldW, 20, Component.empty());
        ConfigScreenLayout.positionField(this, row++, textSenseButton);
        addRenderableWidget(textSenseButton);

        anchorButton = CycleButton.<String>builder(Component::literal)
                .withValues(ClientConfig.ANCHORS)
                .withInitialValue(find(ClientConfig.ANCHORS, values.anchor(), "top_right"))
                .displayOnlyValue()
                .create(0, 0, fieldW, 20, Component.empty());
        ConfigScreenLayout.positionField(this, row++, anchorButton);
        addRenderableWidget(anchorButton);

        xField = new EditBox(font, 0, 0, fieldW, 20, Component.literal("X"));
        xField.setValue(String.valueOf(values.x() != null ? values.x() : -8));
        ConfigScreenLayout.positionField(this, row++, xField);
        addRenderableWidget(xField);

        yField = new EditBox(font, 0, 0, fieldW, 20, Component.literal("Y"));
        yField.setValue(String.valueOf(values.y() != null ? values.y() : 40));
        ConfigScreenLayout.positionField(this, row++, yField);
        addRenderableWidget(yField);

        scaleField = new EditBox(font, 0, 0, fieldW, 20, Component.literal("Scale"));
        scaleField.setValue(String.valueOf(values.scale() != null ? values.scale() : 0.85f));
        ConfigScreenLayout.positionField(this, row++, scaleField);
        addRenderableWidget(scaleField);

        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
        pushLive();
    }

    @Override
    public void tick() {
        super.tick();
        pushLive();
    }

    private void pushLive() {
        ClientConfigEditorState state = ClientConfigPersistence.session();
        state.setSkills(readValues());
        ClientConfig.applyEditorState(state);
    }

    private ClientConfigEditorState.SkillsHudValues readValues() {
        return new ClientConfigEditorState.SkillsHudValues(
                enabledButton.getValue(),
                directionButton.getValue(),
                fillSenseButton.getValue(),
                textSenseButton.getValue(),
                anchorButton.getValue(),
                parseInt(xField.getValue(), -8),
                parseInt(yField.getValue(), 40),
                parseFloat(scaleField.getValue(), 0.85f)
        );
    }

    @Override
    public void onClose() {
        ClientConfigEditorState state = ClientConfigPersistence.session();
        state.setSkills(readValues());
        ClientConfigPersistence.save(minecraft);
        ClientConfig.applyEditorState(state);
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        ConfigScreenLayout.renderLabel(this, graphics, 0, "Enabled");
        ConfigScreenLayout.renderLabel(this, graphics, 1, "Direction");
        ConfigScreenLayout.renderLabel(this, graphics, 2, "Fill Sense");
        ConfigScreenLayout.renderLabel(this, graphics, 3, "Text Sense");
        ConfigScreenLayout.renderLabel(this, graphics, 4, "Anchor");
        ConfigScreenLayout.renderLabel(this, graphics, 5, "X");
        ConfigScreenLayout.renderLabel(this, graphics, 6, "Y");
        ConfigScreenLayout.renderLabel(this, graphics, 7, "Scale");
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String find(String[] values, String current, String fallback) {
        if (current != null) {
            for (String v : values) {
                if (v.equals(current)) {
                    return v;
                }
            }
        }
        return fallback;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static float parseFloat(String raw, float fallback) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
