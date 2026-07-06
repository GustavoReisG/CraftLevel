package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.scaling.ApplyMode;
import com.grgs.craftlevel.scaling.ScalingDefinition;
import com.grgs.craftlevel.scaling.ScalingMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Campos reutilizáveis de definição de scaling (Base Value, Scaling, modes). */
final class ConfigScalingForm {

    private static final String LABEL_BASE_VALUE = "Base Value";
    private static final String LABEL_SCALING = "Scaling";
    private static final String LABEL_SCALING_MODE = "Scaling Mode";
    private static final String LABEL_APPLY_MODE = "Apply Mode";
    private static final String LABEL_INCLUDE_MODIFIERS = "Include Modifiers";

    private final EditBox baseValueField;
    private final EditBox scalingField;
    private final CycleButton<ScalingMode> scalingModeButton;
    private final CycleButton<ApplyMode> applyModeButton;
    private final CycleButton<Boolean> includeModifiersButton;

    /** LP scaling: apply mode is always overwrite, so it is not editable. */
    ConfigScalingForm(Font font, Double baseValue, Double scaling, ScalingMode scalingMode) {
        baseValueField = newNumericField(font, baseValue);
        scalingField = newNumericField(font, scaling);
        scalingModeButton = enumButton(ScalingMode.values(),
                scalingMode != null ? scalingMode : ScalingMode.POWER);
        applyModeButton = null;
        includeModifiersButton = null;
    }

    ConfigScalingForm(
            Font font,
            Double baseValue,
            Double scaling,
            ScalingMode scalingMode,
            ApplyMode applyMode,
            Boolean includeModifiers
    ) {
        baseValueField = newNumericField(font, baseValue);
        scalingField = newNumericField(font, scaling);
        scalingModeButton = enumButton(ScalingMode.values(),
                scalingMode != null ? scalingMode : ScalingMode.POWER);
        applyModeButton = enumButton(ApplyMode.values(),
                applyMode != null ? applyMode : ApplyMode.MULTIPLY);
        includeModifiersButton = booleanButton(includeModifiers != null ? includeModifiers : false);
    }

    ConfigScalingForm(
            Font font,
            Double baseValue,
            Double scaling,
            ScalingMode scalingMode,
            ApplyMode applyMode
    ) {
        this(font, baseValue, scaling, scalingMode, applyMode, false);
    }

    ConfigScalingForm(Font font, ScalingDefinition definition) {
        this(font, definition.baseValue(), definition.scaling(), definition.scalingMode(),
                definition.applyMode(), definition.includeModifiers());
    }

    ConfigScalingForm(Font font, ConfigEditorState.ScalerEntry entry) {
        this(font, entry.baseValue(), entry.scaling(), entry.scalingMode(), entry.applyMode(),
                entry.includeModifiers());
    }

    void positionRows(Screen screen, int startRow) {
        ConfigScreenLayout.positionField(screen, startRow, baseValueField);
        ConfigScreenLayout.positionField(screen, startRow + 1, scalingField);
        ConfigScreenLayout.positionField(screen, startRow + 2, scalingModeButton);
        if (applyModeButton != null) {
            ConfigScreenLayout.positionField(screen, startRow + 3, applyModeButton);
        }
        if (includeModifiersButton != null) {
            ConfigScreenLayout.positionField(screen, startRow + 4, includeModifiersButton);
        }
    }

    void renderLabels(Screen screen, GuiGraphics graphics, int startRow) {
        ConfigScreenLayout.renderLabel(screen, graphics, startRow, LABEL_BASE_VALUE);
        ConfigScreenLayout.renderLabel(screen, graphics, startRow + 1, LABEL_SCALING);
        ConfigScreenLayout.renderLabel(screen, graphics, startRow + 2, LABEL_SCALING_MODE);
        if (applyModeButton != null) {
            ConfigScreenLayout.renderLabel(screen, graphics, startRow + 3, LABEL_APPLY_MODE);
        }
        if (includeModifiersButton != null) {
            ConfigScreenLayout.renderLabel(screen, graphics, startRow + 4, LABEL_INCLUDE_MODIFIERS);
        }
    }

    EditBox baseValueField() {
        return baseValueField;
    }

    EditBox scalingField() {
        return scalingField;
    }

    CycleButton<ScalingMode> scalingModeButton() {
        return scalingModeButton;
    }

    CycleButton<ApplyMode> applyModeButton() {
        return applyModeButton;
    }

    CycleButton<Boolean> includeModifiersButton() {
        return includeModifiersButton;
    }

    ConfigEditorState.ScalerEntry readScalerEntry() {
        return new ConfigEditorState.ScalerEntry(
                parseOptionalDouble(baseValueField),
                parseOptionalDouble(scalingField),
                scalingModeButton.getValue(),
                applyModeButton != null ? applyModeButton.getValue() : null,
                includeModifiersButton != null && includeModifiersButton.getValue()
        );
    }

    void applyToSession(ConfigEditorState session) {
        session.setLpScaling(
                parseOptionalDouble(baseValueField),
                parseOptionalDouble(scalingField),
                scalingModeButton.getValue()
        );
    }

    static EditBox newIntField(Font font, Integer value) {
        EditBox box = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        if (value != null) {
            box.setValue(String.valueOf(value));
        }
        box.setMaxLength(16);
        box.setFilter(text -> text.isEmpty() || text.matches("-?\\d*"));
        return box;
    }

    static EditBox newIntFieldFor(Font font, Screen screen, int row, Integer value) {
        EditBox field = newIntField(font, value);
        ConfigScreenLayout.positionField(screen, row, field);
        return field;
    }

    private static EditBox newNumericField(Font font, Double value) {
        EditBox box = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        if (value != null) {
            box.setValue(String.valueOf(value));
        }
        box.setMaxLength(32);
        box.setFilter(text -> text.isEmpty() || text.matches("-?\\d*\\.?\\d*"));
        return box;
    }

    private static <T extends Enum<T>> CycleButton<T> enumButton(T[] values, T initial) {
        return CycleButton.<T>builder(ConfigScalingForm::formatEnum)
                .withValues(values)
                .withInitialValue(initial)
                .displayOnlyValue()
                .create(0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
    }

    private static CycleButton<Boolean> booleanButton(boolean initial) {
        return CycleButton.<Boolean>builder(value -> Component.literal(value ? "True" : "False"))
                .withValues(false, true)
                .withInitialValue(initial)
                .displayOnlyValue()
                .create(0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
    }

    private static Component formatEnum(Enum<?> value) {
        String name = value.name().toLowerCase();
        return Component.literal(Character.toUpperCase(name.charAt(0)) + name.substring(1));
    }

    static Integer parseOptionalInt(EditBox field) {
        String text = field.getValue().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseOptionalDouble(EditBox field) {
        String text = field.getValue().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
