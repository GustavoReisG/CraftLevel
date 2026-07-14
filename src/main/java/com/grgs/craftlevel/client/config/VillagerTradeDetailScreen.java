package com.grgs.craftlevel.client.config;

import com.grgs.craftlevel.villager.VillagerTradeDef;
import com.grgs.craftlevel.villager.VillagerTradeSlotDef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Editor resumido de um trade — abre telas por slot. */
@OnlyIn(Dist.CLIENT)
public class VillagerTradeDetailScreen extends Screen {

    private static final int ROW_INPUT1 = 0;
    private static final int ROW_INPUT2 = 1;
    private static final int ROW_OUTPUT = 2;
    private static final int ROW_TRADES = 3;
    private static final int ICON_SIZE = 16;
    private static final int CONFIG_BTN_WIDTH = 52;
    private static final int ICON_GAP = 4;

    private final Screen parent;
    private final String professionId;
    private final int tradeIndex;

    private EditBox tradesField;

    public VillagerTradeDetailScreen(Screen parent, String professionId, int tradeIndex) {
        super(Component.literal("Trade #" + (tradeIndex + 1)));
        this.parent = parent;
        this.professionId = professionId;
        this.tradeIndex = tradeIndex;
    }

    @Override
    protected void init() {
        VillagerTradeDef trade = currentTrade();

        Button input1Button = slotButton(ROW_INPUT1, () ->
                minecraft.setScreen(new VillagerTradeSlotDetailScreen(this, professionId, tradeIndex, SlotKind.INPUT1)));
        Button input2Button = slotButton(ROW_INPUT2, () ->
                minecraft.setScreen(new VillagerTradeSlotDetailScreen(this, professionId, tradeIndex, SlotKind.INPUT2)));
        Button outputButton = slotButton(ROW_OUTPUT, () ->
                minecraft.setScreen(new VillagerTradeSlotDetailScreen(this, professionId, tradeIndex, SlotKind.OUTPUT)));

        tradesField = numericField(String.valueOf(trade.limit()));
        ConfigScreenLayout.positionField(this, ROW_TRADES, tradesField);

        addRenderableWidget(input1Button);
        addRenderableWidget(input2Button);
        addRenderableWidget(outputButton);
        addRenderableWidget(tradesField);

        addRenderableWidget(ConfigScreenLayout.bottomSave(this, this::saveAndClose));
        addRenderableWidget(ConfigScreenLayout.bottomBack(this, this::onClose));
    }

    private Button slotButton(int row, Runnable action) {
        int fieldX = fieldStartX();
        int y = ConfigScreenLayout.rowY(this, row);
        int btnX = fieldX + ICON_SIZE + ICON_GAP;
        return Button.builder(Component.literal("Config"), btn -> action.run())
                .bounds(btnX, y, CONFIG_BTN_WIDTH, 20)
                .build();
    }

    private int fieldStartX() {
        return ConfigScreenLayout.centerX(this) + ConfigScreenLayout.LABEL_WIDTH + ConfigScreenLayout.FIELD_GAP;
    }

    private EditBox numericField(String value) {
        EditBox field = new EditBox(font, 0, 0, ConfigScreenLayout.fieldWidth(), 20, Component.literal(""));
        field.setValue(value);
        field.setFilter(text -> text.isEmpty() || text.matches("-?\\d+"));
        return field;
    }

    VillagerTradeDef currentTrade() {
        return VillagerPersistence.session().tradesFor(professionId).get(tradeIndex);
    }

    void updateSlot(SlotKind kind, VillagerTradeSlotDef slot) {
        VillagerTradeDef trade = currentTrade();
        VillagerTradeDef updated = switch (kind) {
            case INPUT1 -> new VillagerTradeDef(trade.limit(), slot, trade.input2(), trade.output());
            case INPUT2 -> new VillagerTradeDef(trade.limit(), trade.input1(), slot, trade.output());
            case OUTPUT -> new VillagerTradeDef(trade.limit(), trade.input1(), trade.input2(), slot);
        };
        VillagerPersistence.session().setTrade(professionId, tradeIndex, updated);
    }

    private void saveAndClose() {
        VillagerTradeDef trade = currentTrade();
        VillagerPersistence.session().setTrade(professionId, tradeIndex, new VillagerTradeDef(
                parseInt(tradesField.getValue(), 0),
                trade.input1(),
                trade.input2(),
                trade.output()
        ));
        VillagerPersistence.save(minecraft);
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
        ConfigScreenLayout.renderLabel(this, graphics, ROW_INPUT1, "Input 1");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_INPUT2, "Input 2");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_OUTPUT, "Output");
        ConfigScreenLayout.renderLabel(this, graphics, ROW_TRADES, "Trades");

        VillagerTradeDef trade = currentTrade();
        renderSlotIcon(graphics, ROW_INPUT1, trade.input1());
        renderSlotIcon(graphics, ROW_INPUT2, trade.input2());
        renderSlotIcon(graphics, ROW_OUTPUT, trade.output());

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSlotIcon(GuiGraphics graphics, int row, VillagerTradeSlotDef slot) {
        ItemStack stack = TradeSlotIconHelper.preview(slot);
        if (stack.isEmpty()) {
            return;
        }
        int x = fieldStartX();
        int y = ConfigScreenLayout.rowY(this, row) + 2;
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
    }

    enum SlotKind {
        INPUT1,
        INPUT2,
        OUTPUT
    }
}
