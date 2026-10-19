package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.arena.ArenaSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Equipamento e efeito de um mob da horda. Campo inválido é ignorado na hora de spawnar. */
@OnlyIn(Dist.CLIENT)
public class ArenaMobGearScreen extends Screen {

    private final Screen parent;
    private final ArenaSettings.Mob mob;
    private EditBox helmet;
    private EditBox chestplate;
    private EditBox leggings;
    private EditBox boots;
    private EditBox hand;
    private EditBox effect;

    public ArenaMobGearScreen(Screen parent, ArenaSettings.Mob mob) {
        super(Component.literal("Horde Mob"));
        this.parent = parent;
        this.mob = mob;
    }

    @Override
    protected void init() {
        ArenaConfigPersistence.ensureSession(minecraft);
        helmet = field(0, mob.helmet);
        chestplate = field(1, mob.chestplate);
        leggings = field(2, mob.leggings);
        boots = field(3, mob.boots);
        hand = field(4, mob.hand);
        effect = field(5, mob.effect);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private EditBox field(int row, String value) {
        EditBox box = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.empty());
        box.setMaxLength(ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
        box.setValue(value == null ? "" : value);
        ConfigScreenLayout.positionField(this, row, box);
        addRenderableWidget(box);
        return box;
    }

    private void flush() {
        mob.helmet = helmet.getValue().trim();
        mob.chestplate = chestplate.getValue().trim();
        mob.leggings = leggings.getValue().trim();
        mob.boots = boots.getValue().trim();
        mob.hand = hand.getValue().trim();
        mob.effect = effect.getValue().trim();
    }

    private void save() {
        flush();
        ArenaConfigPersistence.save(minecraft);
    }

    @Override
    public void onClose() {
        flush();
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        ConfigScreenLayout.renderLabel(this, graphics, 0, "Helmet");
        ConfigScreenLayout.renderLabel(this, graphics, 1, "Chestplate");
        ConfigScreenLayout.renderLabel(this, graphics, 2, "Leggings");
        ConfigScreenLayout.renderLabel(this, graphics, 3, "Boots");
        ConfigScreenLayout.renderLabel(this, graphics, 4, "Hand Item");
        ConfigScreenLayout.renderLabel(this, graphics, 5, "Effect");
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
