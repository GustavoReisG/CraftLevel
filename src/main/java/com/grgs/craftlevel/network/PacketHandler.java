package com.grgs.craftlevel.network;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/** Canal Forge SimpleChannel — server → client e client → server. */
public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CraftLevel.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /** IDs: 0=player level, 1=mob level sync, 2=mob level request, 3=damage indicator */
    public static void register() {
        CHANNEL.registerMessage(
                0,
                SyncPlayerLevelPacket.class,
                SyncPlayerLevelPacket::encode,
                SyncPlayerLevelPacket::decode,
                SyncPlayerLevelPacket::handle
        );

        CHANNEL.registerMessage(
                1,
                SyncMobLevelPacket.class,
                SyncMobLevelPacket::encode,
                SyncMobLevelPacket::decode,
                SyncMobLevelPacket::handle
        );

        CHANNEL.registerMessage(
                2,
                RequestMobLevelPacket.class,
                RequestMobLevelPacket::encode,
                RequestMobLevelPacket::decode,
                RequestMobLevelPacket::handle
        );

        CHANNEL.registerMessage(
                3,
                DamageIndicatorPacket.class,
                DamageIndicatorPacket::encode,
                DamageIndicatorPacket::decode,
                DamageIndicatorPacket::handle
        );

        CHANNEL.registerMessage(
                4,
                SyncEmeraldWalletPacket.class,
                SyncEmeraldWalletPacket::encode,
                SyncEmeraldWalletPacket::decode,
                SyncEmeraldWalletPacket::handle
        );

        CHANNEL.registerMessage(
                5,
                WalletWithdrawPacket.class,
                WalletWithdrawPacket::encode,
                WalletWithdrawPacket::decode,
                WalletWithdrawPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                6,
                SyncVillagerBalancePacket.class,
                SyncVillagerBalancePacket::encode,
                SyncVillagerBalancePacket::decode,
                SyncVillagerBalancePacket::handle
        );

        CHANNEL.registerMessage(
                7,
                VillagerResortTradesPacket.class,
                VillagerResortTradesPacket::encode,
                VillagerResortTradesPacket::decode,
                VillagerResortTradesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                8,
                SyncMerchantOffersPacket.class,
                SyncMerchantOffersPacket::encode,
                SyncMerchantOffersPacket::decode,
                SyncMerchantOffersPacket::handle
        );

        CHANNEL.registerMessage(
                9,
                SyncPlayerSoulsPacket.class,
                SyncPlayerSoulsPacket::encode,
                SyncPlayerSoulsPacket::decode,
                SyncPlayerSoulsPacket::handle
        );
    }

    /** Envia pacote do servidor para um jogador específico. */
    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
