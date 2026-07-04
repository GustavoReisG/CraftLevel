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

/** Lista de variáveis em scaling.{nome}. */
@OnlyIn(Dist.CLIENT)
public class ScalingVariablesScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;
    private static final int OPEN_BTN_WIDTH = 20;
    private static final int REMOVE_BTN_WIDTH = 18;

    private final Screen parent;

    private VariablesList variablesList;
    private Button addButton;

    private int tableLeft;
    private int tableWidth;
    private int colNameW;
    private int colOpenW;
    private int colRemoveW;

    protected ScalingVariablesScreen(Screen parent) {
        super(Component.literal("CraftLevel - Scaling"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        variablesList = new VariablesList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        populateList();
        addRenderableWidget(variablesList);

        addButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)), btn -> addVariable())
                .bounds(0, 0, 16, 16)
                .build();
        addRenderableWidget(addButton);

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
        for (String name : ConfigPersistence.session().scalers().keySet()) {
            variablesList.addEntry(name);
        }
    }

    void openVariable(VariablesList.VariableEntry entry) {
        syncEntryToSession(entry);
        String name = entry.getName().trim();
        if (name.isEmpty()) {
            return;
        }
        minecraft.setScreen(new ScalingVariableConfigScreen(this, name));
    }

    void addVariable() {
        String name = uniqueName("variable");
        ConfigPersistence.session().putScaler(name, ConfigEditorState.ScalerEntry.empty());
        variablesList.addEntry(name);
    }

    void removeVariable(VariablesList.VariableEntry entry) {
        syncEntryToSession(entry);
        ConfigPersistence.session().removeScaler(entry.getOriginalName());
        variablesList.removeRow(entry);
    }

    void syncEntryToSession(VariablesList.VariableEntry entry) {
        String newName = entry.getName().trim();
        if (newName.isEmpty()) {
            return;
        }
        String oldName = entry.getOriginalName();
        if (newName.equals(oldName)) {
            return;
        }
        ConfigEditorState state = ConfigPersistence.session();
        ConfigEditorState.ScalerEntry data = state.scaler(oldName);
        if (data == null) {
            data = ConfigEditorState.ScalerEntry.empty();
        }
        state.removeScaler(oldName);
        state.putScaler(newName, data);
        entry.setOriginalName(newName);
    }

    private void save() {
        variablesList.rebuildSession();
        ConfigPersistence.save(minecraft);
    }

    private static String uniqueName(String base) {
        ConfigEditorState state = ConfigPersistence.session();
        if (!state.scalers().containsKey(base)) {
            return base;
        }
        int index = 2;
        while (state.scalers().containsKey(base + index)) {
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
        drawCenteredHeaderCell(graphics, "Scaler Name", tableLeft, colNameW);
        drawCenteredHeaderCell(graphics, "Scaling", tableLeft + colNameW, colOpenW);

        int removeColX = tableLeft + colNameW + colOpenW;
        addButton.setX(ConfigTableColumns.centeredX(removeColX, colRemoveW, 16));
        addButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredHeaderCell(GuiGraphics graphics, String label, int x, int colWidth) {
        graphics.drawCenteredString(font, label, x + colWidth / 2, HEADER_Y + 6, 0xFFFFFF);
    }

    @OnlyIn(Dist.CLIENT)
    static class VariablesList extends ContainerObjectSelectionList<VariablesList.VariableEntry> {

        private final ScalingVariablesScreen screen;

        VariablesList(ScalingVariablesScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 24);
            this.screen = screen;
            this.setRenderBackground(true);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(screen.getTableLeft());
        }

        void addEntry(String name) {
            addEntry(new VariableEntry(this, name));
        }

        void removeRow(VariableEntry entry) {
            removeEntry(entry);
        }

        void rebuildSession() {
            LinkedHashMap<String, ConfigEditorState.ScalerEntry> map = new LinkedHashMap<>();
            ConfigEditorState state = ConfigPersistence.session();
            for (VariableEntry entry : children()) {
                screen.syncEntryToSession(entry);
                String name = entry.getName().trim();
                if (name.isEmpty()) {
                    continue;
                }
                ConfigEditorState.ScalerEntry data = state.scaler(entry.getOriginalName());
                if (data == null) {
                    data = ConfigEditorState.ScalerEntry.empty();
                }
                map.put(name, data);
            }
            state.scalers().clear();
            state.scalers().putAll(map);
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
        static class VariableEntry extends Entry<VariableEntry> {

            private final VariablesList list;
            private String originalName;
            private final EditBox nameField;
            private final Button openButton;
            private final Button removeButton;

            VariableEntry(VariablesList list, String name) {
                this.list = list;
                this.originalName = name;
                ScalingVariablesScreen screen = list.screen;

                nameField = new EditBox(
                        screen.minecraft.font,
                        0,
                        0,
                        screen.getColNameW() - 6,
                        18,
                        Component.empty()
                );
                nameField.setValue(name);
                nameField.setMaxLength(64);

                openButton = Button.builder(Component.literal("..."), btn -> screen.openVariable(this))
                        .bounds(0, 0, OPEN_BTN_WIDTH, 18)
                        .build();

                removeButton = Button.builder(
                                Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.removeVariable(this))
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
                ScalingVariablesScreen screen = list.screen;
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
