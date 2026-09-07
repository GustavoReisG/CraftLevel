package com.grgs.craftlevel.network;

import com.grgs.craftlevel.altar.AltarConversionService;
import com.grgs.craftlevel.altar.AltarImprovementService;
import com.grgs.craftlevel.altar.AltarPotionService;
import com.grgs.craftlevel.menu.AltarOfDeathMenu;
import com.grgs.craftlevel.menu.AltarTab;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Ações do altar enviadas pelo cliente (troca de aba, convert, potions, improve). */
public class AltarActionPacket {

    public enum Action {
        SWITCH_TAB,
        CONVERT,
        POTION_TIME,
        POTION_EFFECT,
        IMPROVE,
        TRADE_BUY
    }

    private final Action action;
    private final int tabId;
    private final boolean shift;
    private final int data;

    public AltarActionPacket(Action action, int tabId, boolean shift) {
        this(action, tabId, shift, 0);
    }

    public AltarActionPacket(Action action, int tabId, boolean shift, int data) {
        this.action = action;
        this.tabId = tabId;
        this.shift = shift;
        this.data = data;
    }

    public static void encode(AltarActionPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeVarInt(packet.tabId);
        buf.writeBoolean(packet.shift);
        buf.writeVarInt(packet.data);
    }

    public static AltarActionPacket decode(FriendlyByteBuf buf) {
        return new AltarActionPacket(
                buf.readEnum(Action.class),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readVarInt()
        );
    }

    public static void handle(AltarActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            AbstractContainerMenu menu = player.containerMenu;
            if (!(menu instanceof AltarOfDeathMenu altarMenu)) {
                return;
            }

            switch (packet.action) {
                case SWITCH_TAB -> altarMenu.setActiveTab(player, AltarTab.byId(packet.tabId));
                case CONVERT -> AltarConversionService.tryConvert(
                        player,
                        altarMenu,
                        altarMenu.getAltarPos(),
                        packet.shift
                );
                case POTION_TIME -> AltarPotionService.tryExtendDuration(
                        player,
                        altarMenu,
                        altarMenu.getAltarPos()
                );
                case POTION_EFFECT -> AltarPotionService.tryUpgradeEffect(
                        player,
                        altarMenu,
                        altarMenu.getAltarPos()
                );
                case IMPROVE -> AltarImprovementService.tryImprove(
                        player,
                        altarMenu,
                        altarMenu.getAltarPos(),
                        packet.data
                );
                case TRADE_BUY -> player.displayClientMessage(
                        Component.translatable("gui.craftlevel.altar.coming_soon"),
                        true
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
