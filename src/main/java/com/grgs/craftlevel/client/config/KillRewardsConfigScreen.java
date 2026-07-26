package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Editor de kill_rewards em economy.json. */
@OnlyIn(Dist.CLIENT)
public class KillRewardsConfigScreen extends Screen {

    private static final int ROW_NORMAL = 0;
    private static final int ROW_LOW = 1;
    private static final int ROW_HIGH = 2;
    private static final int ROW_GAP = 3;
    private static final int ROW_BOSS_BONUS = 4;
    private static final int ROW_BOSS_LIST = 5;
    private static final int ROW_BLACKLIST = 6;

    private static final int DOTS_WIDTH = 28;

    private final Screen parent;

    private EditBox normalRewardField;
    private EditBox lowLevelField;
    private EditBox highBonusField;
    private EditBox levelGapField;
    private EditBox bossBonusField;

    public KillRewardsConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Kill Rewards"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        KillRewardsPersistence.ensureSession(minecraft);
        KillRewardsEditorState state = KillRewardsPersistence.session();

        normalRewardField = numericField(String.valueOf(state.sameLevelEmeralds()));
        ConfigScreenLayout.positionField(this, ROW_NORMAL, normalRewardField);
        addRenderableWidget(normalRewardField);

        lowLevelField = numericField(String.valueOf(state.lowLevelEmeralds()));
        ConfigScreenLayout.positionField(this, ROW_LOW, lowLevelField);
        addRenderableWidget(lowLevelField);

        highBonusField = numericField(String.valueOf(state.highLevelBonus()));
        ConfigScreenLayout.positionField(this, ROW_HIGH, highBonusField);
        addRenderableWidget(highBonusField);

        levelGapField = numericField(String.valueOf(state.levelGap()));
        ConfigScreenLayout.positionField(this, ROW_GAP, levelGapField);
        addRenderableWidget(levelGapField);

        bossBonusField = numericField(String.valueOf(state.bossBonus()));
        ConfigScreenLayout.positionField(this, ROW_BOSS_BONUS, bossBonusField);
        addRenderableWidget(bossBonusField);

        addRenderableWidget(dotsButton(ROW_BOSS_LIST, () -> openList(KillRewardMobListScreen.Kind.BOSSES)));
        addRenderableWidget(dotsButton(ROW_BLACKLIST, () -> openList(KillRewardMobListScreen.Kind.BLACKLIST)));

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private Button dotsButton(int row, Runnable action) {
        Button button = Button.builder(Component.literal("..."), btn -> action.run())
                .bounds(0, 0, DOTS_WIDTH, 20)
                .build();
        ConfigScreenLayout.positionField(this, row, button);
        button.setWidth(DOTS_WIDTH);
        return button;
    }

    private void openList(KillRewardMobListScreen.Kind kind) {
        flushFieldsToState();
        minecraft.setScreen(new KillRewardMobListScreen(this, kind));
    }

    private void flushFieldsToState() {
        KillRewardsEditorState state = KillRewardsPersistence.session();
        state.setSameLevelEmeralds(parseInt(normalRewardField.getValue(), state.sameLevelEmeralds()));
        state.setLowLevelEmeralds(parseInt(lowLevelField.getValue(), state.lowLevelEmeralds()));
        state.setHighLevelBonus(parseInt(highBonusField.getValue(), state.highLevelBonus()));
        state.setLevelGap(parseInt(levelGapField.getValue(), state.levelGap()));
        state.setBossBonus(parseInt(bossBonusField.getValue(), state.bossBonus()));
    }

    private void save() {
        flushFieldsToState();
        KillRewardsPersistence.save(minecraft);
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
        ConfigScreenLayout.renderLabel(this, graphics, ROW_NORMAL, "Normal Reward");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_LOW, "Low Level Reward");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_HIGH, "High Level Bonus");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_GAP, "Level Gap");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_BOSS_BONUS, "Boss Bonus");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_BOSS_LIST, "Boss List");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_BLACKLIST, "Blacklist");
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
