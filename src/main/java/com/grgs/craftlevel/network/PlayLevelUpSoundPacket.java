package com.grgs.craftlevel.network;

import com.grgs.craftlevel.client.LevelUpSoundQueue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Pede ao cliente para tocar o som de level up (ignora se já estiver tocando). */
public class PlayLevelUpSoundPacket {

    public PlayLevelUpSoundPacket() {}

    public static void encode(PlayLevelUpSoundPacket packet, FriendlyByteBuf buf) {
        // sem payload
    }

    public static PlayLevelUpSoundPacket decode(FriendlyByteBuf buf) {
        return new PlayLevelUpSoundPacket();
    }

    public static void handle(PlayLevelUpSoundPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> LevelUpSoundQueue::tryPlay
        ));
        ctx.get().setPacketHandled(true);
    }
}
