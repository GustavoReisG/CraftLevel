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

/** Editor de economy.json — saldos iniciais e overrides por profissão. */
@OnlyIn(Dist.CLIENT)
public class EconomyBalanceConfigScreen extends Screen {

    private static final int TOP_FIELD_ROWS = 3;
    private static final int HEADER_Y = ConfigScreenLayout.START_Y + TOP_FIELD_ROWS * ConfigScreenLayout.ROW_HEIGHT + 8;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;

    private final Screen parent;

    private EditBox playerBalanceField;
    private EditBox defaultBalanceField;
    private EditBox defaultRefreshField;
    private ProfessionList professionList;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colProfessionW;
    private int colBalanceW;
    private int colRefreshW;
    private int colRemoveW;

    public EconomyBalanceConfigScreen(Screen parent) {
        super(Component.literal("CraftLevel - Balance"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        EconomyBalancePersistence.beginEditing(minecraft);
        layoutColumns();

        EconomyBalanceEditorState state = EconomyBalancePersistence.session();
        playerBalanceField = createNumericField(String.valueOf(state.playerStartingBalance()));
        defaultBalanceField = createNumericField(String.valueOf(state.villagerDefaultStarting()));
        defaultRefreshField = createNumericField(String.valueOf(state.villagerRefreshTicks()));

        ConfigScreenLayout.positionField(this, 0, playerBalanceField);
        ConfigScreenLayout.positionField(this, 1, defaultBalanceField);
        ConfigScreenLayout.positionField(this, 2, defaultRefreshField);
        addRenderableWidget(playerBalanceField);
        addRenderableWidget(defaultBalanceField);
        addRenderableWidget(defaultRefreshField);

        int listBottom = ConfigScreenLayout.contentBottom(this);
        professionList = new ProfessionList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        populateList();
        addRenderableWidget(professionList);

        addRowButton = createPlusButton(this::addNewRow);
        addRenderableWidget(addRowButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::saveAndClose));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private EditBox createNumericField(String initial) {
        EditBox field = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.literal(""));
        field.setValue(initial);
        field.setFilter(text -> text.isEmpty() || text.matches("\\d+"));
        field.setMaxLength(12);
        return field;
    }

    private void layoutColumns() {
        tableWidth = Math.min(460, width - 40);
        tableLeft = (width - tableWidth) / 2;
        colProfessionW = (int) (tableWidth * 0.46f);
        colBalanceW = (int) (tableWidth * 0.22f);
        colRefreshW = (int) (tableWidth * 0.22f);
        colRemoveW = tableWidth - colProfessionW - colBalanceW - colRefreshW;
    }

    private void populateList() {
        for (Map.Entry<String, EconomyBalanceEditorState.ProfessionEntry> entry
                : EconomyBalancePersistence.session().perProfession().entrySet()) {
            professionList.addRow(new ProfessionRowData(
                    entry.getKey(),
                    entry.getValue().balance(),
                    entry.getValue().refreshTicks()
            ));
        }
    }

    void addNewRow() {
        EconomyBalanceEditorState state = EconomyBalancePersistence.session();
        professionList.addRow(new ProfessionRowData(
                "minecraft:",
                state.villagerDefaultStarting(),
                state.villagerRefreshTicks()
        ));
    }

    void removeRow(ProfessionList.ProfessionRowEntry entry) {
        professionList.removeRow(entry);
    }

    private void saveAndClose() {
        EconomyBalanceEditorState state = EconomyBalancePersistence.session();
        state.setPlayerStartingBalance(parseLong(playerBalanceField.getValue(), 0L));
        state.setVillagerDefaultStarting(parseInt(defaultBalanceField.getValue(), 1000));
        state.setVillagerRefreshTicks(parseInt(defaultRefreshField.getValue(), 24000));
        state.clearProfessions();
        state.perProfession().putAll(professionList.buildProfessions());
        EconomyBalancePersistence.save(minecraft);
        onClose();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Button createPlusButton(Runnable action) {
        return Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)), btn -> action.run())
                .bounds(0, 0, 16, 16)
                .build();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);

        ConfigScreenLayout.renderLabel(this, graphics, 0, "Player Start");
        ConfigScreenLayout.renderLabel(this, graphics, 1, "Balance");
        ConfigScreenLayout.renderLabel(this, graphics, 2, "Default Refresh");

        graphics.fill(tableLeft, HEADER_Y, tableLeft + tableWidth, HEADER_Y + HEADER_HEIGHT, 0x88000000);
        drawCenteredHeaderCell(graphics, "Profession", tableLeft, colProfessionW);
        drawCenteredHeaderCell(graphics, "Balance", tableLeft + colProfessionW, colBalanceW);
        drawCenteredHeaderCell(graphics, "Refresh", tableLeft + colProfessionW + colBalanceW, colRefreshW);

        int removeColX = tableLeft + colProfessionW + colBalanceW + colRefreshW;
        addRowButton.setX(removeColX + (colRemoveW - 16) / 2);
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredHeaderCell(GuiGraphics graphics, String label, int x, int colWidth) {
        graphics.drawCenteredString(font, label, x + colWidth / 2, HEADER_Y + 6, 0xFFFFFF);
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

    int getColBalanceW() {
        return colBalanceW;
    }

    int getColRefreshW() {
        return colRefreshW;
    }

    int getColRemoveW() {
        return colRemoveW;
    }

    @OnlyIn(Dist.CLIENT)
    static class ProfessionList extends ContainerObjectSelectionList<ProfessionList.ProfessionRowEntry> {

        private final EconomyBalanceConfigScreen screen;

        ProfessionList(EconomyBalanceConfigScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            setRenderBackground(true);
            setRenderTopAndBottom(false);
            setLeftPos(screen.getTableLeft());
        }

        void addRow(ProfessionRowData data) {
            addEntry(new ProfessionRowEntry(this, data));
        }

        void removeRow(ProfessionRowEntry entry) {
            removeEntry(entry);
        }

        LinkedHashMap<String, EconomyBalanceEditorState.ProfessionEntry> buildProfessions() {
            LinkedHashMap<String, EconomyBalanceEditorState.ProfessionEntry> professions = new LinkedHashMap<>();
            for (ProfessionRowEntry entry : children()) {
                String professionId = entry.getProfessionId().trim();
                if (professionId.isEmpty()) {
                    continue;
                }
                Integer balance = entry.getBalance();
                Integer refreshTicks = entry.getRefreshTicks();
                professions.put(professionId, new EconomyBalanceEditorState.ProfessionEntry(
                        balance != null ? balance : screen.parseDefaultBalance(),
                        refreshTicks != null ? refreshTicks : screen.parseDefaultRefresh()
                ));
            }
            return professions;
        }

        @Override
        public int getRowWidth() {
            return screen.getTableWidth();
        }

        @Override
        protected int getScrollbarPosition() {
            return screen.getTableLeft() + screen.getTableWidth() - 6;
        }

        @OnlyIn(Dist.CLIENT)
        static class ProfessionRowEntry extends Entry<ProfessionRowEntry> {

            private final ProfessionList list;
            private final EditBox professionField;
            private final EditBox balanceField;
            private final EditBox refreshField;
            private final Button removeButton;

            ProfessionRowEntry(ProfessionList list, ProfessionRowData data) {
                this.list = list;
                EconomyBalanceConfigScreen screen = list.screen;

                professionField = new EditBox(
                        screen.font,
                        0,
                        0,
                        screen.getColProfessionW() - 6,
                        18,
                        Component.literal("Profession")
                );
                professionField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                professionField.setValue(data.professionId());

                balanceField = new EditBox(
                        screen.font,
                        0,
                        0,
                        screen.getColBalanceW() - 6,
                        18,
                        Component.literal("Balance")
                );
                balanceField.setValue(data.balance() != null ? String.valueOf(data.balance()) : "");
                balanceField.setFilter(text -> text.isEmpty() || text.matches("\\d+"));
                balanceField.setMaxLength(8);

                refreshField = new EditBox(
                        screen.font,
                        0,
                        0,
                        screen.getColRefreshW() - 6,
                        18,
                        Component.literal("Refresh")
                );
                refreshField.setValue(data.refreshTicks() != null ? String.valueOf(data.refreshTicks()) : "");
                refreshField.setFilter(text -> text.isEmpty() || text.matches("\\d+"));
                refreshField.setMaxLength(8);

                removeButton = Button.builder(
                                Component.literal("-").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.removeRow(this))
                        .bounds(0, 0, 18, 18)
                        .build();
            }

            String getProfessionId() {
                return professionField.getValue();
            }

            Integer getBalance() {
                return ConfigScalingForm.parseOptionalInt(balanceField);
            }

            Integer getRefreshTicks() {
                return ConfigScalingForm.parseOptionalInt(refreshField);
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
                EconomyBalanceConfigScreen screen = list.screen;
                int tableLeft = screen.getTableLeft();

                professionField.setX(tableLeft + 2);
                professionField.setY(top + 2);
                balanceField.setX(tableLeft + screen.getColProfessionW() + 2);
                balanceField.setY(top + 2);
                refreshField.setX(tableLeft + screen.getColProfessionW() + screen.getColBalanceW() + 2);
                refreshField.setY(top + 2);
                removeButton.setX(tableLeft + screen.getColProfessionW() + screen.getColBalanceW() + screen.getColRefreshW()
                        + (screen.getColRemoveW() - 18) / 2);
                removeButton.setY(top + 2);

                professionField.render(graphics, mouseX, mouseY, partialTick);
                balanceField.render(graphics, mouseX, mouseY, partialTick);
                refreshField.render(graphics, mouseX, mouseY, partialTick);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(professionField, balanceField, refreshField, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(professionField, balanceField, refreshField, removeButton);
            }
        }
    }

    int parseDefaultBalance() {
        return parseInt(defaultBalanceField.getValue(), 1000);
    }

    int parseDefaultRefresh() {
        return parseInt(defaultRefreshField.getValue(), 24000);
    }
}
