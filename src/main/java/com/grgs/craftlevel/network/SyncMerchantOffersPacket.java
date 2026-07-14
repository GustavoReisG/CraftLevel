package com.grgs.craftlevel.network;

import com.grgs.craftlevel.economy.MerchantMenuHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client: atualiza ofertas do villager enquanto o menu está aberto. */
public class SyncMerchantOffersPacket {

    private final int containerId;
    private final MerchantOffers offers;

    public SyncMerchantOffersPacket(int containerId, MerchantOffers offers) {
        this.containerId = containerId;
        this.offers = offers;
    }

    public static void encode(SyncMerchantOffersPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.containerId);
        MerchantOffersNetwork.write(buf, packet.offers);
    }

    public static SyncMerchantOffersPacket decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        MerchantOffers offers = MerchantOffersNetwork.read(buf);
        return new SyncMerchantOffersPacket(containerId, offers);
    }

    public static void handle(SyncMerchantOffersPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.player.containerMenu.containerId != packet.containerId) {
                return;
            }
            if (!(minecraft.player.containerMenu instanceof MerchantMenu menu)) {
                return;
            }

            Merchant trader = MerchantMenuHelper.getTrader(menu);
            if (trader == null) {
                return;
            }

            MerchantOffers current = trader.getOffers();
            current.clear();
            current.addAll(packet.offers);
            menu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
