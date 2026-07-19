package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.villager.VillagerTradeSlotDef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Editor de um slot (input/output) de trade. */
@OnlyIn(Dist.CLIENT)
public class VillagerTradeSlotDetailScreen extends Screen {

    private static final int ROW_ITEM = 0;
    private static final int ROW_MIN = 1;
    private static final int ROW_MAX = 2;
    private static final int ROW_WHITELIST = 3;
    private static final int ROW_BLACKLIST = 4;
    private static final int ROW_NBT = 5;

    private final VillagerTradeDetailScreen parent;
    private final String professionId;
    private final int tradeIndex;
    private final VillagerTradeDetailScreen.SlotKind slotKind;

    private EditBox itemField;
    private EditBox minField;
    private EditBox maxField;
    private EditBox whitelistField;
    private EditBox blacklistField;
    private EditBox nbtField;

    public VillagerTradeSlotDetailScreen(
            VillagerTradeDetailScreen parent,
            String professionId,
            int tradeIndex,
            VillagerTradeDetailScreen.SlotKind slotKind
    ) {
        super(Component.literal(slotTitle(slotKind)));
        this.parent = parent;
        this.professionId = professionId;
        this.tradeIndex = tradeIndex;
        this.slotKind = slotKind;
    }

    private static String slotTitle(VillagerTradeDetailScreen.SlotKind kind) {
        return switch (kind) {
            case INPUT1 -> "Input 1";
            case INPUT2 -> "Input 2";
            case OUTPUT -> "Output";
        };
    }

    @Override
    protected void init() {
        VillagerTradeSlotDef slot = currentSlot();

        itemField = textField(slot.item());
        minField = numericField(String.valueOf(slot.min()));
        maxField = numericField(String.valueOf(slot.max()));
        whitelistField = textField(VillagerListText.join(slot.whitelist()));
        blacklistField = textField(VillagerListText.join(slot.blacklist()));
        nbtField = textField(slot.nbt() != null ? slot.nbt() : "");

        ConfigScreenLayout.positionField(this, ROW_ITEM, itemField);
        ConfigScreenLayout.positionField(this, ROW_MIN, minField);
        ConfigScreenLayout.positionField(this, ROW_MAX, maxField);
        ConfigScreenLayout.positionField(this, ROW_WHITELIST, whitelistField);
        ConfigScreenLayout.positionField(this, ROW_BLACKLIST, blacklistField);
        ConfigScreenLayout.positionField(this, ROW_NBT, nbtField);

        addRenderableWidget(itemField);
        addRenderableWidget(minField);
        addRenderableWidget(maxField);
        addRenderableWidget(whitelistField);
        addRenderableWidget(blacklistField);
        addRenderableWidget(nbtField);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::saveAndClose));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private VillagerTradeSlotDef currentSlot() {
        return switch (slotKind) {
            case INPUT1 -> parent.currentTrade().input1();
            case INPUT2 -> parent.currentTrade().input2();
            case OUTPUT -> parent.currentTrade().output();
        };
    }

    private EditBox textField(String value) {
        return textField(value, ConfigScreenLayout.TEXT_FIELD_MAX_LENGTH);
    }

    private EditBox textField(String value, int maxLength) {
        EditBox field = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.literal(""));
        // setMaxLength before setValue — EditBox defaults to 32 and truncates on setValue.
        field.setMaxLength(maxLength);
        field.setValue(value != null ? value : "");
        return field;
    }

    private EditBox numericField(String value) {
        EditBox field = textField(value, 32);
        field.setFilter(text -> text.isEmpty() || text.matches("-?\\d+"));
        return field;
    }

    private void saveAndClose() {
        parent.updateSlot(slotKind, new VillagerTradeSlotDef(
                itemField.getValue().trim(),
                parseInt(minField.getValue(), 0),
                parseInt(maxField.getValue(), 0),
                VillagerListText.split(whitelistField.getValue()),
                VillagerListText.split(blacklistField.getValue()),
                nbtField.getValue().trim()
        ));
        onClose();
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
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        ConfigScreenLayout.renderLabel(this, graphics, ROW_ITEM, "Item / Tag");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_MIN, "Min");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_MAX, "Max");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_WHITELIST, "Whitelist");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_BLACKLIST, "Blacklist");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_NBT, "NBT fallback");
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
