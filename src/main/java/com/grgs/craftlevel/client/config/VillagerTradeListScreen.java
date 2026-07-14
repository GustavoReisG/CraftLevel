package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.villager.VillagerTradeDef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Lista trades de uma profissão. */
@OnlyIn(Dist.CLIENT)
public class VillagerTradeListScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;
    private static final int OPEN_BTN_WIDTH = 48;
    private static final int REMOVE_BTN_WIDTH = 18;
    private static final int ICON_SIZE = 16;
    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final String professionId;

    private TradeList list;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colInput1W;
    private int colInput2W;
    private int colOutputW;
    private int colConfigW;
    private int colRemoveW;

    public VillagerTradeListScreen(Screen parent, String professionId) {
        super(Component.literal(professionId));
        this.parent = parent;
        this.professionId = professionId;
    }

    @Override
    protected void init() {
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        list = new TradeList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        List<VillagerTradeDef> trades = VillagerPersistence.session().tradesFor(professionId);
        for (int i = 0; i < trades.size(); i++) {
            list.addRow(i);
        }
        addRenderableWidget(list);

        addRowButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)), btn -> addTrade())
                .bounds(0, 0, 16, 16)
                .build();
        addRenderableWidget(addRowButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, () -> VillagerPersistence.save(minecraft)));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutColumns() {
        colInput1W = 65;
        colInput2W = 65;
        colOutputW = 65;
        colConfigW = 70;
        colRemoveW = 30;
        tableWidth = colInput1W + colInput2W + colOutputW + colConfigW + colRemoveW;
        tableLeft = (width - tableWidth) / 2;
    }

    private void addTrade() {
        int index = VillagerPersistence.session().tradesFor(professionId).size();
        VillagerPersistence.session().addTrade(professionId, VillagerEditorState.emptyTrade());
        list.addRow(index);
    }

    void openTrade(int index) {
        minecraft.setScreen(new VillagerTradeDetailScreen(this, professionId, index));
    }

    void removeTrade(int index) {
        VillagerPersistence.session().removeTrade(professionId, index);
    }

    void removeRow(TradeList.TradeEntry entry) {
        list.removeRow(entry);
    }

    int getTableLeft() {
        return tableLeft;
    }

    int getTableWidth() {
        return tableWidth;
    }

    int getColInput1W() {
        return colInput1W;
    }

    int getColInput2W() {
        return colInput2W;
    }

    int getColOutputW() {
        return colOutputW;
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
        int x = tableLeft;
        drawCenteredHeaderCell(graphics, "Input 1", x, colInput1W);
        x += colInput1W;
        drawCenteredHeaderCell(graphics, "Input 2", x, colInput2W);
        x += colInput2W;
        drawCenteredHeaderCell(graphics, "Output", x, colOutputW);
        x += colOutputW;
        drawCenteredHeaderCell(graphics, "Config", x, colConfigW);

        int removeColX = tableLeft + colInput1W + colInput2W + colOutputW + colConfigW;
        addRowButton.setX(ConfigTableColumns.centeredX(removeColX, colRemoveW, 16));
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredHeaderCell(GuiGraphics graphics, String label, int x, int colWidth) {
        graphics.drawCenteredString(font, label, x + colWidth / 2, HEADER_Y + 6, 0xFFFFFF);
    }

    private static void renderSlotIcon(GuiGraphics graphics, Screen screen, ItemStack stack, int colX, int colW, int top) {
        if (stack.isEmpty()) {
            return;
        }
        int x = colX + (colW - ICON_SIZE) / 2;
        int y = top + 3;
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(screen.getMinecraft().font, stack, x, y);
    }

    @OnlyIn(Dist.CLIENT)
    private static class TradeList extends ContainerObjectSelectionList<TradeList.TradeEntry> {
        private final VillagerTradeListScreen screen;

        TradeList(VillagerTradeListScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, ROW_HEIGHT);
            this.screen = screen;
            setRenderBackground(true);
            setRenderTopAndBottom(false);
            setLeftPos(screen.getTableLeft());
        }

        void addRow(int index) {
            addEntry(new TradeEntry(this, index));
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
            private final TradeList list;
            private final int index;
            private final Button openButton;
            private final Button removeButton;

            TradeEntry(TradeList list, int index) {
                this.list = list;
                this.index = index;
                VillagerTradeListScreen screen = list.screen;

                openButton = Button.builder(Component.literal("Config"), btn ->
                        screen.openTrade(index)).bounds(0, 0, OPEN_BTN_WIDTH, 18).build();

                removeButton = Button.builder(
                                Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> {
                                    screen.removeTrade(index);
                                    screen.removeRow(this);
                                })
                        .bounds(0, 0, REMOVE_BTN_WIDTH, 18)
                        .build();
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                VillagerTradeListScreen screen = list.screen;
                VillagerTradeDef trade = VillagerPersistence.session().tradesFor(screen.professionId).get(this.index);

                int tableLeft = screen.getTableLeft();
                int x = tableLeft;

                renderSlotIcon(graphics, screen, TradeSlotIconHelper.preview(trade.input1()), x, screen.getColInput1W(), top);
                x += screen.getColInput1W();
                renderSlotIcon(graphics, screen, TradeSlotIconHelper.preview(trade.input2()), x, screen.getColInput2W(), top);
                x += screen.getColInput2W();
                renderSlotIcon(graphics, screen, TradeSlotIconHelper.preview(trade.output()), x, screen.getColOutputW(), top);
                x += screen.getColOutputW();

                openButton.setX(ConfigTableColumns.centeredX(x, screen.getColConfigW(), OPEN_BTN_WIDTH));
                openButton.setY(top + 2);

                int removeColX = tableLeft + screen.getColInput1W() + screen.getColInput2W()
                        + screen.getColOutputW() + screen.getColConfigW();
                removeButton.setX(ConfigTableColumns.centeredX(removeColX, screen.getColRemoveW(), REMOVE_BTN_WIDTH));
                removeButton.setY(top + 2);

                openButton.render(graphics, mouseX, mouseY, partialTick);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(openButton, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(openButton, removeButton);
            }
        }
    }
}
