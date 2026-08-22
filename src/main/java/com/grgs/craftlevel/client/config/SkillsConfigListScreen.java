package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.skill.SkillCatalog;
import com.grgs.craftlevel.skill.SkillDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/** Lista de skills para editar custo / min lvl. */
@OnlyIn(Dist.CLIENT)
public class SkillsConfigListScreen extends Screen {

    private final Screen parent;
    private final List<Button> skillButtons = new ArrayList<>();
    private int scrollY;
    private int maxScroll;

    public SkillsConfigListScreen(Screen parent) {
        super(Component.literal("CraftLevel - Skills"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        SkillsPersistence.beginEditing(minecraft);
        skillButtons.clear();
        int row = 0;
        for (SkillDefinition def : SkillCatalog.all()) {
            Button button = Button.builder(Component.translatable(def.titleKey()), btn ->
                            minecraft.setScreen(new SkillConfigDetailScreen(this, def)))
                    .width(ConfigScreenLayout.BUTTON_WIDTH)
                    .build();
            ConfigScreenLayout.positionRow(this, row++, button);
            addRenderableWidget(button);
            skillButtons.add(button);
        }

        int contentHeight = skillButtons.size() * ConfigScreenLayout.ROW_HEIGHT;
        int viewHeight = Math.max(1, ConfigScreenLayout.contentBottom(this) - ConfigScreenLayout.START_Y);
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollY = Mth.clamp(scrollY, 0, maxScroll);
        applyScroll();

        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private void applyScroll() {
        int viewTop = ConfigScreenLayout.START_Y;
        int viewBottom = ConfigScreenLayout.contentBottom(this);
        for (int i = 0; i < skillButtons.size(); i++) {
            Button button = skillButtons.get(i);
            int y = ConfigScreenLayout.START_Y + i * ConfigScreenLayout.ROW_HEIGHT - scrollY;
            button.setY(y);
            button.visible = y + 20 > viewTop && y < viewBottom;
            button.active = button.visible;
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

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        if (maxScroll > 0) {
            int trackX = ConfigScreenLayout.centerX(this) + ConfigScreenLayout.BUTTON_WIDTH + 6;
            int trackTop = ConfigScreenLayout.START_Y;
            int trackBottom = ConfigScreenLayout.contentBottom(this);
            int trackHeight = Math.max(1, trackBottom - trackTop);
            graphics.fill(trackX, trackTop, trackX + 3, trackBottom, 0x66000000);
            float ratio = (float) scrollY / (float) maxScroll;
            int thumbHeight = Math.max(12, (int) (trackHeight * (trackHeight / (float) (trackHeight + maxScroll))));
            int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * ratio);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xAAFFFFFF);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
