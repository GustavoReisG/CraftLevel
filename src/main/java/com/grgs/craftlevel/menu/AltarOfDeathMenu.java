package com.grgs.craftlevel.menu;

import com.grgs.craftlevel.altar.AltarPotionService;
import com.grgs.craftlevel.block.ModBlocks;
import com.grgs.craftlevel.glory.GloryKillsService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;

/** Menu do Altar da Morte — 176×168 (baú simples vanilla), 4 abas, inventário do jogador. */
public class AltarOfDeathMenu extends AbstractContainerMenu {

    /** Baú simples (3 fileiras): 114 + 3×18. */
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 168;
    public static final int SLOT_SIZE = 18;
    public static final int SLOT_ORIGIN_X = 8;
    public static final int SLOT_STEP = 18;

    public static final int TAB_BUTTON_SIZE = 42;
    public static final int TAB_BUTTON_GAP = 6;
    public static final int TAB_ROW_HEIGHT = TAB_BUTTON_SIZE + 8;
    public static final int TITLE_GAP = 6;
    public static final int TITLE_HEIGHT = 16;
    public static final int CHROME_HEIGHT = TAB_ROW_HEIGHT + TITLE_GAP + TITLE_HEIGHT;

    public static final int INV_LABEL_Y = 74;
    public static final int INV_ROW1_Y = 85;
    public static final int INV_ROW2_Y = 103;
    public static final int INV_ROW3_Y = 121;
    public static final int HOTBAR_Y = 143;

    public static final int CONVERSION_SLOT_X = AltarChestGrid.columnX(2) + 7;
    public static final int CONVERSION_SLOT_Y = AltarChestGrid.rowY('B') + 2;
    public static final int CONVERT_BTN_X = AltarChestGrid.columnX(5) + 6;
    public static final int CONVERT_BTN_Y = AltarChestGrid.rowY('B') + 1;
    public static final int CONVERSION_COST_X = AltarChestGrid.columnX(7) - 3;
    public static final int CONVERSION_COST_Y = AltarChestGrid.rowY('B') + 13;
    public static final int CONVERSION_COST_W = AltarChestGrid.spanWidth(3);
    public static final int CONVERSION_COST_H = SLOT_SIZE;

    public static final int POTION_LAYOUT_OFFSET_X = -9;

    public static final int POTION_SLOT_X = AltarChestGrid.slotX(2) + POTION_LAYOUT_OFFSET_X;
    public static final int POTION_SLOT_Y = AltarChestGrid.slotY('B');
    public static final int POTION_OUTPUT_SLOT_X = AltarChestGrid.slotX(8) + POTION_LAYOUT_OFFSET_X;
    public static final int POTION_OUTPUT_SLOT_Y = AltarChestGrid.slotY('B') + 18;
    public static final int POTION_BTN_X = AltarChestGrid.buttonX(5) + POTION_LAYOUT_OFFSET_X;
    public static final int POTION_TIME_BTN_Y = AltarChestGrid.buttonY('B') - 21;
    public static final int POTION_EFFECT_BTN_Y = AltarChestGrid.buttonY('B') + 3;
    /** Glory cost nas colunas 4–6, linha C. */
    public static final int POTION_COST_X = AltarChestGrid.columnX(4) - 3;
    public static final int POTION_COST_Y = AltarChestGrid.rowY('C') + 10;
    public static final int POTION_COST_W = AltarChestGrid.spanWidth(3);
    public static final int POTION_COST_H = SLOT_SIZE;

    /** Slot B2 e botão E2 deslocados 9px à esquerda. */
    public static final int IMPROVE_LAYOUT_OFFSET_X = -9;

    public static final int IMPROVE_SLOT_X = AltarChestGrid.slotX(2) + IMPROVE_LAYOUT_OFFSET_X;
    public static final int IMPROVE_SLOT_Y = AltarChestGrid.slotY('B');
    /** Botão de improve: deslocado à esquerda sem cortar na moldura. */
    public static final int IMPROVE_BTN_X = AltarChestGrid.buttonX(5) + IMPROVE_LAYOUT_OFFSET_X - 9;
    public static final int IMPROVE_BTN_Y = AltarChestGrid.buttonY('B') + 9;

    /** Lista 70×70, 5 linhas de 14px; −2px X / +2px Y. */
    public static final int IMPROVE_LIST_X = AltarChestGrid.columnX(6) - 2;
    public static final int IMPROVE_LIST_Y = AltarChestGrid.rowY('A') - 9 + 2;
    public static final int IMPROVE_LIST_W = 70;
    public static final int IMPROVE_LIST_H = 70;
    public static final int IMPROVE_LIST_ROW_H = 14;

