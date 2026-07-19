package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
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

/** Lista editável de uma seção (attribute, hurt ou heal). */
@OnlyIn(Dist.CLIENT)
public class StatWiringSectionScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;

    private static final int OPEN_BTN_WIDTH = 20;
    private static final int REMOVE_BTN_WIDTH = 18;

    private final Screen parent;
    private final StatWiringEditor editor;
    private final StatWiringEditorState.Section section;

    private WiringList wiringList;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colKeyW;
    private int colScalerW;
    private int colRemoveW;

    protected StatWiringSectionScreen(Screen parent, StatWiringEditor editor, StatWiringEditorState.Section section) {
        super(Component.literal("CraftLevel - " + sectionTitle(section)));
        this.parent = parent;
        this.editor = editor;
        this.section = section;
    }

    @Override
    protected void init() {
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        wiringList = new WiringList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        populateList();
        addRenderableWidget(wiringList);

        addRowButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)), btn -> addNewRow())
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
        colKeyW = cols.primary();
        colScalerW = cols.secondary();
        colRemoveW = cols.remove();
    }

    private void populateList() {
        Map<String, String> entries = editor.state().section(section);
        if (section.requiresDefault()) {
            wiringList.addRow(new StatWiringRowData(
                    ConfigParser.DEFAULT_KEY,
                    entries.getOrDefault(ConfigParser.DEFAULT_KEY, ""),
                    false
            ));
        }
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (section.requiresDefault() && ConfigParser.DEFAULT_KEY.equals(entry.getKey())) {
                continue;
            }
            wiringList.addRow(new StatWiringRowData(entry.getKey(), entry.getValue(), true));
        }
    }

    void addNewRow() {
        wiringList.addRow(new StatWiringRowData("", "", true));
    }

    void removeRow(WiringList.WiringRowEntry entry) {
        wiringList.removeRow(entry);
    }

    private void save() {
        LinkedHashMap<String, String> entries = wiringList.buildEntries(section);
        editor.state().setSection(section, entries);
        editor.save();
    }

    private static String sectionTitle(StatWiringEditorState.Section section) {
        return switch (section) {
            case ATTRIBUTE -> "Attribute";
            case HURT -> "Hurt";
            case HURT_SELF -> "Hurt Self (alvo; default só sem atacante)";
            case HEAL -> "Heal";
        };
    }

    private static String keyColumnLabel(StatWiringEditorState.Section section) {
        return switch (section) {
            case ATTRIBUTE -> "Attribute";
            case HURT, HURT_SELF -> "Damage Type";
            case HEAL -> "Key";
        };
    }

    int getTableLeft() {
        return tableLeft;
    }

    int getTableWidth() {
        return tableWidth;
    }

    int getColKeyW() {
        return colKeyW;
    }

    int getColScalerW() {
        return colScalerW;
    }

    int getColRemoveW() {
        return colRemoveW;
    }

    StatWiringEditorState.Section getSection() {
        return section;
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
        drawCenteredHeaderCell(graphics, keyColumnLabel(section), tableLeft, colKeyW);
        drawCenteredHeaderCell(graphics, "Scaler", tableLeft + colKeyW, colScalerW);

        int removeColX = tableLeft + colKeyW + colScalerW;
        addRowButton.setX(ConfigTableColumns.centeredX(removeColX, colRemoveW, 16));
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredHeaderCell(GuiGraphics graphics, String label, int x, int colWidth) {
        graphics.drawCenteredString(font, label, x + colWidth / 2, HEADER_Y + 6, 0xFFFFFF);
    }

    @OnlyIn(Dist.CLIENT)
    static class WiringList extends ContainerObjectSelectionList<WiringList.WiringRowEntry> {

        private final StatWiringSectionScreen screen;

        WiringList(StatWiringSectionScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            this.setRenderBackground(true);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(screen.getTableLeft());
        }

        void addRow(StatWiringRowData data) {
            addEntry(new WiringRowEntry(this, data));
        }

        void removeRow(WiringRowEntry entry) {
            removeEntry(entry);
        }

        LinkedHashMap<String, String> buildEntries(StatWiringEditorState.Section section) {
            LinkedHashMap<String, String> entries = new LinkedHashMap<>();
            for (WiringRowEntry entry : children()) {
                String key = entry.getKey().trim();
                if (key.isEmpty()) {
                    continue;
                }
                entries.put(key, entry.getScaler().trim());
            }
            if (section.requiresDefault() && !entries.containsKey(ConfigParser.DEFAULT_KEY)) {
                entries.put(ConfigParser.DEFAULT_KEY, "");
            }
            return entries;
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
        static class WiringRowEntry extends Entry<WiringRowEntry> {

            private final WiringList list;
            private final boolean removable;
            private final EditBox keyField;
            private final CycleButton<String> scalerButton;
            private final Button removeButton;

            WiringRowEntry(WiringList list, StatWiringRowData data) {
                this.list = list;
                this.removable = data.removable();
                StatWiringSectionScreen screen = list.screen;

                keyField = new EditBox(
                        screen.font,
                        0,
                        0,
                        screen.getColKeyW() - 6,
                        18,
                        Component.literal("Key")
                );
                keyField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                keyField.setValue(data.key());
                keyField.setEditable(removable);

                List<String> scalerOptions = ScalerOptions.optionsIncluding(screen.minecraft, data.scaler());
                String initialScaler = ScalerOptions.resolveInitial(data.scaler(), scalerOptions);
                scalerButton = CycleButton.<String>builder(WiringRowEntry::formatScaler)
                        .withValues(scalerOptions.toArray(new String[0]))
                        .withInitialValue(initialScaler)
                        .displayOnlyValue()
                        .create(0, 0, screen.getColScalerW() - 6, 18, Component.empty());

                removeButton = Button.builder(
                                Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.removeRow(this))
                        .bounds(0, 0, REMOVE_BTN_WIDTH, 18)
                        .build();
            }

            private static Component formatScaler(String value) {
                return Component.literal(value.isEmpty() ? "-" : value);
            }

            String getKey() {
                return keyField.getValue();
            }

            String getScaler() {
                return scalerButton.getValue();
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
                StatWiringSectionScreen screen = list.screen;
                int tableLeft = screen.getTableLeft();
                int removeColX = tableLeft + screen.getColKeyW() + screen.getColScalerW();

                keyField.setX(tableLeft + 2);
                keyField.setY(top + 2);
                scalerButton.setX(tableLeft + screen.getColKeyW() + 2);
                scalerButton.setY(top + 2);

                keyField.render(graphics, mouseX, mouseY, partialTick);
                scalerButton.render(graphics, mouseX, mouseY, partialTick);

                if (removable) {
                    removeButton.setX(ConfigTableColumns.centeredX(removeColX, screen.getColRemoveW(), REMOVE_BTN_WIDTH));
                    removeButton.setY(top + 2);
                    removeButton.render(graphics, mouseX, mouseY, partialTick);
                }
            }

            @Override
            public List<? extends GuiEventListener> children() {
                if (removable) {
                    return List.of(keyField, scalerButton, removeButton);
                }
                return List.of(keyField, scalerButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                if (removable) {
                    return List.of(keyField, scalerButton, removeButton);
                }
                return List.of(keyField, scalerButton);
            }
        }
    }
}
