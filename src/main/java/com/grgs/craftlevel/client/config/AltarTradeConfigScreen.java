package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.config.AltarTradeParser;
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

/** Lista de trocas do altar: Item | Glory Cost | +/-. */
@OnlyIn(Dist.CLIENT)
public class AltarTradeConfigScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;

    private final Screen parent;

    private OfferList offerList;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colItemW;
    private int colGloryW;
    private int colRemoveW;

    public AltarTradeConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Altar Trade"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AltarOfDeathPersistence.ensureSession(minecraft);
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        offerList = new OfferList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        for (AltarTradeParser.TradeEntry entry : AltarOfDeathPersistence.session().tradeOffers()) {
            offerList.addRow(entry.itemId(), entry.gloryCost());
        }
        addRenderableWidget(offerList);

        addRowButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)),
                        btn -> offerList.addRow("", 1))
                .bounds(0, 0, 16, 16)
                .build();
        addRenderableWidget(addRowButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutColumns() {
        tableWidth = Math.min(480, width - 40);
        tableLeft = (width - tableWidth) / 2;
        ConfigTableColumns.ThreeColumns cols = ConfigTableColumns.layout(tableWidth);
        colItemW = cols.primary();
        colGloryW = cols.secondary();
        colRemoveW = cols.remove();
    }

    void removeRow(OfferList.OfferEntry entry) {
        offerList.removeRow(entry);
    }

    private void save() {
        AltarOfDeathPersistence.session().setTradeOffers(offerList.buildOffers());
        AltarOfDeathPersistence.save(minecraft);
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
        int x = tableLeft;
        graphics.drawCenteredString(font, "Item", x + colItemW / 2, HEADER_Y + 6, 0xFFFFFF);
        x += colItemW;
        graphics.drawCenteredString(font, "Glory Cost", x + colGloryW / 2, HEADER_Y + 6, 0xFFFFFF);

        addRowButton.setX(ConfigTableColumns.centeredX(tableLeft + colItemW + colGloryW, colRemoveW, 16));
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    static class OfferList extends ContainerObjectSelectionList<OfferList.OfferEntry> {

        private final AltarTradeConfigScreen screen;

        OfferList(AltarTradeConfigScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            this.setRenderBackground(true);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(screen.getTableLeft());
        }

        void addRow(String itemId, int gloryCost) {
            addEntry(new OfferEntry(this, itemId, gloryCost));
        }

        void removeRow(OfferEntry entry) {
            removeEntry(entry);
        }

        List<AltarTradeParser.TradeEntry> buildOffers() {
            List<AltarTradeParser.TradeEntry> list = new ArrayList<>();
            for (OfferEntry entry : children()) {
                String id = entry.getItemId().trim();
                if (id.isEmpty()) {
                    continue;
                }
                list.add(new AltarTradeParser.TradeEntry(id, entry.getGloryCost(), 1));
            }
            return list;
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
        static class OfferEntry extends ContainerObjectSelectionList.Entry<OfferEntry> {

            private final OfferList list;
            private final EditBox itemField;
            private final EditBox gloryField;
            private final Button removeButton;

            OfferEntry(OfferList list, String itemId, int gloryCost) {
                this.list = list;
                AltarTradeConfigScreen screen = list.screen;

                itemField = new EditBox(screen.font, 0, 0, screen.getColItemW() - 4, 18, Component.empty());
                itemField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                itemField.setValue(itemId == null ? "" : itemId);

                gloryField = new EditBox(screen.font, 0, 0, screen.getColGloryW() - 4, 18, Component.empty());
                gloryField.setMaxLength(16);
                gloryField.setFilter(text -> text.isEmpty() || text.matches("\\d*"));
                gloryField.setValue(String.valueOf(Math.max(0, gloryCost)));

                removeButton = Button.builder(Component.literal("-").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.removeRow(this))
                        .bounds(0, 0, 16, 16)
                        .build();
            }

            String getItemId() {
                return itemField.getValue();
            }

            int getGloryCost() {
                try {
                    return Math.max(0, Integer.parseInt(gloryField.getValue().trim()));
                } catch (Exception e) {
                    return 0;
                }
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
                AltarTradeConfigScreen screen = list.screen;
                int x = screen.getTableLeft();

                itemField.setX(x + 2);
                itemField.setY(top + 1);
                itemField.setWidth(screen.getColItemW() - 4);
                itemField.render(graphics, mouseX, mouseY, partialTick);
                x += screen.getColItemW();

                gloryField.setX(x + 2);
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