    /** Glory cost nas colunas 3–5, linha A. */
    public static final int IMPROVE_COST_X = AltarChestGrid.columnX(3) - 2;
    public static final int IMPROVE_COST_Y = AltarChestGrid.rowY('A') - 8;
    public static final int IMPROVE_COST_W = AltarChestGrid.spanWidth(3) - 2;
    public static final int IMPROVE_COST_H = SLOT_SIZE;

    /**
     * Trade — malha do baú: ofertas cols 5–9 / fileiras A–B;
     * botão col 4 / fileira C; glory cols 7–9 / fileira C.
     */
    public static final int TRADE_OFFER_COLS = 5;
    public static final int TRADE_OFFER_ROWS = 2;
    public static final int TRADE_OFFER_COUNT = TRADE_OFFER_COLS * TRADE_OFFER_ROWS;
    /** Top-left do 1º slot de oferta em {@code altar_trade_gui.png}. */
    public static final int TRADE_OFFER_SLOT_X0 = 80;
    public static final int TRADE_OFFER_SLOT_Y0 = 9;

    /** Botão: slot 4 da 3ª fileira (+9px à direita). */
    public static final int TRADE_BTN_X = AltarChestGrid.buttonX(4) + 9;
    public static final int TRADE_BTN_Y = AltarChestGrid.buttonY('C');

    public static final int TRADE_COST_X = AltarChestGrid.columnX(7) - 2;
    public static final int TRADE_COST_Y = AltarChestGrid.slotY('C');
    public static final int TRADE_COST_W = AltarChestGrid.spanWidth(3) - 2;
    public static final int TRADE_COST_H = SLOT_SIZE;

    public static int tradeOfferSlotX(int index) {
        return TRADE_OFFER_SLOT_X0 + (index % TRADE_OFFER_COLS) * SLOT_STEP;
    }

    public static int tradeOfferSlotY(int index) {
        return TRADE_OFFER_SLOT_Y0 + (index / TRADE_OFFER_COLS) * SLOT_STEP;
    }

    private static final int ALTAR_SLOT_COUNT = 4;
    private static final int CONVERSION_SLOT = 0;
    private static final int POTION_SLOT = 1;
    private static final int IMPROVEMENT_SLOT = 2;
    private static final int POTION_OUTPUT_SLOT = 3;
    private static final int PLAYER_INV_START = 4;

    private final BlockPos altarPos;
    private final ContainerLevelAccess access;
    private final Container altarItems = new SimpleContainer(ALTAR_SLOT_COUNT);
    private final ContainerData data = new SimpleContainerData(1);

