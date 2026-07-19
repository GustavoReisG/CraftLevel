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

/** Lista profissões com trades em villager.json. */
@OnlyIn(Dist.CLIENT)
public class VillagerTradesListScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;

    private final Screen parent;

    private TradeProfessionList list;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colProfessionW;
    private int colConfigW;
    private int colRemoveW;

    public VillagerTradesListScreen(Screen parent) {
        super(Component.literal("Trades"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        VillagerPersistence.beginEditing(minecraft);
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        list = new TradeProfessionList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        new ArrayList<>(VillagerPersistence.session().trades().keySet()).forEach(list::addRow);
        addRenderableWidget(list);

        addRowButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)), btn -> addProfession())
                .bounds(0, 0, 16, 16)
                .build();
        addRenderableWidget(addRowButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutColumns() {
        colConfigW = 70;
        colRemoveW = 28;
        colProfessionW = Math.min(240, Math.max(160, width - colConfigW - colRemoveW - 40));
        tableWidth = colProfessionW + colConfigW + colRemoveW;
        tableLeft = (width - tableWidth) / 2;
    }

    private void addProfession() {
        String id = "minecraft:";
        VillagerPersistence.session().tradesFor(id);
        list.addRow(id);
    }

    void openTrades(String professionId) {
        minecraft.setScreen(new VillagerTradeListScreen(this, professionId));
    }

    void removeProfession(String professionId) {
        VillagerPersistence.session().trades().remove(professionId);
    }

    void removeRow(TradeProfessionList.TradeEntry entry) {
        list.removeRow(entry);
    }

    private void save() {
        VillagerPersistence.save(minecraft);
    }

    int getTableLeft() {
        return tableLeft;
    }

    int getTableWidth() {
        return tableWidth;
    }

    int getColProfessionW() {
        return colProfessionW;
    }

    int getColConfigW() {
        return colConfigW;
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
        drawCenteredHeaderCell(graphics, "Professions", tableLeft, colProfessionW);
        drawCenteredHeaderCell(graphics, "Trades", tableLeft + colProfessionW, colConfigW);

        int removeColX = tableLeft + colProfessionW + colConfigW;
        addRowButton.setX(ConfigTableColumns.centeredX(removeColX, colRemoveW, 16));
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredHeaderCell(GuiGraphics graphics, String label, int x, int colWidth) {
        graphics.drawCenteredString(font, label, x + colWidth / 2, HEADER_Y + 6, 0xFFFFFF);
    }

    @OnlyIn(Dist.CLIENT)
    private static class TradeProfessionList extends ContainerObjectSelectionList<TradeProfessionList.TradeEntry> {
        private final VillagerTradesListScreen screen;

        TradeProfessionList(VillagerTradesListScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            setRenderBackground(true);
            setRenderTopAndBottom(false);
            setLeftPos(screen.getTableLeft());
        }

        void addRow(String id) {
            addEntry(new TradeEntry(this, id));
        }

        void removeRow(TradeEntry entry) {
            removeEntry(entry);
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
        private static class TradeEntry extends Entry<TradeEntry> {
            private final TradeProfessionList list;
            private final EditBox idField;
            private final Button openButton;
            private final Button removeButton;

            TradeEntry(TradeProfessionList list, String id) {
                this.list = list;
                VillagerTradesListScreen screen = list.screen;

                idField = new EditBox(screen.minecraft.font, 0, 0, screen.getColProfessionW() - 6, 18, Component.literal(""));
                idField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                idField.setValue(id);

                openButton = Button.builder(Component.literal("..."), btn ->
                        screen.openTrades(idField.getValue().trim())).bounds(0, 0, 20, 18).build();

                removeButton = Button.builder(
                                Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> {
                                    screen.removeProfession(idField.getValue().trim());
                                    screen.removeRow(this);
                                })
                        .bounds(0, 0, 18, 18)
                        .build();
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                VillagerTradesListScreen screen = list.screen;
                int tableLeft = screen.getTableLeft();
                int configColX = tableLeft + screen.getColProfessionW();
                int removeColX = configColX + screen.getColConfigW();
                int count = VillagerPersistence.session().tradesFor(idField.getValue().trim()).size();

                idField.setX(tableLeft + 2);
                idField.setY(top + 2);
                openButton.setX(ConfigTableColumns.centeredX(configColX, screen.getColConfigW(), 20));
                openButton.setY(top + 2);
                removeButton.setX(ConfigTableColumns.centeredX(removeColX, screen.getColRemoveW(), 18));
                removeButton.setY(top + 2);

                idField.render(graphics, mouseX, mouseY, partialTick);
                graphics.drawString(screen.minecraft.font, "(" + count + ")", tableLeft + screen.getColProfessionW() - 36, top + 6, 0xAAAAAA);
                openButton.render(graphics, mouseX, mouseY, partialTick);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(idField, openButton, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(idField, openButton, removeButton);
            }
        }
    }
}
