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

/** Escolhe quais hordas já criadas esta arena pode sortear. */
@OnlyIn(Dist.CLIENT)
public class ArenaSchematicHordesScreen extends Screen {

    private final Screen parent;
    private final ArenaSettings.Schematic schematic;
    private PickList list;
    private int tableLeft;
    private int tableWidth;
    private int colNameW;
    private int colPickW;

    public ArenaSchematicHordesScreen(Screen parent, ArenaSettings.Schematic schematic) {
        super(Component.literal("Hordes - " + ArenaSettings.displayName(schematic.file)));
        this.parent = parent;
        this.schematic = schematic;
    }

    @Override
    protected void init() {
        ArenaConfigPersistence.ensureSession(minecraft);
        layoutTable();

        int listTop = 48;
        int listBottom = ConfigScreenLayout.contentBottom(this);
        list = new PickList(this, tableWidth, listBottom - listTop, listTop, listBottom);
        for (ArenaSettings.HordeSet horde : ArenaConfigPersistence.session().hordes()) {
            list.addRow(horde);
        }
        addRenderableWidget(list);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, () -> ArenaConfigPersistence.save(minecraft)));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutTable() {
        tableWidth = Math.min(420, width - 40);
        tableLeft = (width - tableWidth) / 2;
        colPickW = 72;
        colNameW = tableWidth - colPickW;
    }

    void toggle(ArenaSettings.HordeSet horde) {
        String name = horde.name == null ? "" : horde.name;
        if (schematic.hordeNames.remove(name)) {
            return;
        }
        schematic.hordeNames.add(name);
    }

    boolean selected(ArenaSettings.HordeSet horde) {
        return schematic.hordeNames.contains(horde.name == null ? "" : horde.name);
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

    int colPickW() {
        return colPickW;
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

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    static final class PickList extends ContainerObjectSelectionList<PickList.Row> {

        private final ArenaSchematicHordesScreen screen;

        PickList(ArenaSchematicHordesScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            setRenderBackground(true);
            setRenderTopAndBottom(false);
            setLeftPos(screen.tableLeft());
        }

        void addRow(ArenaSettings.HordeSet horde) {
            addEntry(new Row(this, horde));
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

            private final PickList list;
            private final ArenaSettings.HordeSet horde;
            private final Button pickButton;

            Row(PickList list, ArenaSettings.HordeSet horde) {
                this.list = list;
                this.horde = horde;
                pickButton = Button.builder(Component.literal("Off"), btn -> {
                            list.screen.toggle(horde);
                            refreshLabel();
                        })
                        .bounds(0, 0, 56, 18)
                        .build();
                refreshLabel();
            }

            private void refreshLabel() {
                boolean on = list.screen.selected(horde);
                pickButton.setMessage(Component.literal(on ? "On" : "Off")
                        .withStyle(style -> style.withColor(on ? 0x55FF55 : 0xFF5555)));
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
                ArenaSchematicHordesScreen screen = list.screen;
                String name = horde.name == null || horde.name.isBlank() ? "(unnamed)" : horde.name;
                graphics.drawString(screen.font, name, screen.tableLeft() + 6, top + 6, 0xFFFFFF);
                pickButton.setX(screen.tableLeft() + screen.colNameW() + (screen.colPickW() - 56) / 2);
                pickButton.setY(top + 1);
                pickButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(pickButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(pickButton);
            }
        }
    }
}
