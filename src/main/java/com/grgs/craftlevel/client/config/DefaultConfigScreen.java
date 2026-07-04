package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Menu principal de config.json: Debug, LP Scaling e Scaling. */
@OnlyIn(Dist.CLIENT)
public class DefaultConfigScreen extends Screen {

    private static final int DEBUG_ROW = 0;
    private static final int LP_SCALING_ROW = 1;
    private static final int SCALING_ROW = 2;

    private final Screen parent;

    private CycleButton<Boolean> debugToggle;

    protected DefaultConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ConfigPersistence.beginEditing(minecraft);
        ConfigEditorState state = ConfigPersistence.session();

        debugToggle = CycleButton.<Boolean>builder(value -> Component.literal(value ? "True" : "False"))
                .withValues(true, false)
                .withInitialValue(state.debug() != null ? state.debug() : false)
                .displayOnlyValue()
                .create(0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        ConfigScreenLayout.positionField(this, DEBUG_ROW, debugToggle);
        addRenderableWidget(debugToggle);

        Button lpScalingButton = Button.builder(Component.literal("LP Scaling"), btn ->
                        minecraft.setScreen(new LpScalingConfigScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, LP_SCALING_ROW, lpScalingButton);
        addRenderableWidget(lpScalingButton);

        Button scalingButton = Button.builder(Component.literal("Scaling"), btn ->
                        minecraft.setScreen(new ScalingVariablesScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, SCALING_ROW, scalingButton);
        addRenderableWidget(scalingButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void save() {
        ConfigPersistence.session().setDebug(debugToggle.getValue());
        ConfigPersistence.save(minecraft);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        ConfigScreenLayout.renderLabel(this, graphics, DEBUG_ROW, "Debug");
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
