package com.grgs.craftlevel.network;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CraftLevel.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.registerMessage(
                0,
                SyncPlayerLevelPacket.class,
                SyncPlayerLevelPacket::encode,
                SyncPlayerLevelPacket::decode,
                SyncPlayerLevelPacket::handle
        );

        CHANNEL.registerMessage(
                1, // ← ID 1, o do jogador é 0
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
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}