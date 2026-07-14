package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.client.ProfessionTextureLoader;
import com.grgs.craftlevel.villager.ProfessionIds;
import com.grgs.craftlevel.villager.VillagerProfessionDef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Editor de uma profissão custom. */
@OnlyIn(Dist.CLIENT)
public class VillagerProfessionDetailScreen extends Screen {

    private final Screen parent;
    private final String professionId;
    private final String professionPath;
    private EditBox textureField;
    private EditBox stationField;

    public VillagerProfessionDetailScreen(Screen parent, String professionId) {
        super(Component.literal("Profession: " + professionId));
        this.parent = parent;
        this.professionId = professionId;
        this.professionPath = ProfessionIds.pathFromFullId(professionId);
    }

    @Override
    protected void init() {
        VillagerProfessionDef def = VillagerPersistence.session().professions().getOrDefault(
                professionId, new VillagerProfessionDef(professionPath, "minecraft:smithing_table"));

        textureField = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.literal(""));
        textureField.setValue(professionPath + ".png");
        textureField.setMaxLength(128);
        textureField.setEditable(false);
        textureField.setTextColor(0xA0A0A0);
        ConfigScreenLayout.positionField(this, 0, textureField);
        addRenderableWidget(textureField);

        stationField = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.literal(""));
        stationField.setValue(def.station());
        stationField.setMaxLength(128);
        ConfigScreenLayout.positionField(this, 1, stationField);
        addRenderableWidget(stationField);

        Button openFolder = Button.builder(Component.literal("Open Texture Folder"), btn ->
                        ProfessionTextureLoader.ensureFolder())
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, 3, openFolder);
        addRenderableWidget(openFolder);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomExit(this, this::onClose));
    }

    private void save() {
        VillagerPersistence.session().setProfession(professionId, new VillagerProfessionDef(
                professionPath,
                stationField.getValue().trim()
        ));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        ConfigScreenLayout.renderLabel(this, graphics, 0, "Texture file (auto)");
        ConfigScreenLayout.renderLabel(this, graphics, 1, "Station block");
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
