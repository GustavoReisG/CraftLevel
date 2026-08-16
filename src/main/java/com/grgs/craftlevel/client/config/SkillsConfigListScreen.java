package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.skill.SkillCatalog;
import com.grgs.craftlevel.skill.SkillDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Lista de skills para editar custo / min lvl. */
@OnlyIn(Dist.CLIENT)
public class SkillsConfigListScreen extends Screen {

    private final Screen parent;

    public SkillsConfigListScreen(Screen parent) {
        super(Component.literal("CraftLevel - Skills"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        SkillsPersistence.beginEditing(minecraft);
        int row = 0;
        for (SkillDefinition def : SkillCatalog.all()) {
            Button button = Button.builder(Component.translatable(def.titleKey()), btn ->
                            minecraft.setScreen(new SkillConfigDetailScreen(this, def)))
                    .width(ConfigScreenLayout.BUTTON_WIDTH)
                    .build();
            ConfigScreenLayout.positionRow(this, row++, button);
            addRenderableWidget(button);
        }
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
