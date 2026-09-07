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

/** Lista de limites por encantamento (modid:enchantment → max lvl). */
@OnlyIn(Dist.CLIENT)
public class AltarEnchantmentLimitListScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;

    private final Screen parent;

    private LimitList limitList;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colEnchantW;
    private int colLevelW;
    private int colRemoveW;

    public AltarEnchantmentLimitListScreen(Screen parent) {
        super(Component.literal("CraftLevel - Enchantment Limits"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AltarOfDeathPersistence.ensureSession(minecraft);
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        limitList = new LimitList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        for (Map.Entry<String, Integer> entry : AltarOfDeathPersistence.session().enchantmentLimits().entrySet()) {
            limitList.addRow(entry.getKey(), entry.getValue());
        }
        addRenderableWidget(limitList);

        addRowButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)),
                        btn -> limitList.addRow("", 10))
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
        colEnchantW = cols.primary();
        colLevelW = cols.secondary();
        colRemoveW = cols.remove();
    }

    void removeRow(LimitList.LimitEntry entry) {
        limitList.removeRow(entry);
    }

    private void save() {
        AltarOfDeathPersistence.session().setEnchantmentLimits(limitList.buildLimits());
        AltarOfDeathPersistence.save(minecraft);
    }

    int getTableLeft() {
        return tableLeft;
    }

    int getTableWidth() {
        return tableWidth;
    }

    int getColEnchantW() {
        return colEnchantW;
    }

    int getColLevelW() {
        return colLevelW;
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
        graphics.drawCenteredString(font, "Enchantment", tableLeft + colEnchantW / 2, HEADER_Y + 6, 0xFFFFFF);
        graphics.drawCenteredString(font, "Max Level", tableLeft + colEnchantW + colLevelW / 2, HEADER_Y + 6, 0xFFFFFF);

        addRowButton.setX(ConfigTableColumns.centeredX(tableLeft + colEnchantW + colLevelW, colRemoveW, 16));
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    static class LimitList extends ContainerObjectSelectionList<LimitList.LimitEntry> {

        private final AltarEnchantmentLimitListScreen screen;

        LimitList(AltarEnchantmentLimitListScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            this.setRenderBackground(true);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(screen.getTableLeft());
        }

        void addRow(String enchantId, int maxLevel) {
            addEntry(new LimitEntry(this, enchantId, maxLevel));
        }

        void removeRow(LimitEntry entry) {
            removeEntry(entry);
        }

        LinkedHashMap<String, Integer> buildLimits() {
            LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
            for (LimitEntry entry : children()) {
                String id = entry.getEnchantId().trim();
                if (id.isEmpty()) {
                    continue;
                }
                map.put(id, entry.getMaxLevel());
            }
            return map;
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
        static class LimitEntry extends ContainerObjectSelectionList.Entry<LimitEntry> {

            private final LimitList list;
            private final EditBox enchantField;
            private final EditBox levelField;
            private final Button removeButton;

            LimitEntry(LimitList list, String enchantId, int maxLevel) {
                this.list = list;
                AltarEnchantmentLimitListScreen screen = list.screen;

                enchantField = new EditBox(screen.font, 0, 0, screen.getColEnchantW() - 4, 18, Component.empty());
                enchantField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                enchantField.setValue(enchantId == null ? "" : enchantId);

                levelField = new EditBox(screen.font, 0, 0, screen.getColLevelW() - 4, 18, Component.empty());
                levelField.setMaxLength(16);
                levelField.setFilter(text -> text.isEmpty() || text.matches("\\d*"));
                levelField.setValue(String.valueOf(Math.max(1, maxLevel)));

                removeButton = Button.builder(Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.removeRow(this))
                        .bounds(0, 0, 16, 16)
                        .build();
            }

            String getEnchantId() {
                return enchantField.getValue();
            }

            int getMaxLevel() {
                try {
                    return Math.max(1, Integer.parseInt(levelField.getValue().trim()));
                } catch (Exception e) {
                    return 1;
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
                AltarEnchantmentLimitListScreen screen = list.screen;
                enchantField.setX(screen.getTableLeft() + 2);
                enchantField.setY(top + 1);
                enchantField.setWidth(screen.getColEnchantW() - 4);
                enchantField.render(graphics, mouseX, mouseY, partialTick);

                levelField.setX(screen.getTableLeft() + screen.getColEnchantW() + 2);
                levelField.setY(top + 1);
                levelField.setWidth(screen.getColLevelW() - 4);
                levelField.render(graphics, mouseX, mouseY, partialTick);

                removeButton.setX(ConfigTableColumns.centeredX(
                        screen.getTableLeft() + screen.getColEnchantW() + screen.getColLevelW(),
                        screen.getColRemoveW(),
                        16
                ));
                removeButton.setY(top + 2);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(enchantField, levelField, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(enchantField, levelField, removeButton);
            }
        }
    }
}
