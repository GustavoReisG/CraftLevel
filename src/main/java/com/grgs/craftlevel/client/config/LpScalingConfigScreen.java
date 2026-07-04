package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Edição de lp_scaling em config.json. */
@OnlyIn(Dist.CLIENT)
public class LpScalingConfigScreen extends Screen {

    private static final int ROW_MOB_VARIATION = 0;
    private static final int ROW_LVL_MAX = 1;
    private static final int ROW_SCALING_START = 2;

    private final Screen parent;

    private EditBox mobLevelVariationField;
    private EditBox lvlMaxField;
    private ConfigScalingForm scalingForm;

    protected LpScalingConfigScreen(Screen parent) {
        super(net.minecraft.network.chat.Component.literal("CraftLevel - LP Scaling"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ConfigEditorState state = ConfigPersistence.session();

        mobLevelVariationField = ConfigScalingForm.newIntFieldFor(font, this, ROW_MOB_VARIATION, state.mobLevelVariation());
        addRenderableWidget(mobLevelVariationField);

        lvlMaxField = ConfigScalingForm.newIntFieldFor(font, this, ROW_LVL_MAX, state.lvlMax());
        addRenderableWidget(lvlMaxField);

        scalingForm = new ConfigScalingForm(font, state.lpBaseValue(), state.lpScaling(), state.lpScalingMode());
        scalingForm.positionRows(this, ROW_SCALING_START);
        addRenderableWidget(scalingForm.baseValueField());
        addRenderableWidget(scalingForm.scalingField());
        addRenderableWidget(scalingForm.scalingModeButton());

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void save() {
        ConfigEditorState state = ConfigPersistence.session();
        state.setMobLevelVariation(ConfigScalingForm.parseOptionalInt(mobLevelVariationField));
        state.setLvlMax(ConfigScalingForm.parseOptionalInt(lvlMaxField));
        scalingForm.applyToSession(state);
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
        ConfigScreenLayout.renderLabel(this, graphics, ROW_MOB_VARIATION, "Mob Level Variation");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_LVL_MAX, "Level Max");
        scalingForm.renderLabels(this, graphics, ROW_SCALING_START);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
