package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.config.ConfigManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Tela inicial aberta pelo botão Config na lista de mods. */
public class CraftLevelConfigScreen extends Screen {

    private final Screen parent;

    public CraftLevelConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int row = 0;
        for (String fileName : ConfigManager.CONFIG_FILES) {
            Button button = createFileButton(fileName);
            ConfigScreenLayout.positionRow(this, row++, button);
            addRenderableWidget(button);
        }

        Button economy = Button.builder(Component.literal("Economy"), btn ->
                        minecraft.setScreen(new EconomyConfigMenuScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, row++, economy);
        addRenderableWidget(economy);

        Button souls = Button.builder(Component.literal("Souls"), btn ->
                        minecraft.setScreen(new SoulsConfigScreen(this)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, row, souls);
        addRenderableWidget(souls);

        addRenderableWidget(ConfigScreenLayout.bottomDone(this, this::onClose));
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
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
