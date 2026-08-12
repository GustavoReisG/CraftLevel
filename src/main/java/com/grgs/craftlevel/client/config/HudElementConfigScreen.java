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
        HUNGER_BAR("Hunger Bar"),
        EMERALDS("Emeralds"),
        SOULS("Souls"),
        GLORY_KILLS("Glory Kills"),
        MOB_FOCUS("Mob Focus");

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
    private CycleButton<String> iconSideButton;
    private CycleButton<String> directionButton;
    private EditBox xField;
    private EditBox yField;
    private EditBox scaleField;
    private EditBox fontScaleField;
    private EditBox fontYOffsetField;
    private EditBox marginField;
    private EditBox mountHealthMarginField;
    private EditBox mobYawField;
    private EditBox headYawField;
    private EditBox cameraPitchField;

    private int rowEnabled = -1;
    private int rowAnchor;
    private int rowX;
    private int rowY;
    private int rowScale;
    private int rowFontScale = -1;
    private int rowFontYOffset = -1;
    private int rowIconSide = -1;
    private int rowDirection = -1;
    private int rowMobYaw = -1;
    private int rowHeadYaw = -1;
    private int rowCameraPitch = -1;
    private int colAnchor;
    private int colX;
    private int colY;
    private int colScale;
    private int colFontScale = -1;
    private int colFontYOffset = -1;
    private int colDirection = -1;
    private int colMobYaw = -1;
    private int colHeadYaw = -1;
    private int colCameraPitch = -1;
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
        int nextRow = 0;

        if (element == Element.HEALTH_BAR || element == Element.HUNGER_BAR
                || element == Element.EMERALDS || element == Element.SOULS
                || element == Element.GLORY_KILLS) {
            rowEnabled = nextRow;
            boolean enabled = switch (element) {
                case HEALTH_BAR -> state.resolvedHealthBar().enabled();
                case HUNGER_BAR -> state.resolvedHungerBar().enabled();
                case EMERALDS -> state.resolvedEmeralds().enabled();
                case SOULS -> state.resolvedSouls().enabled();
                case GLORY_KILLS -> state.resolvedGloryKills().enabled();
                default -> true;
            };
            String label = switch (element) {
                case HEALTH_BAR -> "Enable CL Health Bar";
                case HUNGER_BAR -> "Enable CL Hunger Bar";
                case EMERALDS -> "Enable Emerald HUD";
                case SOULS -> "Enable Souls HUD";
                case GLORY_KILLS -> "Enable Glory Kills HUD";
                default -> "Enable";
            };
            enabledButton = CycleButton.onOffBuilder()
                    .withInitialValue(enabled)
                    .displayOnlyValue()
                    .create(0, 0, fieldWidth(), 20, Component.literal(label));
            placeField(enabledButton, 0, rowEnabled);
            nextRow++;
        }

        ClientConfigEditorState.HudElementValues values = switch (element) {
            case LP_BAR -> state.lpBar();
            case LEVEL_CIRCLE -> state.levelCircle();
            case HEALTH_BAR -> {
                ClientConfigEditorState.HealthBarValues healthBar = state.resolvedHealthBar();
                yield new ClientConfigEditorState.HudElementValues(
                        healthBar.anchor(), healthBar.x(), healthBar.y(), healthBar.scale(), healthBar.fontScale(), null);
            }
            case HUNGER_BAR -> {
                ClientConfigEditorState.HungerBarValues hungerBar = state.resolvedHungerBar();
                yield new ClientConfigEditorState.HudElementValues(
                        hungerBar.anchor(), hungerBar.x(), hungerBar.y(), hungerBar.scale(), hungerBar.fontScale(), null);
            }
            case EMERALDS -> {
                ClientConfigEditorState.EmeraldHudValues emeralds = state.resolvedEmeralds();
                yield new ClientConfigEditorState.HudElementValues(
                        emeralds.anchor(), emeralds.x(), emeralds.y(), emeralds.scale(), emeralds.fontScale(), null);
            }
            case SOULS -> {
                ClientConfigEditorState.SoulsHudValues souls = state.resolvedSouls();
                yield new ClientConfigEditorState.HudElementValues(
                        souls.anchor(), souls.x(), souls.y(), souls.scale(), souls.fontScale(), null);
            }
            case GLORY_KILLS -> {
                ClientConfigEditorState.GloryKillsHudValues gloryKills = state.resolvedGloryKills();
                yield new ClientConfigEditorState.HudElementValues(
                        gloryKills.anchor(), gloryKills.x(), gloryKills.y(),
                        gloryKills.scale(), gloryKills.fontScale(), null);
            }
            case MOB_FOCUS -> {
                ClientConfigEditorState.MobFocusValues mobFocus = state.resolvedMobFocus();
                yield new ClientConfigEditorState.HudElementValues(
                        mobFocus.anchor(), mobFocus.x(), mobFocus.y(), mobFocus.scale(), mobFocus.fontScale(), null);
            }
        };

        rowAnchor = assignRow(nextRow, 0);
        colAnchor = 0;

        String anchor = values.anchor() != null ? values.anchor() : ClientConfig.ANCHORS[0];
        anchorButton = CycleButton.<String>builder(Component::literal)
                .withValues(ClientConfig.ANCHORS)
                .withInitialValue(findAnchor(anchor))
                .displayOnlyValue()
                .create(0, 0, fieldWidth(), 20, Component.empty());
        placeField(anchorButton, colAnchor, rowAnchor);
        if (!twoColumnLayout()) {
            nextRow++;
        }

        rowX = assignRow(nextRow, 1);
        colX = 0;
        xField = newIntField(values.x());
        placeField(xField, colX, rowX);
        if (!twoColumnLayout()) {
            nextRow++;
        }

        rowY = assignRow(nextRow, 2);
        colY = 0;
        yField = newIntField(values.y());
        placeField(yField, colY, rowY);
        if (!twoColumnLayout()) {
            nextRow++;
        }

        rowScale = assignRow(nextRow, 3);
        colScale = 0;
        scaleField = newFloatField(values.scale());
        placeField(scaleField, colScale, rowScale);
        if (!twoColumnLayout()) {
            nextRow++;
        }

        if (element == Element.LEVEL_CIRCLE || element == Element.HEALTH_BAR
                || element == Element.LP_BAR || element == Element.HUNGER_BAR || element == Element.EMERALDS
                || element == Element.SOULS || element == Element.GLORY_KILLS || element == Element.MOB_FOCUS) {
            rowFontScale = assignRow(nextRow, 4);
            colFontScale = 0;
            fontScaleField = newFloatField(values.fontScale());
            placeField(fontScaleField, colFontScale, rowFontScale);
            if (!twoColumnLayout()) {
                nextRow++;
            }
        }

        if (element == Element.LEVEL_CIRCLE) {
            rowFontYOffset = assignRow(nextRow, 5);
            colFontYOffset = 0;
            fontYOffsetField = newFloatField(values.fontYOffset());
            placeField(fontYOffsetField, colFontYOffset, rowFontYOffset);
            if (!twoColumnLayout()) {
                nextRow++;
            }
        }

        if (element == Element.EMERALDS || element == Element.SOULS || element == Element.GLORY_KILLS) {
            rowIconSide = nextRow;
            String iconSide = switch (element) {
                case EMERALDS -> state.resolvedEmeralds().iconSide() != null
                        ? state.resolvedEmeralds().iconSide()
                        : ClientConfigEditorState.ICON_SIDES[1];
                case SOULS -> state.resolvedSouls().iconSide() != null
                        ? state.resolvedSouls().iconSide()
                        : ClientConfigEditorState.ICON_SIDES[1];
                default -> state.resolvedGloryKills().iconSide() != null
                        ? state.resolvedGloryKills().iconSide()
                        : ClientConfigEditorState.ICON_SIDES[1];
            };
            iconSideButton = CycleButton.<String>builder(Component::literal)
                    .withValues(ClientConfigEditorState.ICON_SIDES)
                    .withInitialValue(findIconSide(iconSide))
                    .displayOnlyValue()
                    .create(0, 0, fieldWidth(), 20, Component.empty());
            placeField(iconSideButton, 0, rowIconSide);
            nextRow++;
        }

        if (element == Element.MOB_FOCUS) {
            rowDirection = assignRow(nextRow, 0);
            colDirection = 1;
            String direction = state.resolvedMobFocus().direction() != null
                    ? state.resolvedMobFocus().direction()
                    : ClientConfigEditorState.MOB_FOCUS_DIRECTIONS[0];
            directionButton = CycleButton.<String>builder(Component::literal)
                    .withValues(ClientConfigEditorState.MOB_FOCUS_DIRECTIONS)
                    .withInitialValue(findDirection(direction))
                    .displayOnlyValue()
                    .create(0, 0, fieldWidth(), 20, Component.empty());
            placeField(directionButton, colDirection, rowDirection);

            ClientConfigEditorState.MobFocusValues mobFocus = state.resolvedMobFocus();
            rowMobYaw = assignRow(nextRow, 1);
            colMobYaw = 1;
            mobYawField = newFloatField(mobFocus.mobYaw());
            placeField(mobYawField, colMobYaw, rowMobYaw);

            rowHeadYaw = assignRow(nextRow, 2);
            colHeadYaw = 1;
            headYawField = newFloatField(mobFocus.headYaw());
            placeField(headYawField, colHeadYaw, rowHeadYaw);

            rowCameraPitch = assignRow(nextRow, 3);
            colCameraPitch = 1;
            cameraPitchField = newFloatField(mobFocus.cameraPitch());
            placeField(cameraPitchField, colCameraPitch, rowCameraPitch);
        }

        if (element == Element.HEALTH_BAR || element == Element.HUNGER_BAR) {
            rowMargin = nextRow;
            Integer margin = element == Element.HEALTH_BAR
                    ? state.resolvedHealthBar().armorMargin()
                    : state.resolvedHungerBar().breathMargin();
            marginField = newIntField(margin);
            placeField(marginField, 0, rowMargin);
            nextRow++;
        }

        if (element == Element.HUNGER_BAR) {
            rowMountHealthMargin = nextRow;
            Integer mountMargin = state.resolvedHungerBar().mountHealthMargin();
            if (mountMargin == null) {
                mountMargin = state.resolvedHungerBar().breathMargin();
            }
            mountHealthMarginField = newIntField(mountMargin);
            placeField(mountHealthMarginField, 0, rowMountHealthMargin);
        }

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private boolean twoColumnLayout() {
        return element == Element.MOB_FOCUS;
    }

    private int assignRow(int singleColumnRow, int twoColumnRow) {
        return twoColumnLayout() ? twoColumnRow : singleColumnRow;
    }

    private int fieldWidth() {
        return twoColumnLayout()
                ? ConfigScreenLayout.twoColumnFieldWidth(this)
                : ConfigScreenLayout.fieldWidth();
    }

    private void placeField(net.minecraft.client.gui.components.AbstractWidget widget, int column, int row) {
        if (twoColumnLayout()) {
            ConfigScreenLayout.positionFieldTwoColumn(this, column, row, widget);
        } else {
            ConfigScreenLayout.positionField(this, row, widget);
        }
        addRenderableWidget(widget);
    }

    private void drawFieldLabel(GuiGraphics graphics, int column, int row, String label) {
        if (twoColumnLayout()) {
            ConfigScreenLayout.renderLabelTwoColumn(this, graphics, column, row, label);
        } else {
            ConfigScreenLayout.renderLabel(this, graphics, row, label);
        }
    }

    private EditBox newIntField(Integer value) {
        EditBox box = new EditBox(font, 0, 0, fieldWidth(), 20, Component.empty());
        if (value != null) {
            box.setValue(String.valueOf(value));
        }
        box.setMaxLength(32);
        box.setFilter(text -> text.isEmpty() || text.matches("-?\\d*"));
        return box;
    }

    private EditBox newFloatField(Float value) {
        EditBox box = new EditBox(font, 0, 0, fieldWidth(), 20, Component.empty());
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

    private static String findIconSide(String iconSide) {
        for (String option : ClientConfigEditorState.ICON_SIDES) {
            if (option.equals(iconSide)) {
                return option;
            }
        }
        return ClientConfigEditorState.ICON_SIDES[1];
    }

    private static String findDirection(String direction) {
        for (String option : ClientConfigEditorState.MOB_FOCUS_DIRECTIONS) {
            if (option.equals(direction)) {
                return option;
            }
        }
        return ClientConfigEditorState.MOB_FOCUS_DIRECTIONS[0];
    }

    private void save() {
        ClientConfigEditorState.HudElementValues values = new ClientConfigEditorState.HudElementValues(
                anchorButton.getValue(),
                ConfigScalingForm.parseOptionalInt(xField),
                ConfigScalingForm.parseOptionalInt(yField),
                parseOptionalFloat(scaleField),
                fontScaleField != null ? parseOptionalFloat(fontScaleField) : null,
                fontYOffsetField != null ? parseOptionalFloat(fontYOffsetField) : null
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
        } else if (element == Element.HUNGER_BAR) {
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
        } else if (element == Element.MOB_FOCUS) {
            session.setMobFocus(new ClientConfigEditorState.MobFocusValues(
                    anchorButton.getValue(),
                    values.x(),
                    values.y(),
                    values.scale(),
                    values.fontScale(),
                    directionButton != null ? directionButton.getValue() : "data_first",
                    mobYawField != null ? parseOptionalFloat(mobYawField) : null,
                    headYawField != null ? parseOptionalFloat(headYawField) : null,
                    cameraPitchField != null ? parseOptionalFloat(cameraPitchField) : null
            ));
        } else if (element == Element.EMERALDS) {
            session.setEmeralds(new ClientConfigEditorState.EmeraldHudValues(
                    enabledButton.getValue(),
                    anchorButton.getValue(),
                    values.x(),
                    values.y(),
                    values.scale(),
                    values.fontScale(),
                    iconSideButton != null ? iconSideButton.getValue() : "right"
            ));
        } else if (element == Element.SOULS) {
            session.setSouls(new ClientConfigEditorState.SoulsHudValues(
                    enabledButton.getValue(),
                    anchorButton.getValue(),
                    values.x(),
                    values.y(),
                    values.scale(),
                    values.fontScale(),
                    iconSideButton != null ? iconSideButton.getValue() : "right"
            ));
        } else if (element == Element.GLORY_KILLS) {
            session.setGloryKills(new ClientConfigEditorState.GloryKillsHudValues(
                    enabledButton.getValue(),
                    anchorButton.getValue(),
                    values.x(),
                    values.y(),
                    values.scale(),
                    values.fontScale(),
                    iconSideButton != null ? iconSideButton.getValue() : "right"
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
            String enabledLabel = switch (element) {
                case HEALTH_BAR -> "Enable CL Health Bar";
                case HUNGER_BAR -> "Enable CL Hunger Bar";
                case EMERALDS -> "Enable Emerald HUD";
                case SOULS -> "Enable Souls HUD";
                case GLORY_KILLS -> "Enable Glory Kills HUD";
                default -> "Enable";
            };
            drawFieldLabel(graphics, 0, rowEnabled, enabledLabel);
        }
        drawFieldLabel(graphics, colAnchor, rowAnchor, "Anchor");
        drawFieldLabel(graphics, colX, rowX, "Pos X");
        drawFieldLabel(graphics, colY, rowY, "Pos Y");
        drawFieldLabel(graphics, colScale, rowScale, "Scale");
        if (rowFontScale >= 0) {
            drawFieldLabel(graphics, colFontScale, rowFontScale, "Font Scale");
        }
        if (rowFontYOffset >= 0) {
            drawFieldLabel(graphics, colFontYOffset, rowFontYOffset, "Text Y Offset");
        }
        if (rowIconSide >= 0) {
            drawFieldLabel(graphics, 0, rowIconSide, "Icon Side");
        }
        if (rowDirection >= 0) {
            drawFieldLabel(graphics, colDirection, rowDirection, "Direction");
        }
        if (rowMobYaw >= 0) {
            drawFieldLabel(graphics, colMobYaw, rowMobYaw, "Mob Yaw");
        }
        if (rowHeadYaw >= 0) {
            drawFieldLabel(graphics, colHeadYaw, rowHeadYaw, "Head Yaw");
        }
        if (rowCameraPitch >= 0) {
            drawFieldLabel(graphics, colCameraPitch, rowCameraPitch, "Camera Pitch");
        }
        if (rowMargin >= 0) {
            String marginLabel = element == Element.HEALTH_BAR ? "Armor Margin" : "Breath Margin";
            drawFieldLabel(graphics, 0, rowMargin, marginLabel);
        }
        if (rowMountHealthMargin >= 0) {
            drawFieldLabel(graphics, 0, rowMountHealthMargin, "Mount Health Margin");
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
