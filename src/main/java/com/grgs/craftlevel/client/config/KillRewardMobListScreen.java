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
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Editor de lista de mobs em kill_rewards (bosses ou blacklist). */
@OnlyIn(Dist.CLIENT)
public class KillRewardMobListScreen extends Screen {

    enum Kind {
        BOSSES(
                "CraftLevel - Boss List",
                "Bosses",
                () -> KillRewardsPersistence.session().bosses(),
                ids -> KillRewardsPersistence.session().setBosses(ids)
        ),
        BLACKLIST(
                "CraftLevel - Blacklist",
                "No emerald reward",
                () -> KillRewardsPersistence.session().blacklist(),
                ids -> KillRewardsPersistence.session().setBlacklist(ids)
        );

        private final String title;
        private final String header;
        private final Supplier<List<String>> load;
        private final Consumer<List<String>> store;

        Kind(String title, String header, Supplier<List<String>> load, Consumer<List<String>> store) {
            this.title = title;
            this.header = header;
            this.load = load;
            this.store = store;
        }
    }

    private final Screen parent;
    private final Kind kind;
    private MobList mobList;
    private Button addButton;
    private int tableLeft;
    private int tableWidth;
    private int colMobW;
    private int colRemoveW;

    public KillRewardMobListScreen(Screen parent, Kind kind) {
        super(Component.literal(kind.title));
        this.parent = parent;
        this.kind = kind;
    }

    @Override
    protected void init() {
        KillRewardsPersistence.ensureSession(minecraft);
        layoutTable();

        int listTop = 48;
        int listBottom = ConfigScreenLayout.contentBottom(this);
        mobList = new MobList(this, tableWidth, listBottom - listTop, listTop, listBottom);
        for (String mob : kind.load.get()) {
            mobList.addRow(mob);
        }
        addRenderableWidget(mobList);

        addButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)),
                        btn -> mobList.addRow(""))
                .bounds(0, 0, 16, 16)
                .build();
        addRenderableWidget(addButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutTable() {
        tableWidth = Math.min(420, width - 40);
        tableLeft = (width - tableWidth) / 2;
        colMobW = (int) (tableWidth * 0.82f);
        colRemoveW = tableWidth - colMobW;
    }

    private void save() {
        kind.store.accept(mobList.buildIds());
        KillRewardsPersistence.save(minecraft);
    }

    void removeMob(MobList.MobEntry entry) {
        mobList.removeRow(entry);
    }

    int getTableLeft() {
        return tableLeft;
    }

    int getColMobW() {
        return colMobW;
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
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        int headerY = 28;
        graphics.fill(tableLeft, headerY, tableLeft + tableWidth, headerY + 20, 0x88000000);
        graphics.drawCenteredString(font, kind.header, tableLeft + colMobW / 2, headerY + 6, 0xFFFFFF);
        addButton.setX(tableLeft + colMobW + (colRemoveW - 16) / 2);
        addButton.setY(headerY + 2);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    static class MobList extends ContainerObjectSelectionList<MobList.MobEntry> {

        private final KillRewardMobListScreen screen;

        MobList(KillRewardMobListScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            this.setRenderBackground(true);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(screen.getTableLeft());
        }

        void addRow(String mobId) {
            addEntry(new MobEntry(this, mobId));
        }

        void removeRow(MobEntry entry) {
            removeEntry(entry);
        }

        List<String> buildIds() {
            List<String> ids = new ArrayList<>();
            for (MobEntry entry : children()) {
                String id = entry.getMobId().trim();
                if (!id.isEmpty()) {
                    ids.add(id);
                }
            }
            return ids;
        }

        @Override
        public int getRowWidth() {
            return screen.tableWidth;
        }

        @Override
        protected int getScrollbarPosition() {
            return screen.getTableLeft() + screen.tableWidth;
        }

        @OnlyIn(Dist.CLIENT)
        static class MobEntry extends ContainerObjectSelectionList.Entry<MobEntry> {

            private final MobList list;
            private final EditBox mobField;
            private final Button removeButton;

            MobEntry(MobList list, String mobId) {
                this.list = list;
                KillRewardMobListScreen screen = list.screen;

                mobField = new EditBox(screen.font, 0, 0, screen.getColMobW() - 4, 18, Component.empty());
                mobField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                mobField.setValue(mobId == null ? "" : mobId);

                removeButton = Button.builder(Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.removeMob(this))
                        .bounds(0, 0, 16, 16)
                        .build();
            }

            String getMobId() {
                return mobField.getValue();
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
                KillRewardMobListScreen screen = list.screen;
                mobField.setX(screen.getTableLeft() + 2);
                mobField.setY(top + 1);
                mobField.setWidth(screen.getColMobW() - 4);
                mobField.render(graphics, mouseX, mouseY, partialTick);

                removeButton.setX(screen.getTableLeft() + screen.getColMobW() + (screen.getColRemoveW() - 16) / 2);
                removeButton.setY(top + 2);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(mobField, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(mobField, removeButton);
            }
        }
    }
}
