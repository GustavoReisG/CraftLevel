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

/** Lista as schematics do mod. O bioma e as hordas são por arena. */
@OnlyIn(Dist.CLIENT)
public class ArenaSchematicsScreen extends Screen {

    private final Screen parent;
    private SchematicList list;
    private int tableLeft;
    private int tableWidth;
    private int colNameW;
    private int colBiomeW;
    private int colHordesW;

    public ArenaSchematicsScreen(Screen parent) {
        super(Component.literal("CraftLevel - Schematics"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ArenaConfigPersistence.ensureSession(minecraft);
        layoutTable();

        int listTop = 48;
        int listBottom = ConfigScreenLayout.contentBottom(this);
        list = new SchematicList(this, tableWidth, listBottom - listTop, listTop, listBottom);
        for (ArenaSettings.Schematic schematic : ArenaConfigPersistence.session().schematics()) {
            list.addRow(schematic);
        }
        addRenderableWidget(list);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutTable() {
        tableWidth = Math.min(520, width - 40);
        tableLeft = (width - tableWidth) / 2;
        colHordesW = 72;
        int rest = tableWidth - colHordesW;
        colNameW = (int) (rest * 0.34f);
        colBiomeW = rest - colNameW;
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

    void openHordes(ArenaSettings.Schematic schematic) {
        flush();
        minecraft.setScreen(new ArenaSchematicHordesScreen(this, schematic));
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

    int colBiomeW() {
        return colBiomeW;
    }

    int colHordesW() {
        return colHordesW;
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
        graphics.drawCenteredString(font, "Schematic", tableLeft + colNameW / 2, headerY + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, "Biome", tableLeft + colNameW + colBiomeW / 2, headerY + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, "Hordes", tableLeft + colNameW + colBiomeW + colHordesW / 2, headerY + 6, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    static final class SchematicList extends ContainerObjectSelectionList<SchematicList.Row> {

        private final ArenaSchematicsScreen screen;

        SchematicList(ArenaSchematicsScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            setRenderBackground(true);
            setRenderTopAndBottom(false);
            setLeftPos(screen.tableLeft());
        }

        void addRow(ArenaSettings.Schematic schematic) {
            addEntry(new Row(this, schematic));
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

            private final SchematicList list;
            private final ArenaSettings.Schematic schematic;
            private final EditBox biomeField;
            private final Button hordesButton;

            Row(SchematicList list, ArenaSettings.Schematic schematic) {
                this.list = list;
                this.schematic = schematic;
                ArenaSchematicsScreen screen = list.screen;

                biomeField = new EditBox(screen.font, 0, 0, 80, 18, Component.empty());
                biomeField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                biomeField.setValue(schematic.biome == null ? "" : schematic.biome);
                biomeField.moveCursorToStart();

                hordesButton = Button.builder(Component.literal("Hordes"), btn -> {
                            flush();
                            screen.openHordes(schematic);
                        })
                        .bounds(0, 0, 64, 18)
                        .build();
            }

            void flush() {
                schematic.biome = biomeField.getValue().trim();
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
                ArenaSchematicsScreen screen = list.screen;
                int x = screen.tableLeft();
                String label = ArenaSettings.displayName(schematic.file);
                int nameCenter = x + screen.colNameW() / 2;
                graphics.drawString(
                        screen.font,
                        label,
                        nameCenter - screen.font.width(label) / 2,
                        top + 6,
                        0xFFFFFF
                );

                biomeField.setX(x + screen.colNameW() + 2);
                biomeField.setY(top + 1);
                biomeField.setWidth(screen.colBiomeW() - 4);
                biomeField.render(graphics, mouseX, mouseY, partialTick);

                hordesButton.setX(x + screen.colNameW() + screen.colBiomeW() + (screen.colHordesW() - 64) / 2);
                hordesButton.setY(top + 1);
                hordesButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(biomeField, hordesButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(biomeField, hordesButton);
            }
        }
    }
}
