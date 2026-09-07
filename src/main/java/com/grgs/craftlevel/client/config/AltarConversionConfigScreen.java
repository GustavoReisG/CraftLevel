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

import java.util.ArrayList;
import java.util.List;

/** Lista de itens convertíveis em Skill Points (altar_conversion.json). */
@OnlyIn(Dist.CLIENT)
public class AltarConversionConfigScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;

    private final Screen parent;

    private ConversionList conversionList;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colItemW;
    private int colGloryW;
    private int colRemoveW;

    public AltarConversionConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Altar Conversion"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AltarConversionPersistence.ensureSession(minecraft);
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        conversionList = new ConversionList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        for (AltarConversionEditorState.ConversionRow row : AltarConversionPersistence.session().rows()) {
            conversionList.addRow(row);
        }
        addRenderableWidget(conversionList);

        addRowButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)),
                        btn -> conversionList.addRow(new AltarConversionEditorState.ConversionRow("", 100, 1, 1)))
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
        colItemW = cols.primary();
        colGloryW = cols.secondary();
        colRemoveW = cols.remove();
    }

    void removeRow(ConversionList.ConversionEntry entry) {
        conversionList.removeRow(entry);
    }

    private void save() {
        AltarConversionPersistence.session().setRows(conversionList.buildRows());
        AltarConversionPersistence.save(minecraft);
    }

    int getTableLeft() {
        return tableLeft;
    }

    int getTableWidth() {
        return tableWidth;
    }

    int getColItemW() {
        return colItemW;
    }

    int getColGloryW() {
        return colGloryW;
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
        graphics.drawCenteredString(font, "Item ID", tableLeft + colItemW / 2, HEADER_Y + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, "Glory Cost", tableLeft + colItemW + colGloryW / 2, HEADER_Y + 6, 0xFFFFFF);

        addRowButton.setX(ConfigTableColumns.centeredX(tableLeft + colItemW + colGloryW, colRemoveW, 16));
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    static class ConversionList extends ContainerObjectSelectionList<ConversionList.ConversionEntry> {

        private final AltarConversionConfigScreen screen;

        ConversionList(AltarConversionConfigScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            this.setRenderBackground(true);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(screen.getTableLeft());
        }

        void addRow(AltarConversionEditorState.ConversionRow row) {
            addEntry(new ConversionEntry(this, row));
        }

        void removeRow(ConversionEntry entry) {
            removeEntry(entry);
        }

        List<AltarConversionEditorState.ConversionRow> buildRows() {
            List<AltarConversionEditorState.ConversionRow> rows = new ArrayList<>();
            for (ConversionEntry entry : children()) {
                String itemId = entry.getItemId().trim();
                if (itemId.isEmpty()) {
                    continue;
                }
                rows.add(new AltarConversionEditorState.ConversionRow(
                        itemId,
                        entry.getGloryCost(),
                        entry.getCount(),
                        entry.getSkillPoints()
                ));
            }
            return rows;
        }

        @Override
        public int getRowWidth() {
            return screen.getTableWidth();
        }

        @Override
        protected int getScrollbarPosition() {
            return screen.getTableLeft() + screen.getTableWidth();
        }

        @OnlyIn(Dist.CLIENT)
        static class ConversionEntry extends ContainerObjectSelectionList.Entry<ConversionEntry> {

            private final ConversionList list;
            private final EditBox itemField;
            private final EditBox gloryField;
            private final Button removeButton;
            private final int count;
            private final int skillPoints;

            ConversionEntry(ConversionList list, AltarConversionEditorState.ConversionRow row) {
                this.list = list;
                this.count = row.count();
                this.skillPoints = row.skillPoints();
                AltarConversionConfigScreen screen = list.screen;

                itemField = new EditBox(screen.font, 0, 0, screen.getColItemW() - 4, 18, Component.empty());
                itemField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                itemField.setValue(row.itemId() == null ? "" : row.itemId());

                gloryField = new EditBox(screen.font, 0, 0, screen.getColGloryW() - 4, 18, Component.empty());
                gloryField.setMaxLength(16);
                gloryField.setFilter(text -> text.isEmpty() || text.matches("\\d*"));
                gloryField.setValue(String.valueOf(row.gloryCost()));

                removeButton = Button.builder(Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.removeRow(this))
                        .bounds(0, 0, 16, 16)
                        .build();
            }

            String getItemId() {
                return itemField.getValue();
            }

            int getGloryCost() {
                try {
                    return Math.max(1, Integer.parseInt(gloryField.getValue().trim()));
                } catch (Exception e) {
                    return 1;
                }
            }

            int getCount() {
                return count;
            }

            int getSkillPoints() {
                return skillPoints;
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
                AltarConversionConfigScreen screen = list.screen;
                itemField.setX(screen.getTableLeft() + 2);
                itemField.setY(top + 1);
                itemField.setWidth(screen.getColItemW() - 4);
                itemField.render(graphics, mouseX, mouseY, partialTick);

                gloryField.setX(screen.getTableLeft() + screen.getColItemW() + 2);
                gloryField.setY(top + 1);
                gloryField.setWidth(screen.getColGloryW() - 4);
                gloryField.render(graphics, mouseX, mouseY, partialTick);

                removeButton.setX(ConfigTableColumns.centeredX(
                        screen.getTableLeft() + screen.getColItemW() + screen.getColGloryW(),
                        screen.getColRemoveW(),
                        16
                ));
                removeButton.setY(top + 2);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(itemField, gloryField, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(itemField, gloryField, removeButton);
            }
        }
    }
}
