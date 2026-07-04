package com.grgs.craftlevel.client.config;

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

import java.util.LinkedHashMap;
import java.util.List;

/** Editor scrollável de mob_levels.json. */
@OnlyIn(Dist.CLIENT)
public class MobLevelsConfigScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;

    private final Screen parent;

    private MobLevelsList mobList;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colMobW;
    private int colMinW;
    private int colMaxW;
    private int colRemoveW;

    protected MobLevelsConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Mob Levels"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MobLevelsPersistence.beginEditing(minecraft);
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        mobList = new MobLevelsList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        populateList();
        addRenderableWidget(mobList);

        addRowButton = createPlusButton(this::addNewRow);
        addRenderableWidget(addRowButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void populateList() {
        for (var entry : MobLevelsPersistence.session().levels().entrySet()) {
            mobList.addRow(new MobLevelRowData(
                    entry.getKey(),
                    entry.getValue().lvlMin(),
                    entry.getValue().lvlMax()
            ));
        }
    }

    void addNewRow() {
        mobList.addRow(new MobLevelRowData("", null, null));
    }

    private void layoutColumns() {
        tableWidth = Math.min(420, width - 40);
        tableLeft = (width - tableWidth) / 2;
        colMobW = (int) (tableWidth * 0.46f);
        colMinW = (int) (tableWidth * 0.22f);
        colMaxW = (int) (tableWidth * 0.22f);
        colRemoveW = tableWidth - colMobW - colMinW - colMaxW;
    }

    void removeRow(MobLevelsList.MobRowEntry entry) {
        mobList.removeRow(entry);
    }

    private void save() {
        MobLevelsEditorState state = MobLevelsPersistence.session();
        state.clear();
        state.levels().putAll(mobList.buildLevels());
        MobLevelsPersistence.save(minecraft);
    }

    private static Button createPlusButton(Runnable action) {
        return Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)), btn -> action.run())
                .bounds(0, 0, 16, 16)
                .build();
    }

    int getTableLeft() {
        return tableLeft;
    }

    int getTableWidth() {
        return tableWidth;
    }

    int getColMobW() {
        return colMobW;
    }

    int getColMinW() {
        return colMinW;
    }

    int getColMaxW() {
        return colMaxW;
    }

    int getColRemoveW() {
        return colRemoveW;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);

        graphics.fill(tableLeft, HEADER_Y, tableLeft + tableWidth, HEADER_Y + HEADER_HEIGHT, 0x88000000);
        drawCenteredHeaderCell(graphics, "Mob", tableLeft, colMobW);
        drawCenteredHeaderCell(graphics, "Level Min", tableLeft + colMobW, colMinW);
        drawCenteredHeaderCell(graphics, "Level Max", tableLeft + colMobW + colMinW, colMaxW);

        int removeColX = tableLeft + colMobW + colMinW + colMaxW;
        addRowButton.setX(removeColX + (colRemoveW - 16) / 2);
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredHeaderCell(GuiGraphics graphics, String label, int x, int colWidth) {
        graphics.drawCenteredString(font, label, x + colWidth / 2, HEADER_Y + 6, 0xFFFFFF);
    }

    @OnlyIn(Dist.CLIENT)
    static class MobLevelsList extends ContainerObjectSelectionList<MobLevelsList.MobRowEntry> {

        private final MobLevelsConfigScreen screen;

        MobLevelsList(MobLevelsConfigScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            this.setRenderBackground(true);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(screen.getTableLeft());
        }

        void addRow(MobLevelRowData data) {
            addEntry(new MobRowEntry(this, data));
        }

        void removeRow(MobRowEntry entry) {
            removeEntry(entry);
        }

        LinkedHashMap<String, MobLevelsEditorState.MobLevelEntry> buildLevels() {
            LinkedHashMap<String, MobLevelsEditorState.MobLevelEntry> levels = new LinkedHashMap<>();
            for (MobRowEntry entry : children()) {
                String mobId = entry.getMobId().trim();
                if (mobId.isEmpty()) {
                    continue;
                }
                levels.put(mobId, new MobLevelsEditorState.MobLevelEntry(
                        entry.getLvlMin(),
                        entry.getLvlMax()
                ));
            }
            return levels;
        }

        @Override
        protected int getScrollbarPosition() {
            return screen.getTableLeft() + screen.getTableWidth() - 6;
        }

        @Override
        public int getRowWidth() {
            return screen.getTableWidth();
        }

        @OnlyIn(Dist.CLIENT)
        static class MobRowEntry extends Entry<MobRowEntry> {

            private final MobLevelsList list;
            private final EditBox mobField;
            private final EditBox minField;
            private final EditBox maxField;
            private final Button removeButton;

            MobRowEntry(MobLevelsList list, MobLevelRowData data) {
                this.list = list;
                MobLevelsConfigScreen screen = list.screen;

                mobField = new EditBox(
                        screen.font,
                        0,
                        0,
                        screen.getColMobW() - 6,
                        18,
                        Component.literal("Mob")
                );
                mobField.setValue(data.mobId());
                mobField.setMaxLength(256);

                minField = new EditBox(
                        screen.font,
                        0,
                        0,
                        screen.getColMinW() - 6,
                        18,
                        Component.literal("Min")
                );
                minField.setValue(data.lvlMin() != null ? String.valueOf(data.lvlMin()) : "");
                minField.setFilter(text -> text.isEmpty() || text.matches("-?\\d*"));

                maxField = new EditBox(
                        screen.font,
                        0,
                        0,
                        screen.getColMaxW() - 6,
                        18,
                        Component.literal("Max")
                );
                maxField.setValue(data.lvlMax() != null ? String.valueOf(data.lvlMax()) : "");
                maxField.setFilter(text -> text.isEmpty() || text.matches("-?\\d*"));

                removeButton = Button.builder(
                                Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.removeRow(this))
                        .bounds(0, 0, 18, 18)
                        .build();
            }

            String getMobId() {
                return mobField.getValue();
            }

            Integer getLvlMin() {
                return ConfigScalingForm.parseOptionalInt(minField);
            }

            Integer getLvlMax() {
                return ConfigScalingForm.parseOptionalInt(maxField);
            }

            @Override
            public void render(
                    GuiGraphics graphics,
                    int index,
                    int top,
                    int left,
                    int rowWidth,
                    int rowHeight,
                    int mouseX,
                    int mouseY,
                    boolean hovered,
                    float partialTick
            ) {
                MobLevelsConfigScreen screen = list.screen;
                int tableLeft = screen.getTableLeft();

                mobField.setX(tableLeft + 2);
                mobField.setY(top + 2);
                minField.setX(tableLeft + screen.getColMobW() + 2);
                minField.setY(top + 2);
                maxField.setX(tableLeft + screen.getColMobW() + screen.getColMinW() + 2);
                maxField.setY(top + 2);
                removeButton.setX(tableLeft + screen.getColMobW() + screen.getColMinW() + screen.getColMaxW()
                        + (screen.getColRemoveW() - 18) / 2);
                removeButton.setY(top + 2);

                mobField.render(graphics, mouseX, mouseY, partialTick);
                minField.render(graphics, mouseX, mouseY, partialTick);
                maxField.render(graphics, mouseX, mouseY, partialTick);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(mobField, minField, maxField, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(mobField, minField, maxField, removeButton);
            }
        }
    }
}
