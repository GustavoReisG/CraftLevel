package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.skill.SkillDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/** Tabela: Skill Level | Skill Point Cost | Min Player Level. */
@OnlyIn(Dist.CLIENT)
public class SkillConfigDetailScreen extends Screen {

    private final Screen parent;
    private final SkillDefinition skill;
    private final List<RowWidgets> rows = new ArrayList<>();
    private int tableLeft;
    private int colLevelW;
    private int colCostW;
    private int colMinW;
    private int scrollY;
    private int maxScroll;

    public SkillConfigDetailScreen(Screen parent, SkillDefinition skill) {
        super(Component.translatable(skill.titleKey()));
        this.parent = parent;
        this.skill = skill;
    }

    @Override
    protected void init() {
        SkillsPersistence.beginEditing(minecraft);
        rows.clear();
        layoutTable();

        int startY = 48;
        List<SkillsEditorState.LevelRow> data = SkillsPersistence.session().rows(skill.id());
        for (int i = 0; i < data.size(); i++) {
            SkillsEditorState.LevelRow row = data.get(i);
            EditBox cost = numericField(String.valueOf(row.skillPointCost()));
            EditBox minLevel = numericField(String.valueOf(row.minPlayerLevel()));
            addRenderableWidget(cost);
            addRenderableWidget(minLevel);
            rows.add(new RowWidgets(i, row.level(), cost, minLevel));
        }

        int contentHeight = rows.size() * 24;
        int viewHeight = Math.max(1, ConfigScreenLayout.contentBottom(this) - startY);
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollY = Mth.clamp(scrollY, 0, maxScroll);
        applyScroll();

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void layoutTable() {
        int tableWidth = Math.min(420, width - 40);
        tableLeft = (width - tableWidth) / 2;
        colLevelW = tableWidth / 4;
        colCostW = tableWidth / 3;
        colMinW = tableWidth - colLevelW - colCostW;
    }

    private void applyScroll() {
        int startY = 48;
        for (int i = 0; i < rows.size(); i++) {
            RowWidgets row = rows.get(i);
            int y = startY + i * 24 - scrollY;
            boolean visible = y + 20 > startY && y < ConfigScreenLayout.contentBottom(this);
            row.cost.setY(y);
            row.minLevel.setY(y);
            row.cost.setX(tableLeft + colLevelW + 4);
            row.minLevel.setX(tableLeft + colLevelW + colCostW + 4);
            row.cost.setWidth(colCostW - 8);
            row.minLevel.setWidth(colMinW - 8);
            row.cost.visible = visible;
            row.minLevel.visible = visible;
            row.cost.active = visible;
            row.minLevel.active = visible;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            scrollY = Mth.clamp(scrollY - (int) Math.round(delta * 12), 0, maxScroll);
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void save() {
        for (RowWidgets row : rows) {
            SkillsEditorState.LevelRow current = SkillsPersistence.session().rows(skill.id()).get(row.index);
            SkillsPersistence.session().setRow(
                    skill.id(),
                    row.index,
                    current.withCost(parseInt(row.cost.getValue(), current.skillPointCost()))
                            .withMinLevel(parseInt(row.minLevel.getValue(), current.minPlayerLevel()))
            );
        }
        SkillsPersistence.save(minecraft);
    }

    private EditBox numericField(String value) {
        EditBox field = new EditBox(font, 0, 0, 40, 20, Component.empty());
        field.setMaxLength(8);
        field.setFilter(text -> text.isEmpty() || text.matches("\\d*"));
        field.setValue(value);
        return field;
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return fallback;
        }
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
        graphics.fill(tableLeft, headerY, tableLeft + colLevelW + colCostW + colMinW, headerY + 18, 0x88000000);
        graphics.drawCenteredString(font, "Skill Level", tableLeft + colLevelW / 2, headerY + 5, 0xFFFFFF);
        graphics.drawCenteredString(font, "Skill Point Cost", tableLeft + colLevelW + colCostW / 2, headerY + 5, 0xFFFFFF);
        graphics.drawCenteredString(
                font,
                "Min Player Level",
                tableLeft + colLevelW + colCostW + colMinW / 2,
                headerY + 5,
                0xFFFFFF
        );

        int startY = 48;
        for (int i = 0; i < rows.size(); i++) {
            int y = startY + i * 24 - scrollY;
            if (y + 20 > startY && y < ConfigScreenLayout.contentBottom(this)) {
                graphics.drawCenteredString(
                        font,
                        String.valueOf(rows.get(i).level),
                        tableLeft + colLevelW / 2,
                        y + 6,
                        0xFFFFFF
                );
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private record RowWidgets(int index, int level, EditBox cost, EditBox minLevel) {}
}
