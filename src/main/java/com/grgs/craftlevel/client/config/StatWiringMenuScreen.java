package com.grgs.craftlevel.client.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Menu de wiring: Attribute, Hurt e Heal. */
@OnlyIn(Dist.CLIENT)
public class StatWiringMenuScreen extends Screen {

    private final Screen parent;
    private final StatWiringEditor editor;

    protected StatWiringMenuScreen(Screen parent, String title, StatWiringEditor editor) {
        super(Component.literal("CraftLevel - " + title));
        this.parent = parent;
        this.editor = editor;
    }

    static StatWiringMenuScreen forFile(Screen parent, String fileName, net.minecraft.client.Minecraft minecraft) {
        return new StatWiringMenuScreen(
                parent,
                ConfigScreenLabels.fileButtonLabel(fileName),
                StatWiringEditor.forFile(fileName, minecraft)
        );
    }

    static StatWiringMenuScreen forMobOverride(Screen parent, String mobId, net.minecraft.client.Minecraft minecraft) {
        return new StatWiringMenuScreen(
                parent,
                mobId,
                StatWiringEditor.forMobOverride(mobId, minecraft)
        );
    }

    @Override
    protected void init() {
        Button attributeButton = sectionButton("Attribute", StatWiringEditorState.Section.ATTRIBUTE, 0);
        addRenderableWidget(attributeButton);

        Button hurtButton = sectionButton("Hurt", StatWiringEditorState.Section.HURT, 1);
        addRenderableWidget(hurtButton);

        Button healButton = sectionButton("Heal", StatWiringEditorState.Section.HEAL, 2);
        addRenderableWidget(healButton);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::save));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private Button sectionButton(String label, StatWiringEditorState.Section section, int row) {
        Button button = Button.builder(Component.literal(label), btn ->
                        minecraft.setScreen(new StatWiringSectionScreen(this, editor, section)))
                .width(ConfigScreenLayout.BUTTON_WIDTH)
                .build();
        ConfigScreenLayout.positionRow(this, row, button);
        return button;
    }

    private void save() {
        editor.save();
        if (parent instanceof MobOverridesListScreen listScreen) {
            minecraft.setScreen(new MobOverridesListScreen(listScreen.getMenuParent()));
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
