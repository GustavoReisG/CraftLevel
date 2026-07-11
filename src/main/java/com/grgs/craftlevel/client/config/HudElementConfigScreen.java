package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.client.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Edição de um elemento do HUD (LP Bar ou Level Circle).
 * Estrutura definida pelo mod; valores importados de client.json.
 */
public class HudElementConfigScreen extends Screen {

    public enum Element {
        LP_BAR("LP Bar"),
        LEVEL_CIRCLE("Level Circle"),
        HEALTH_BAR("Health Bar"),
        HUNGER_BAR("Hunger Bar");

        private final String title;

        Element(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    private final Screen parent;
    private final Element element;

    private CycleButton<String> anchorButton;
    private CycleButton<Boolean> enabledButton;
    private EditBox xField;
    private EditBox yField;
    private EditBox scaleField;
    private EditBox fontScaleField;
    private EditBox marginField;
    private EditBox mountHealthMarginField;

    private int rowEnabled = -1;
    private int rowAnchor;
    private int rowX;
    private int rowY;
    private int rowScale;
    private int rowFontScale = -1;
    private int rowMargin = -1;
    private int rowMountHealthMargin = -1;

    protected HudElementConfigScreen(Screen parent, Element element) {
        super(Component.literal("CraftLevel - " + element.title()));
        this.parent = parent;
        this.element = element;
    }

    @Override
    protected void init() {
        ClientConfigEditorState state = ClientConfigPersistence.session();
        int row = 0;

        if (element == Element.HEALTH_BAR || element == Element.HUNGER_BAR) {
            rowEnabled = row;
            boolean enabled = element == Element.HEALTH_BAR
                    ? state.resolvedHealthBar().enabled()
                    : state.resolvedHungerBar().enabled();
            String label = element == Element.HEALTH_BAR ? "Enable CL Health Bar" : "Enable CL Hunger Bar";
            enabledButton = CycleButton.onOffBuilder()
                    .withInitialValue(enabled)
                    .displayOnlyValue()
                    .create(0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.literal(label));
            ConfigScreenLayout.positionField(this, row++, enabledButton);
            addRenderableWidget(enabledButton);
        }

        ClientConfigEditorState.HudElementValues values = switch (element) {
            case LP_BAR -> state.lpBar();
            case LEVEL_CIRCLE -> state.levelCircle();
            case HEALTH_BAR -> {
                ClientConfigEditorState.HealthBarValues healthBar = state.resolvedHealthBar();
                yield new ClientConfigEditorState.HudElementValues(
                        healthBar.anchor(), healthBar.x(), healthBar.y(), healthBar.scale(), healthBar.fontScale());
            }
            case HUNGER_BAR -> {
                ClientConfigEditorState.HungerBarValues hungerBar = state.resolvedHungerBar();
                yield new ClientConfigEditorState.HudElementValues(
                        hungerBar.anchor(), hungerBar.x(), hungerBar.y(), hungerBar.scale(), hungerBar.fontScale());
            }
        };

        rowAnchor = row;

        String anchor = values.anchor() != null ? values.anchor() : ClientConfig.ANCHORS[0];
        anchorButton = CycleButton.<String>builder(Component::literal)
                .withValues(ClientConfig.ANCHORS)
                .withInitialValue(findAnchor(anchor))
                .displayOnlyValue()
                .create(0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        ConfigScreenLayout.positionField(this, row++, anchorButton);
        addRenderableWidget(anchorButton);

        rowX = row;
        xField = newIntField(values.x());
        ConfigScreenLayout.positionField(this, row++, xField);
        addRenderableWidget(xField);

        rowY = row;
        yField = newIntField(values.y());
        ConfigScreenLayout.positionField(this, row++, yField);
        addRenderableWidget(yField);

        rowScale = row;
        scaleField = newFloatField(values.scale());
        ConfigScreenLayout.positionField(this, row++, scaleField);
        addRenderableWidget(scaleField);

        if (element == Element.LEVEL_CIRCLE || element == Element.HEALTH_BAR
                || element == Element.LP_BAR || element == Element.HUNGER_BAR) {
            rowFontScale = row;
            fontScaleField = newFloatField(values.fontScale());
            ConfigScreenLayout.positionField(this, row++, fontScaleField);
            addRenderableWidget(fontScaleField);
        }

        if (element == Element.HEALTH_BAR || element == Element.HUNGER_BAR) {
            rowMargin = row;
            Integer margin = element == Element.HEALTH_BAR
                    ? state.resolvedHealthBar().armorMargin()
                    : state.resolvedHungerBar().breathMargin();
            marginField = newIntField(margin);
            ConfigScreenLayout.positionField(this, row++, marginField);
            addRenderableWidget(marginField);
        }

        if (element == Element.HUNGER_BAR) {
            rowMountHealthMargin = row;
            Integer mountMargin = state.resolvedHungerBar().mountHealthMargin();
            if (mountMargin == null) {
                mountMargin = state.resolvedHungerBar().breathMargin();
            }
            mountHealthMarginField = newIntField(mountMargin);
            ConfigScreenLayout.positionField(this, row++, mountHealthMarginField);
            addRenderableWidget(mountHealthMarginField);
        }

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private EditBox newIntField(Integer value) {
        EditBox box = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        if (value != null) {
            box.setValue(String.valueOf(value));
        }
        box.setMaxLength(32);
        box.setFilter(text -> text.isEmpty() || text.matches("-?\\d*"));
        return box;
    }

    private EditBox newFloatField(Float value) {
        EditBox box = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        if (value != null) {
            box.setValue(String.valueOf(value));
        }
        box.setMaxLength(32);
        box.setFilter(text -> text.isEmpty() || text.matches("-?\\d*\\.?\\d*"));
        return box;
    }

    private static String findAnchor(String anchor) {
        for (String option : ClientConfig.ANCHORS) {
            if (option.equals(anchor)) {
                return option;
            }
        }
        return ClientConfig.ANCHORS[0];
    }

    private void save() {
        ClientConfigEditorState.HudElementValues values = new ClientConfigEditorState.HudElementValues(
                anchorButton.getValue(),
                ConfigScalingForm.parseOptionalInt(xField),
                ConfigScalingForm.parseOptionalInt(yField),
                parseOptionalFloat(scaleField),
                fontScaleField != null ? parseOptionalFloat(fontScaleField) : null
        );

        ClientConfigEditorState session = ClientConfigPersistence.session();
        if (element == Element.LP_BAR) {
            session.setLpBar(values);
        } else if (element == Element.LEVEL_CIRCLE) {
            session.setLevelCircle(values);
        } else if (element == Element.HEALTH_BAR) {
            session.setHealthBar(new ClientConfigEditorState.HealthBarValues(
                    enabledButton.getValue(),
                    anchorButton.getValue(),
                    values.x(),
                    values.y(),
                    values.scale(),
                    values.fontScale(),
                    ConfigScalingForm.parseOptionalInt(marginField)
            ));
        } else {
            session.setHungerBar(new ClientConfigEditorState.HungerBarValues(
                    enabledButton.getValue(),
                    anchorButton.getValue(),
                    values.x(),
                    values.y(),
                    values.scale(),
                    values.fontScale(),
                    ConfigScalingForm.parseOptionalInt(marginField),
                    mountHealthMarginField != null
                            ? ConfigScalingForm.parseOptionalInt(mountHealthMarginField)
                            : null
            ));
        }
        ClientConfigPersistence.save(minecraft);
    }

    private static Float parseOptionalFloat(EditBox field) {
        String text = field.getValue().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(text.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
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
        if (rowEnabled >= 0) {
            String enabledLabel = element == Element.HEALTH_BAR ? "Enable CL Health Bar" : "Enable CL Hunger Bar";
            ConfigScreenLayout.renderLabel(this, graphics, rowEnabled, enabledLabel);
        }
        ConfigScreenLayout.renderLabel(this, graphics, rowAnchor, "Anchor");
        ConfigScreenLayout.renderLabel(this, graphics, rowX, "Pos X");
        ConfigScreenLayout.renderLabel(this, graphics, rowY, "Pos Y");
        ConfigScreenLayout.renderLabel(this, graphics, rowScale, "Scale");
        if (rowFontScale >= 0) {
            ConfigScreenLayout.renderLabel(this, graphics, rowFontScale, "Font Scale");
        }
        if (rowMargin >= 0) {
            String marginLabel = element == Element.HEALTH_BAR ? "Armor Margin" : "Breath Margin";
            ConfigScreenLayout.renderLabel(this, graphics, rowMargin, marginLabel);
        }
        if (rowMountHealthMargin >= 0) {
            ConfigScreenLayout.renderLabel(this, graphics, rowMountHealthMargin, "Mount Health Margin");
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