    public AltarOfDeathMenu(int containerId, Inventory playerInventory, BlockPos altarPos) {
        super(ModMenus.ALTAR_OF_DEATH.get(), containerId);
        this.altarPos = altarPos;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), altarPos);

        addDataSlots(data);
        data.set(0, AltarTab.CONVERSION.id());

        addSlot(new AltarTabSlot(altarItems, CONVERSION_SLOT, CONVERSION_SLOT_X, CONVERSION_SLOT_Y, AltarTab.CONVERSION));
        addSlot(new AltarPotionInputSlot(altarItems, POTION_SLOT, POTION_SLOT_X, POTION_SLOT_Y));
        addSlot(new AltarImprovementInputSlot(altarItems, IMPROVEMENT_SLOT, IMPROVE_SLOT_X, IMPROVE_SLOT_Y));
        addSlot(new AltarTabOutputSlot(altarItems, POTION_OUTPUT_SLOT, POTION_OUTPUT_SLOT_X, POTION_OUTPUT_SLOT_Y, AltarTab.POTION));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        SLOT_ORIGIN_X + col * SLOT_STEP,
                        INV_ROW1_Y + row * SLOT_STEP
                ));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, SLOT_ORIGIN_X + col * SLOT_STEP, HOTBAR_Y));
        }

        if (playerInventory.player instanceof ServerPlayer serverPlayer) {
            GloryKillsService.sync(serverPlayer);
        }
    }

    public BlockPos getAltarPos() {
        return altarPos;
    }

    public AltarTab getActiveTab() {
        return AltarTab.byId(data.get(0));
    }

    public void setActiveTab(Player player, AltarTab tab) {
        AltarTab previous = getActiveTab();
        if (previous == tab) {
            return;
        }
        if (!player.level().isClientSide()) {
            returnItemsForTab(player, previous);
        }
        data.set(0, tab.id());
        broadcastChanges();
    }

    /** Devolve ao inventário (ou dropa) os itens da aba indicada. */
    public void returnItemsForTab(Player player, AltarTab tab) {
        switch (tab) {
            case CONVERSION -> returnAltarSlot(player, CONVERSION_SLOT);
            case POTION -> {
                returnAltarSlot(player, POTION_SLOT);
                returnAltarSlot(player, POTION_OUTPUT_SLOT);
            }
            case IMPROVEMENT -> returnAltarSlot(player, IMPROVEMENT_SLOT);
            case TRADE -> {
            }
        }
    }

    private void returnAltarSlot(Player player, int slotIndex) {
        ItemStack stack = altarItems.getItem(slotIndex);
        if (stack.isEmpty()) {
            return;
        }
        altarItems.setItem(slotIndex, ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(stack);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            clearContainer(player, altarItems);
        }
    }

    public ItemStack getConversionStack() {
        return altarItems.getItem(CONVERSION_SLOT);
    }

    public void setConversionStack(ItemStack stack) {
        altarItems.setItem(CONVERSION_SLOT, stack);
    }

    public ItemStack getPotionStack() {
        return altarItems.getItem(POTION_SLOT);
    }

    public void setPotionStack(ItemStack stack) {
        altarItems.setItem(POTION_SLOT, stack);
    }

    public ItemStack getPotionOutputStack() {
        return altarItems.getItem(POTION_OUTPUT_SLOT);
    }

    public void setPotionOutputStack(ItemStack stack) {
        altarItems.setItem(POTION_OUTPUT_SLOT, stack);
    }

    public ItemStack getImprovementStack() {
        return altarItems.getItem(IMPROVEMENT_SLOT);
    }

    public void setImprovementStack(ItemStack stack) {
        altarItems.setItem(IMPROVEMENT_SLOT, stack);
    }

    public static int tabButtonScreenX(int leftPos, AltarTab tab) {
        int count = AltarTab.values().length;
        int totalWidth = count * TAB_BUTTON_SIZE + (count - 1) * TAB_BUTTON_GAP;
        int rowLeft = leftPos + (GUI_WIDTH - totalWidth) / 2;
        return rowLeft + tab.ordinal() * (TAB_BUTTON_SIZE + TAB_BUTTON_GAP);
    }

    public static int tabButtonScreenY(int topPos) {
        return topPos - CHROME_HEIGHT + (TAB_ROW_HEIGHT - TAB_BUTTON_SIZE) / 2;
    }

    public static int titleScreenY(int topPos) {
        return topPos - CHROME_HEIGHT + TAB_ROW_HEIGHT + TITLE_GAP;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, ModBlocks.ALTAR_OF_DEATH.get())
                && player.distanceToSqr(
                altarPos.getX() + 0.5D,
                altarPos.getY() + 0.5D,
                altarPos.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return original;
        }

        ItemStack stackInSlot = slot.getItem();
        original = stackInSlot.copy();

        if (index < PLAYER_INV_START) {
            if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (getActiveTab() == AltarTab.CONVERSION
                && moveItemStackTo(stackInSlot, CONVERSION_SLOT, CONVERSION_SLOT + 1, false)) {
            // conversion tab
        } else if (getActiveTab() == AltarTab.POTION
                && moveItemStackTo(stackInSlot, POTION_SLOT, POTION_SLOT + 1, false)) {
            // potion tab
        } else if (getActiveTab() == AltarTab.IMPROVEMENT
                && moveItemStackTo(stackInSlot, IMPROVEMENT_SLOT, IMPROVEMENT_SLOT + 1, false)) {
            // improvement tab
        } else if (!moveItemStackTo(stackInSlot, CONVERSION_SLOT, PLAYER_INV_START, false)) {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private class AltarTabSlot extends Slot {

        private final AltarTab tab;

        AltarTabSlot(Container container, int index, int x, int y, AltarTab tab) {
            super(container, index, x, y);
            this.tab = tab;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return getActiveTab() == tab;
        }

        @Override
        public boolean isActive() {
            return getActiveTab() == tab;
        }
    }

    private class AltarPotionInputSlot extends AltarTabSlot {

        AltarPotionInputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y, AltarTab.POTION);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return super.mayPlace(stack) && AltarPotionService.isPotionItem(stack);
        }
    }

    private class AltarImprovementInputSlot extends AltarTabSlot {

        AltarImprovementInputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y, AltarTab.IMPROVEMENT);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return super.mayPlace(stack) && !EnchantmentHelper.getEnchantments(stack).isEmpty();
        }
    }

    private class AltarTabOutputSlot extends AltarTabSlot {

        AltarTabOutputSlot(Container container, int index, int x, int y, AltarTab tab) {
            super(container, index, x, y, tab);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }
    }
}
