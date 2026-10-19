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

/** Mobs de uma leva: id, quantidade total e equipamento. */
@OnlyIn(Dist.CLIENT)
public class ArenaWaveMobsScreen extends Screen {

    private final Screen parent;
    private final ArenaSettings.Wave wave;
    private MobList list;
    private Button addButton;
    private int tableLeft;
    private int tableWidth;
    private int colMobW;
    private int colCountW;
    private int colConfigW;
    private int colRemoveW;

    public ArenaWaveMobsScreen(Screen parent, ArenaSettings.Wave wave) {
        super(Component.literal("Horde Mobs"));
        this.parent = parent;
        this.wave = wave;
    }

    @Override
    protected void init() {
        ArenaConfigPersistence.ensureSession(minecraft);
        layoutTable();

        int listTop = 48;
        int listBottom = ConfigScreenLayout.contentBottom(this);
        list = new MobList(this, tableWidth, listBottom - listTop, listTop, listBottom);
        for (ArenaSettings.Mob mob : wave.mobs) {
            list.addRow(mob);
        }
        addRenderableWidget(list);

        addButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)), btn -> {
                    ArenaSettings.Mob mob = new ArenaSettings.Mob();
                    wave.mobs.add(mob);
                    list.addRow(mob);
                })
                .bounds(0, 0, 16, 16)
                .build();
        addRenderableWidget(addButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutTable() {
        tableWidth = Math.min(520, width - 40);
        tableLeft = (width - tableWidth) / 2;
        colCountW = 56;
        colConfigW = 36;
        colRemoveW = 36;
        colMobW = tableWidth - colCountW - colConfigW - colRemoveW;
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

    void openGear(ArenaSettings.Mob mob) {
        flush();
        minecraft.setScreen(new ArenaMobGearScreen(this, mob));
    }

    void remove(MobList.Row row) {
        wave.mobs.remove(row.mob);
        list.removeRow(row);
    }

    int tableLeft() {
        return tableLeft;
    }

    int tableWidth() {
        return tableWidth;
    }

    int colMobW() {
        return colMobW;
    }

    int colCountW() {
        return colCountW;
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
        graphics.drawCenteredString(font, "Mob", tableLeft + colMobW / 2, headerY + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, "Count", tableLeft + colMobW + colCountW / 2, headerY + 6, 0xFFFFFF);
        addButton.setX(tableLeft + colMobW + colCountW + (colConfigW - 16) / 2);
        addButton.setY(headerY + 2);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    static final class MobList extends ContainerObjectSelectionList<MobList.Row> {

        private final ArenaWaveMobsScreen screen;

        MobList(ArenaWaveMobsScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            setRenderBackground(true);
            setRenderTopAndBottom(false);
            setLeftPos(screen.tableLeft());
        }

        void addRow(ArenaSettings.Mob mob) {
            addEntry(new Row(this, mob));
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

            private final MobList list;
            private final ArenaSettings.Mob mob;
            private final EditBox mobField;
            private final EditBox countField;
            private final Button configButton;
            private final Button removeButton;

            Row(MobList list, ArenaSettings.Mob mob) {
                this.list = list;
                this.mob = mob;
                ArenaWaveMobsScreen screen = list.screen;

                mobField = new EditBox(screen.font, 0, 0, 80, 18, Component.empty());
                mobField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                mobField.setValue(mob.id == null ? "" : mob.id);
                mobField.moveCursorToStart();

                countField = new CenteredEditBox(screen.font, 0, 0, 40, 18, Component.empty());
                countField.setMaxLength(8);
                countField.setValue(mob.count == null ? "1" : mob.count);
                countField.moveCursorToStart();

                configButton = Button.builder(Component.literal("..."), btn -> {
                            flush();
                            screen.openGear(mob);
                        })
                        .bounds(0, 0, 24, 18)
                        .build();
                removeButton = Button.builder(Component.literal("-").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.remove(this))
                        .bounds(0, 0, 16, 16)
                        .build();
            }

            void flush() {
                mob.id = mobField.getValue().trim();
                mob.count = countField.getValue().trim();
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
                ArenaWaveMobsScreen screen = list.screen;
                int x = screen.tableLeft();
                mobField.setX(x + 2);
                mobField.setY(top + 1);
                mobField.setWidth(screen.colMobW() - 4);
                mobField.render(graphics, mouseX, mouseY, partialTick);

                countField.setX(x + screen.colMobW() + 2);
                countField.setY(top + 1);
                countField.setWidth(screen.colCountW() - 4);
                countField.render(graphics, mouseX, mouseY, partialTick);

                configButton.setX(x + screen.colMobW() + screen.colCountW() + (screen.colConfigW() - 24) / 2);
                configButton.setY(top + 1);
                configButton.render(graphics, mouseX, mouseY, partialTick);

                removeButton.setX(x + screen.colMobW() + screen.colCountW() + screen.colConfigW() + (screen.colRemoveW() - 16) / 2);
                removeButton.setY(top + 2);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(mobField, countField, configButton, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(mobField, countField, configButton, removeButton);
            }
        }
    }
}
