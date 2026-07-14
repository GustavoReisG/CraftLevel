package com.grgs.craftlevel.network;

import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.economy.MerchantMenuHelper;
import com.grgs.craftlevel.villager.VillagerTradeEvents;
import com.grgs.craftlevel.villager.VillagerTradeService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client → server: re-roll trades por 10 esmeraldas da wallet. */
public class VillagerResortTradesPacket {

    private static final int COST = 10;

    public VillagerResortTradesPacket() {}

    public static void encode(VillagerResortTradesPacket packet, FriendlyByteBuf buf) {}

    public static VillagerResortTradesPacket decode(FriendlyByteBuf buf) {
        return new VillagerResortTradesPacket();
    }

    public static void handle(VillagerResortTradesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (!(player.containerMenu instanceof MerchantMenu menu)) {
                return;
            }
            if (!(MerchantMenuHelper.getTrader(menu) instanceof AbstractVillager villager)) {
                return;
            }

            boolean paid = player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET)
                    .map(wallet -> wallet.tryRemoveEmeralds(COST))
                    .orElse(false);
            if (!paid) {
                return;
            }

            VillagerTradeService.resortTrades(villager);
            menu.broadcastChanges();
            PacketHandler.sendToPlayer(player, new SyncMerchantOffersPacket(menu.containerId, villager.getOffers()));
            VillagerTradeEvents.syncAfterTrade(player, villager);
            PacketHandler.sendToPlayer(player, new SyncEmeraldWalletPacket(
                    player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET)
                            .map(cap -> cap.getBalance())
                            .orElse(0L)
            ));
        });
        ctx.get().setPacketHandled(true);
    }
}
