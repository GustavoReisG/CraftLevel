package com.grgs.craftlevel.network;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Registra e envia todos os pacotes de rede do mod. */
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
    }

    /** Envia pacote do servidor para um jogador específico. */
    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
