package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Configuração de glory_kills.json. */
@OnlyIn(Dist.CLIENT)
public class GloryKillsConfigScreen extends Screen {

    private static final int ROW_KILLS_PER_SHARD = 0;
    private static final int ROW_MIN_LEVEL_GAP = 1;

    private final Screen parent;

    private EditBox killsPerShardField;
    private EditBox minLevelGapField;

    public GloryKillsConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Glory Kills"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        GloryKillsPersistence.beginEditing(minecraft);
        GloryKillsEditorState state = GloryKillsPersistence.session();

        killsPerShardField = numericField(String.valueOf(state.killsPerShard()));
        ConfigScreenLayout.positionField(this, ROW_KILLS_PER_SHARD, killsPerShardField);
        addRenderableWidget(killsPerShardField);

        minLevelGapField = numericField(String.valueOf(state.minLevelGap()));
        ConfigScreenLayout.positionField(this, ROW_MIN_LEVEL_GAP, minLevelGapField);
        addRenderableWidget(minLevelGapField);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void save() {
        GloryKillsEditorState state = GloryKillsPersistence.session();
        state.setKillsPerShard(parseInt(killsPerShardField.getValue(), state.killsPerShard()));
        state.setMinLevelGap(parseInt(minLevelGapField.getValue(), state.minLevelGap()));
        GloryKillsPersistence.save(minecraft);
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
        ConfigScreenLayout.renderLabel(this, graphics, ROW_KILLS_PER_SHARD, "Kills Per Shard");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_MIN_LEVEL_GAP, "Min Level Gap");
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
