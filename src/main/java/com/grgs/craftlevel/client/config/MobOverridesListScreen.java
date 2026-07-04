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
import java.util.Map;

/** Lista de mobs em mob_overrides.json. */
@OnlyIn(Dist.CLIENT)
public class MobOverridesListScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;
    private static final int OPEN_BTN_WIDTH = 20;
    private static final int REMOVE_BTN_WIDTH = 18;

    private final Screen parent;

    private MobOverridesList mobList;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colNameW;
    private int colOpenW;
    private int colRemoveW;

    protected MobOverridesListScreen(Screen parent) {
        super(Component.literal("CraftLevel - Mob Overrides"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MobOverridesPersistence.beginEditing(minecraft);
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        mobList = new MobOverridesList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        populateList();
        addRenderableWidget(mobList);

        addRowButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)), btn -> addMob())
                .bounds(0, 0, 16, 16)
                .build();
        addRenderableWidget(addRowButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutColumns() {
        tableWidth = Math.min(420, width - 40);
        tableLeft = (width - tableWidth) / 2;
        ConfigTableColumns.ThreeColumns cols = ConfigTableColumns.layout(tableWidth);
        colNameW = cols.primary();
        colOpenW = cols.secondary();
        colRemoveW = cols.remove();
    }

    private void populateList() {
        for (String mobId : MobOverridesPersistence.session().mobs().keySet()) {
            mobList.addRow(mobId);
        }
    }

    void openMob(MobOverridesList.MobOverrideEntry entry) {
        syncEntryToSession(entry);
        String mobId = entry.getName().trim();
        if (mobId.isEmpty()) {
            return;
        }
        minecraft.setScreen(StatWiringMenuScreen.forMobOverride(this, mobId, minecraft));
    }

    void addMob() {
        String mobId = uniqueMobId("minecraft:");
        MobOverridesPersistence.session().mob(mobId);
        mobList.addRow(mobId);
    }

    void removeMob(MobOverridesList.MobOverrideEntry entry) {
        MobOverridesPersistence.session().removeMob(entry.getOriginalName());
        mobList.removeRow(entry);
    }

    void syncEntryToSession(MobOverridesList.MobOverrideEntry entry) {
        String newName = entry.getName().trim();
        if (newName.isEmpty()) {
            return;
        }
        String oldName = entry.getOriginalName();
        if (newName.equals(oldName)) {
            return;
        }
        MobOverridesPersistence.session().renameMob(oldName, newName);
        entry.setOriginalName(newName);
    }

    private void save() {
        mobList.rebuildSession();
        MobOverridesPersistence.save(minecraft);
    }

    private static String uniqueMobId(String base) {
        Map<String, StatWiringEditorState> mobs = MobOverridesPersistence.session().mobs();
        if (!mobs.containsKey(base)) {
            return base;
        }
        int index = 2;
        while (mobs.containsKey(base + index)) {
            index++;
        }
        return base + index;
    }

    int getTableLeft() {
        return tableLeft;
    }

    int getTableWidth() {
        return tableWidth;
    }

    int getColNameW() {
        return colNameW;
    }

    int getColOpenW() {
        return colOpenW;
    }

    int getColRemoveW() {
        return colRemoveW;
    }

    Screen getMenuParent() {
        return parent;
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
        drawCenteredHeaderCell(graphics, "Mob", tableLeft, colNameW);

        int removeColX = tableLeft + colNameW + colOpenW;
        addRowButton.setX(ConfigTableColumns.centeredX(removeColX, colRemoveW, 16));
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredHeaderCell(GuiGraphics graphics, String label, int x, int colWidth) {
        graphics.drawCenteredString(font, label, x + colWidth / 2, HEADER_Y + 6, 0xFFFFFF);
    }

    @OnlyIn(Dist.CLIENT)
    static class MobOverridesList extends ContainerObjectSelectionList<MobOverridesList.MobOverrideEntry> {

        private final MobOverridesListScreen screen;

        MobOverridesList(MobOverridesListScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 24);
            this.screen = screen;
            this.setRenderBackground(true);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(screen.getTableLeft());
        }

        void addRow(String mobId) {
            addEntry(new MobOverrideEntry(this, mobId));
        }

        void removeRow(MobOverrideEntry entry) {
            removeEntry(entry);
        }

        void rebuildSession() {
            LinkedHashMap<String, StatWiringEditorState> map = new LinkedHashMap<>();
            MobOverridesEditorState state = MobOverridesPersistence.session();
            for (MobOverrideEntry entry : children()) {
                screen.syncEntryToSession(entry);
                String name = entry.getName().trim();
                if (name.isEmpty()) {
                    continue;
                }
                StatWiringEditorState wiring = state.mobs().get(entry.getOriginalName());
                if (wiring == null) {
                    wiring = StatWiringEditorState.createEmpty();
                }
                map.put(name, wiring);
            }
            state.mobs().clear();
            state.mobs().putAll(map);
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
        static class MobOverrideEntry extends Entry<MobOverrideEntry> {

            private final MobOverridesList list;
            private String originalName;
            private final EditBox nameField;
            private final Button openButton;
            private final Button removeButton;

            MobOverrideEntry(MobOverridesList list, String mobId) {
                this.list = list;
                this.originalName = mobId;
                MobOverridesListScreen screen = list.screen;

                nameField = new EditBox(
                        screen.minecraft.font,
                        0,
                        0,
                        screen.getColNameW() - 6,
                        18,
                        Component.empty()
                );
                nameField.setValue(mobId);
                nameField.setMaxLength(256);

                openButton = Button.builder(Component.literal("..."), btn -> screen.openMob(this))
                        .bounds(0, 0, OPEN_BTN_WIDTH, 18)
                        .build();

                removeButton = Button.builder(
                                Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.removeMob(this))
                        .bounds(0, 0, REMOVE_BTN_WIDTH, 18)
                        .build();
            }

            String getName() {
                return nameField.getValue();
            }

            String getOriginalName() {
                return originalName;
            }

            void setOriginalName(String originalName) {
                this.originalName = originalName;
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
                MobOverridesListScreen screen = list.screen;
                int tableLeft = screen.getTableLeft();
                int openColX = tableLeft + screen.getColNameW();
                int removeColX = tableLeft + screen.getColNameW() + screen.getColOpenW();

                nameField.setX(tableLeft + 2);
                nameField.setY(top + 2);
                nameField.setWidth(screen.getColNameW() - 6);
                nameField.render(graphics, mouseX, mouseY, partialTick);

                openButton.setX(ConfigTableColumns.centeredX(openColX, screen.getColOpenW(), OPEN_BTN_WIDTH));
                openButton.setY(top + 2);
                openButton.render(graphics, mouseX, mouseY, partialTick);

                removeButton.setX(ConfigTableColumns.centeredX(removeColX, screen.getColRemoveW(), REMOVE_BTN_WIDTH));
                removeButton.setY(top + 2);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(nameField, openButton, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(nameField, openButton, removeButton);
            }
        }
    }
}
