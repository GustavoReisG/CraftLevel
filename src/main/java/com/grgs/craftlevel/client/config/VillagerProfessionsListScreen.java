package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.villager.ProfessionIds;
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

/** Lista profissões custom em villager.json. */
@OnlyIn(Dist.CLIENT)
public class VillagerProfessionsListScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;

    private final Screen parent;

    private ProfessionList professionList;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colProfessionW;
    private int colConfigW;
    private int colRemoveW;

    public VillagerProfessionsListScreen(Screen parent) {
        super(Component.literal("Professions"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        VillagerPersistence.beginEditing(minecraft);
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        professionList = new ProfessionList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        VillagerPersistence.session().professions().keySet().forEach(professionList::addRow);
        addRenderableWidget(professionList);

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
        String path = "new_profession";
        String fullId = ProfessionIds.fullIdFromInput(path);
        VillagerPersistence.session().setProfession(fullId,
                new com.grgs.craftlevel.villager.VillagerProfessionDef("minecraft:smithing_table"));
        professionList.addRow(fullId);
    }

    void openProfession(String id) {
        minecraft.setScreen(new VillagerProfessionDetailScreen(this, id));
    }

    void removeProfession(String id) {
        VillagerPersistence.session().removeProfession(id);
    }

    void removeRow(ProfessionList.ProfessionEntry entry) {
        professionList.removeRow(entry);
    }

    private void save() {
        professionList.syncAllIds();
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
        drawCenteredHeaderCell(graphics, "ID (craftlevel:*)", tableLeft, colProfessionW);
        drawCenteredHeaderCell(graphics, "Config", tableLeft + colProfessionW, colConfigW);

        int removeColX = tableLeft + colProfessionW + colConfigW;
        addRowButton.setX(ConfigTableColumns.centeredX(removeColX, colRemoveW, 16));
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCenteredHeaderCell(GuiGraphics graphics, String label, int x, int colWidth) {
        graphics.drawCenteredString(font, label, x + colWidth / 2, HEADER_Y + 6, 0xFFFFFF);
    }

    @OnlyIn(Dist.CLIENT)
    private static class ProfessionList extends ContainerObjectSelectionList<ProfessionList.ProfessionEntry> {
        private final VillagerProfessionsListScreen screen;

        ProfessionList(VillagerProfessionsListScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            setRenderBackground(true);
            setRenderTopAndBottom(false);
            setLeftPos(screen.getTableLeft());
        }

        void addRow(String id) {
            addEntry(new ProfessionEntry(this, id));
        }

        void removeRow(ProfessionEntry entry) {
            removeEntry(entry);
        }

        void syncAllIds() {
            for (ProfessionEntry entry : children()) {
                entry.syncId();
            }
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
        private static class ProfessionEntry extends Entry<ProfessionEntry> {
            private final ProfessionList list;
            private String fullId;
            private final EditBox idField;
            private final Button openButton;
            private final Button removeButton;

            ProfessionEntry(ProfessionList list, String id) {
                this.list = list;
                this.fullId = id;
                VillagerProfessionsListScreen screen = list.screen;

                idField = new EditBox(screen.minecraft.font, 0, 0, screen.getColProfessionW() - 6, 18, Component.literal(""));
                idField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                idField.setValue(ProfessionIds.pathFromFullId(id));

                openButton = Button.builder(Component.literal("..."), btn -> {
                    syncId();
                    screen.openProfession(fullId);
                }).bounds(0, 0, 20, 18).build();

                removeButton = Button.builder(
                                Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> {
                                    syncId();
                                    screen.removeProfession(fullId);
                                    screen.removeRow(this);
                                })
                        .bounds(0, 0, 18, 18)
                        .build();
            }

            private void syncId() {
                String path = ProfessionIds.pathFromInput(idField.getValue());
                String newId = ProfessionIds.fullIdFromInput(path);
                idField.setValue(path);
                if (newId.equals(fullId)) {
                    return;
                }
                var session = VillagerPersistence.session();
                var def = session.professions().get(fullId);
                if (def != null) {
                    session.removeProfession(fullId);
                    session.setProfession(newId, new com.grgs.craftlevel.villager.VillagerProfessionDef(
                            def.station()
                    ));
                    var trades = session.trades().remove(fullId);
                    if (trades != null) {
                        session.trades().put(newId, trades);
                    }
                }
                fullId = newId;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                VillagerProfessionsListScreen screen = list.screen;
                int tableLeft = screen.getTableLeft();

                int configColX = tableLeft + screen.getColProfessionW();
                int removeColX = configColX + screen.getColConfigW();

                idField.setX(tableLeft + 2);
                idField.setY(top + 2);
                openButton.setX(ConfigTableColumns.centeredX(configColX, screen.getColConfigW(), 20));
                openButton.setY(top + 2);
                removeButton.setX(ConfigTableColumns.centeredX(removeColX, screen.getColRemoveW(), 18));
                removeButton.setY(top + 2);

                idField.render(graphics, mouseX, mouseY, partialTick);
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
