package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.arena.ArenaSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Levas sequenciais de uma horda nomeada. Remover uma renumera as de baixo. */
@OnlyIn(Dist.CLIENT)
public class ArenaWavesScreen extends Screen {

    private final Screen parent;
    private final ArenaSettings.HordeSet horde;
    private WaveList list;
    private Button addButton;
    private int tableLeft;
    private int tableWidth;
    private int colNameW;
    private int colConfigW;
    private int colRemoveW;

    public ArenaWavesScreen(Screen parent, ArenaSettings.HordeSet horde) {
        super(Component.literal(horde.name == null || horde.name.isBlank() ? "Hordes" : horde.name));
        this.parent = parent;
        this.horde = horde;
    }

    @Override
    protected void init() {
        ArenaConfigPersistence.ensureSession(minecraft);
        layoutTable();

        int listTop = 48;
        int listBottom = ConfigScreenLayout.contentBottom(this);
        list = new WaveList(this, tableWidth, listBottom - listTop, listTop, listBottom);
        for (ArenaSettings.Wave wave : horde.waves) {
            list.addRow(wave);
        }
        addRenderableWidget(list);

        addButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)), btn -> {
                    ArenaSettings.Wave wave = new ArenaSettings.Wave();
                    horde.waves.add(wave);
                    list.addRow(wave);
                })
                .bounds(0, 0, 16, 16)
                .build();
        addRenderableWidget(addButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutTable() {
        tableWidth = Math.min(420, width - 40);
        tableLeft = (width - tableWidth) / 2;
        colConfigW = 36;
        colRemoveW = 36;
        colNameW = tableWidth - colConfigW - colRemoveW;
    }

    private void save() {
        ArenaConfigPersistence.save(minecraft);
    }

    void openMobs(ArenaSettings.Wave wave) {
        minecraft.setScreen(new ArenaWaveMobsScreen(this, wave));
    }

    void remove(WaveList.Row row) {
        horde.waves.remove(row.wave);
        list.removeRow(row);
    }

    int tableLeft() {
        return tableLeft;
    }

    int tableWidth() {
        return tableWidth;
    }

    int colNameW() {
        return colNameW;
    }

    int colConfigW() {
        return colConfigW;
    }

    int colRemoveW() {
        return colRemoveW;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        int headerY = 28;
        graphics.fill(tableLeft, headerY, tableLeft + tableWidth, headerY + 20, 0x88000000);
        graphics.drawCenteredString(font, "Horde", tableLeft + colNameW / 2, headerY + 6, 0xFFFFFF);
        addButton.setX(tableLeft + colNameW + (colConfigW - 16) / 2);
        addButton.setY(headerY + 2);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    static final class WaveList extends ContainerObjectSelectionList<WaveList.Row> {

        private final ArenaWavesScreen screen;

        WaveList(ArenaWavesScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            setRenderBackground(true);
            setRenderTopAndBottom(false);
            setLeftPos(screen.tableLeft());
        }

        void addRow(ArenaSettings.Wave wave) {
            addEntry(new Row(this, wave));
        }

        void removeRow(Row row) {
            removeEntry(row);
        }

        @Override
        public int getRowWidth() {
            return screen.tableWidth();
        }

        @Override
        protected int getScrollbarPosition() {
            return screen.tableLeft() + screen.tableWidth();
        }

        @OnlyIn(Dist.CLIENT)
        static final class Row extends ContainerObjectSelectionList.Entry<Row> {

            private final WaveList list;
            private final ArenaSettings.Wave wave;
            private final Button configButton;
            private final Button removeButton;

            Row(WaveList list, ArenaSettings.Wave wave) {
                this.list = list;
                this.wave = wave;
                ArenaWavesScreen screen = list.screen;
                configButton = Button.builder(Component.literal("..."), btn -> screen.openMobs(wave))
                        .bounds(0, 0, 24, 18)
                        .build();
                removeButton = Button.builder(Component.literal("-").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.remove(this))
                        .bounds(0, 0, 16, 16)
                        .build();
            }

            @Override
            public void render(
                    GuiGraphics graphics,
                    int index,
                    int top,
                    int left,
                    int width,
                    int height,
                    int mouseX,
                    int mouseY,
                    boolean hovering,
                    float partialTick
            ) {
                ArenaWavesScreen screen = list.screen;
                int x = screen.tableLeft();
                graphics.drawString(screen.font, "Horde " + (index + 1), x + 6, top + 6, 0xFFFFFF);

                configButton.setX(x + screen.colNameW() + (screen.colConfigW() - 24) / 2);
                configButton.setY(top + 1);
                configButton.render(graphics, mouseX, mouseY, partialTick);

                removeButton.setX(x + screen.colNameW() + screen.colConfigW() + (screen.colRemoveW() - 16) / 2);
                removeButton.setY(top + 2);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(configButton, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(configButton, removeButton);
            }
        }
    }
}
