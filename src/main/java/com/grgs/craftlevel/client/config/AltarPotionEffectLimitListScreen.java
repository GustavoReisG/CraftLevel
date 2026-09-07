package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.config.AltarPotionParser;
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

/** Lista de limites por efeito: Potion | Time | Level. */
@OnlyIn(Dist.CLIENT)
public class AltarPotionEffectLimitListScreen extends Screen {

    private static final int HEADER_Y = 28;
    private static final int HEADER_HEIGHT = 22;
    private static final int LIST_TOP = HEADER_Y + HEADER_HEIGHT + 4;

    private final Screen parent;

    private LimitList limitList;
    private Button addRowButton;

    private int tableLeft;
    private int tableWidth;
    private int colPotionW;
    private int colTimeW;
    private int colLevelW;
    private int colRemoveW;

    public AltarPotionEffectLimitListScreen(Screen parent) {
        super(Component.literal("CraftLevel - Potions List"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AltarOfDeathPersistence.ensureSession(minecraft);
        layoutColumns();

        int listBottom = ConfigScreenLayout.contentBottom(this);
        limitList = new LimitList(this, tableWidth, listBottom - LIST_TOP, LIST_TOP, listBottom);
        int defaultTime = AltarOfDeathPersistence.session().maxUpgradesTime();
        int defaultLevel = AltarOfDeathPersistence.session().maxUpgradesStrength();
        for (Map.Entry<String, AltarPotionParser.EffectLimit> entry
                : AltarOfDeathPersistence.session().effectLimits().entrySet()) {
            AltarPotionParser.EffectLimit limit = entry.getValue();
            limitList.addRow(entry.getKey(), limit.time(), limit.level());
        }
        addRenderableWidget(limitList);

        addRowButton = Button.builder(Component.literal("+").withStyle(style -> style.withColor(0x55FF55)),
                        btn -> limitList.addRow("", defaultTime, defaultLevel))
                .bounds(0, 0, 16, 16)
                .build();
        addRenderableWidget(addRowButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutColumns() {
        tableWidth = Math.min(480, width - 40);
        tableLeft = (width - tableWidth) / 2;
        ConfigTableColumns.FourColumns cols = ConfigTableColumns.layoutFour(tableWidth);
        colPotionW = cols.primary();
        colTimeW = cols.secondary();
        colLevelW = cols.tertiary();
        colRemoveW = cols.remove();
    }

    void removeRow(LimitList.LimitEntry entry) {
        limitList.removeRow(entry);
    }

    private void save() {
        AltarOfDeathPersistence.session().setEffectLimits(limitList.buildLimits());
        AltarOfDeathPersistence.save(minecraft);
    }

    int getTableLeft() {
        return tableLeft;
    }

    int getTableWidth() {
        return tableWidth;
    }

    int getColPotionW() {
        return colPotionW;
    }

    int getColTimeW() {
        return colTimeW;
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
        int x = tableLeft;
        graphics.drawCenteredString(font, "Potion", x + colPotionW / 2, HEADER_Y + 6, 0xFFFFFF);
        x += colPotionW;
        graphics.drawCenteredString(font, "Time", x + colTimeW / 2, HEADER_Y + 6, 0xFFFFFF);
        x += colTimeW;
        graphics.drawCenteredString(font, "Level", x + colLevelW / 2, HEADER_Y + 6, 0xFFFFFF);

        addRowButton.setX(ConfigTableColumns.centeredX(
                tableLeft + colPotionW + colTimeW + colLevelW, colRemoveW, 16));
        addRowButton.setY(HEADER_Y + 3);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    static class LimitList extends ContainerObjectSelectionList<LimitList.LimitEntry> {

        private final AltarPotionEffectLimitListScreen screen;

        LimitList(AltarPotionEffectLimitListScreen screen, int width, int height, int top, int bottom) {
            super(screen.minecraft, width, height, top, bottom, 22);
            this.screen = screen;
            this.setRenderBackground(true);
            this.setRenderTopAndBottom(false);
            this.setLeftPos(screen.getTableLeft());
        }

        void addRow(String effectId, int time, int level) {
            addEntry(new LimitEntry(this, effectId, time, level));
        }

        void removeRow(LimitEntry entry) {
            removeEntry(entry);
        }

        LinkedHashMap<String, AltarPotionParser.EffectLimit> buildLimits() {
            LinkedHashMap<String, AltarPotionParser.EffectLimit> map = new LinkedHashMap<>();
            for (LimitEntry entry : children()) {
                String id = entry.getEffectId().trim();
                if (id.isEmpty()) {
                    continue;
                }
                map.put(id, new AltarPotionParser.EffectLimit(entry.getTime(), entry.getLevel()));
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
            private final EditBox potionField;
            private final EditBox timeField;
            private final EditBox levelField;
            private final Button removeButton;

            LimitEntry(LimitList list, String effectId, int time, int level) {
                this.list = list;
                AltarPotionEffectLimitListScreen screen = list.screen;

                potionField = new EditBox(screen.font, 0, 0, screen.getColPotionW() - 4, 18, Component.empty());
                potionField.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
                potionField.setValue(effectId == null ? "" : effectId);

                timeField = new EditBox(screen.font, 0, 0, screen.getColTimeW() - 4, 18, Component.empty());
                timeField.setMaxLength(16);
                timeField.setFilter(text -> text.isEmpty() || text.matches("\\d*"));
                timeField.setValue(String.valueOf(Math.max(1, time)));

                levelField = new EditBox(screen.font, 0, 0, screen.getColLevelW() - 4, 18, Component.empty());
                levelField.setMaxLength(16);
                levelField.setFilter(text -> text.isEmpty() || text.matches("\\d*"));
                levelField.setValue(String.valueOf(Math.max(1, level)));

                removeButton = Button.builder(Component.literal("X").withStyle(style -> style.withColor(0xFF5555)),
                                btn -> screen.removeRow(this))
                        .bounds(0, 0, 16, 16)
                        .build();
            }

            String getEffectId() {
                return potionField.getValue();
            }

            int getTime() {
                return parsePositive(timeField.getValue(), 1);
            }

            int getLevel() {
                return parsePositive(levelField.getValue(), 1);
            }

            private static int parsePositive(String text, int fallback) {
                try {
                    return Math.max(1, Integer.parseInt(text.trim()));
                } catch (Exception e) {
                    return fallback;
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
                AltarPotionEffectLimitListScreen screen = list.screen;
                int x = screen.getTableLeft();

                potionField.setX(x + 2);
                potionField.setY(top + 1);
                potionField.setWidth(screen.getColPotionW() - 4);
                potionField.render(graphics, mouseX, mouseY, partialTick);
                x += screen.getColPotionW();

                timeField.setX(x + 2);
                timeField.setY(top + 1);
                timeField.setWidth(screen.getColTimeW() - 4);
                timeField.render(graphics, mouseX, mouseY, partialTick);
                x += screen.getColTimeW();

                levelField.setX(x + 2);
                levelField.setY(top + 1);
                levelField.setWidth(screen.getColLevelW() - 4);
                levelField.render(graphics, mouseX, mouseY, partialTick);

                removeButton.setX(ConfigTableColumns.centeredX(
                        screen.getTableLeft() + screen.getColPotionW() + screen.getColTimeW() + screen.getColLevelW(),
                        screen.getColRemoveW(),
                        16
                ));
                removeButton.setY(top + 2);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(potionField, timeField, levelField, removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(potionField, timeField, levelField, removeButton);
            }
        }
    }
}
