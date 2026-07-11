package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.menu.EmeraldWalletMenu;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.WalletWithdrawPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Tela da wallet alinhada à textura emerald_wallet.png (malha hopper). */
@OnlyIn(Dist.CLIENT)
public class EmeraldWalletScreen extends AbstractContainerScreen<EmeraldWalletMenu> {

    private static final ResourceLocation CONTAINER_TEXTURE =
            new ResourceLocation(CraftLevel.MODID, "textures/gui/emerald_wallet.png");

    private static final int WALLET_LABEL_Y = 39;

    private static long cachedBalance;

    public EmeraldWalletScreen(EmeraldWalletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = EmeraldWalletMenu.GUI_WIDTH;
        this.imageHeight = EmeraldWalletMenu.GUI_HEIGHT;
        this.inventoryLabelY = -100;
        this.titleLabelY = -100;
    }

    public static void updateBalance(long balance) {
        cachedBalance = balance;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("\u00A7c-"), btn -> {
                    boolean shift = hasShiftDown();
                    PacketHandler.CHANNEL.sendToServer(new WalletWithdrawPacket(
                            shift ? WalletWithdrawPacket.Action.TO_INVENTORY : WalletWithdrawPacket.Action.TO_CURSOR
                    ));
                })
                .bounds(
                        EmeraldWalletMenu.withdrawButtonScreenX(leftPos),
                        EmeraldWalletMenu.withdrawButtonScreenY(topPos),
                        18,
                        18
                )
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(graphics);
        graphics.blit(CONTAINER_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int iconX = leftPos + 8;
        int iconY = topPos + 6;
        WalletIconRenderer.draw(graphics, iconX, iconY, 16);
        graphics.drawString(font, String.valueOf(cachedBalance), iconX + 20, iconY + 5, 0x404040, false);

        graphics.drawString(
                font,
                Component.translatable("gui.craftlevel.emerald_wallet.section"),
                leftPos + 8,
                topPos + WALLET_LABEL_Y,
                0x404040,
                false
        );
    }
}
