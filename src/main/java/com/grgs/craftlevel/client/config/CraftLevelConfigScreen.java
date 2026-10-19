package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Tela inicial aberta pelo botão Config na lista de mods. */
public class CraftLevelConfigScreen extends Screen {

    private final Screen parent;
    private final List<Button> categoryButtons = new ArrayList<>();
    private int scrollY;
    private int maxScroll;

    public CraftLevelConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        categoryButtons.clear();
        int row = 0;
        for (String fileName : ConfigManager.CONFIG_FILES) {
            Button button = createFileButton(fileName);
            ConfigScreenLayout.positionRow(this, row++, button);
            addRenderableWidget(button);
            categoryButtons.add(button);
        }

        Button economy = Button.builder(Component.literal("Economy"), btn ->
                        minecraft.setScreen(new EconomyConfigMenuScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, row++, economy);
        addRenderableWidget(economy);
        categoryButtons.add(economy);

        Button souls = Button.builder(Component.literal("Souls"), btn ->
                        minecraft.setScreen(new SoulsConfigScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, row++, souls);
        addRenderableWidget(souls);
        categoryButtons.add(souls);

        Button gloryKills = Button.builder(Component.literal("Glory Kills"), btn ->
                        minecraft.setScreen(new GloryKillsConfigScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, row++, gloryKills);
        addRenderableWidget(gloryKills);
        categoryButtons.add(gloryKills);

        Button altar = Button.builder(Component.literal("Altar Of Death"), btn -> {
                        AltarOfDeathPersistence.beginEditing(minecraft);
                        minecraft.setScreen(new AltarOfDeathConfigMenuScreen(this));
                    })
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, row++, altar);
        addRenderableWidget(altar);
        categoryButtons.add(altar);

        Button arena = Button.builder(Component.literal("Arena"), btn ->
                        minecraft.setScreen(new ArenaConfigMenuScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, row++, arena);
        addRenderableWidget(arena);
        categoryButtons.add(arena);

        Button skills = Button.builder(Component.literal("Skills"), btn ->
                        minecraft.setScreen(new SkillsConfigListScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, row, skills);
        addRenderableWidget(skills);
        categoryButtons.add(skills);

        int contentHeight = categoryButtons.size() * ConfigScreenLayout.ROW_HEIGHT;
        int viewHeight = Math.max(1, ConfigScreenLayout.contentBottom(this) - ConfigScreenLayout.START_Y);
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollY = Mth.clamp(scrollY, 0, maxScroll);
        applyScroll();

        addRenderableWidget(ConfigScreenLayout.bottomDone(this, this::onClose));
    }

    private void applyScroll() {
        int viewTop = ConfigScreenLayout.START_Y;
        int viewBottom = ConfigScreenLayout.contentBottom(this);
        for (int i = 0; i < categoryButtons.size(); i++) {
            Button button = categoryButtons.get(i);
            int y = ConfigScreenLayout.START_Y + i * ConfigScreenLayout.ROW_HEIGHT - scrollY;
            button.setY(y);
            button.visible = y + 20 > viewTop && y < viewBottom;
            button.active = button.visible;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            scrollY = Mth.clamp(scrollY - (int) Math.round(delta * 12), 0, maxScroll);
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private Button createFileButton(String fileName) {
        return Button.builder(Component.literal(formatLabel(fileName)), btn -> openConfig(fileName))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
    }

    private static String formatLabel(String fileName) {
        return ConfigScreenLabels.fileButtonLabel(fileName);
    }

    private void openConfig(String fileName) {
        if ("client".equals(fileName)) {
            minecraft.setScreen(new ClientConfigMenuScreen(this));
        } else if ("config".equals(fileName)) {
            minecraft.setScreen(new DefaultConfigScreen(this));
        } else if ("mob_levels".equals(fileName)) {
            minecraft.setScreen(new MobLevelsConfigScreen(this));
        } else if ("player_stats".equals(fileName) || "mob_stats".equals(fileName)) {
            minecraft.setScreen(StatWiringMenuScreen.forFile(this, fileName, minecraft));
        } else if ("mob_overrides".equals(fileName)) {
            minecraft.setScreen(new MobOverridesListScreen(this));
        } else {
            minecraft.setScreen(new ServerConfigInfoScreen(this, fileName));
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
        if (maxScroll > 0) {
            int trackX = ConfigScreenLayout.centerX(this) + ConfigScreenLayout.BUTTON_WIDTH + 6;
            int trackTop = ConfigScreenLayout.START_Y;
            int trackBottom = ConfigScreenLayout.contentBottom(this);
            int trackHeight = Math.max(1, trackBottom - trackTop);
            graphics.fill(trackX, trackTop, trackX + 3, trackBottom, 0x66000000);
            float ratio = (float) scrollY / (float) maxScroll;
            int thumbHeight = Math.max(12, (int) (trackHeight * (trackHeight / (float) (trackHeight + maxScroll))));
            int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * ratio);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xAAFFFFFF);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
