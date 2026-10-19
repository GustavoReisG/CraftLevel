package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.arena.ArenaSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Biblioteca de hordas. Cada uma tem nome e uma sequência de levas. */
@OnlyIn(Dist.CLIENT)
public class ArenaHordeSetsScreen extends Screen {

    private final Screen parent;
    private HordeList list;
    private Button addButton;
    private int tableLeft;
    private int tableWidth;
    private int colNameW;
    private int colConfigW;
    private int colRemoveW;

    public ArenaHordeSetsScreen(Screen parent) {
        super(Component.literal("CraftLevel - Hordes"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ArenaConfigPersistence.ensureSession(minecraft);
        layoutTable();

        int listTop = 48;
        int listBottom = ConfigScreenLayout.contentBottom(this);
        list = new HordeList(this, tableWidth, listBottom - listTop, listTop, listBottom);
        for (ArenaSettings.HordeSet horde : ArenaConfigPersistence.session().hordes()) {
            list.addRow(horde);
        }
        addRenderableWidget(list);

        addButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)), btn -> {
                    ArenaSettings.HordeSet horde = new ArenaSettings.HordeSet();
                    ArenaConfigPersistence.session().hordes().add(horde);
                    list.addRow(horde);
                })
                .bounds(0, 0, 16, 16)
                .build();
        addRenderableWidget(addButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutTable() {
        tableWidth = Math.min(460, width - 40);
        tableLeft = (width - tableWidth) / 2;
        colConfigW = 56;
        colRemoveW = 36;
        colNameW = tableWidth - colConfigW - colRemoveW;
    }

    private void save() {
        flush();
        ArenaConfigPersistence.save(minecraft);
    }

    private void flush() {
        if (list != null) {
            list.flush();
        }
    }

    void openWaves(ArenaSettings.HordeSet horde) {
        flush();
        minecraft.setScreen(new ArenaWavesScreen(this, horde));
    }

    void remove(HordeList.Row row) {
        ArenaConfigPersistence.session().hordes().remove(row.horde);
        for (ArenaSettings.Schematic schematic : ArenaConfigPersistence.session().schematics()) {
            schematic.hordeNames.removeIf(name -> name.equals(row.horde.name));
        }
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
        flush();
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        int headerY = 28;
        graphics.fill(tableLeft, headerY, tableLeft + tableWidth, headerY + 20, 0x88000000);
        graphics.drawCenteredString(font, "Name", tableLeft + colNameW / 2, headerY + 6, 0xFFFFFF);
        addButton.setX(tableLeft + colNameW + (colConfigW - 16) / 2);
        addButton.setY(headerY + 2);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    static final class HordeList extends ContainerObjectSelectionList<HordeList.Row> {

        private final ArenaHordeSetsScreen screen;

        HordeList(ArenaHordeSetsScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            setRenderBackground(true);
            setRenderTopAndBottom(false);
            setLeftPos(screen.tableLeft());
        }

        void addRow(ArenaSettings.HordeSet horde) {
            addEntry(new Row(this, horde));
        }

        void removeRow(Row row) {
            removeEntry(row);
        }

        void flush() {
            for (Row row : children()) {
                row.flush();
            }
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

            private final HordeList list;
            private final ArenaSettings.HordeSet horde;
            private final EditBox nameField;
            private final Button configButton;
            private final Button removeButton;

            Row(HordeList list, ArenaSettings.HordeSet horde) {
                this.list = list;
                this.horde = horde;
                ArenaHordeSetsScreen screen = list.screen;

                nameField = new EditBox(screen.font, 0, 0, 80, 18, Component.empty());
                nameField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                nameField.setValue(horde.name == null ? "" : horde.name);
                nameField.moveCursorToStart();

                configButton = Button.builder(Component.literal("..."), btn -> {
                            flush();
                            screen.openWaves(horde);
                        })
                        .bounds(0, 0, 40, 18)
                        .build();
                removeButton = Button.builder(Component.literal("-").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.remove(this))
                        .bounds(0, 0, 16, 16)
                        .build();
            }

            void flush() {
                String previous = horde.name == null ? "" : horde.name;
                String next = nameField.getValue().trim();
                if (!previous.equals(next)) {
                    for (ArenaSettings.Schematic schematic : ArenaConfigPersistence.session().schematics()) {
                        for (int i = 0; i < schematic.hordeNames.size(); i++) {
                            if (schematic.hordeNames.get(i).equals(previous)) {
                                schematic.hordeNames.set(i, next);
                            }
                        }
                    }
                }
                horde.name = next;
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
                ArenaHordeSetsScreen screen = list.screen;
                int x = screen.tableLeft();
                nameField.setX(x + 2);
                nameField.setY(top + 1);
                nameField.setWidth(screen.colNameW() - 4);
                nameField.render(graphics, mouseX, mouseY, partialTick);

                configButton.setX(x + screen.colNameW() + (screen.colConfigW() - 40) / 2);
                configButton.setY(top + 1);
                configButton.render(graphics, mouseX, mouseY, partialTick);

                removeButton.setX(x + screen.colNameW() + screen.colConfigW() + (screen.colRemoveW() - 16) / 2);
                removeButton.setY(top + 2);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(nameField, configButton, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(nameField, configButton, removeButton);
            }
        }
    }
}
