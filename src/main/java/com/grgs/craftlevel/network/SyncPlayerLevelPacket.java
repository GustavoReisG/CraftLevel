package com.grgs.craftlevel.network;

import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.client.CraftLevelHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sincroniza level, LP atual e LP necessário do jogador para o cliente. */
public class SyncPlayerLevelPacket {

    private final int level;
    private final int lp;
    private final int lpNeeded;

    public SyncPlayerLevelPacket(int level, int lp, int lpNeeded) {
        this.level = level;
        this.lp = lp;
        this.lpNeeded = lpNeeded;
    }

    public static void encode(SyncPlayerLevelPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.level);
        buf.writeInt(packet.lp);
        buf.writeInt(packet.lpNeeded);
    }

    public static SyncPlayerLevelPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerLevelPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    /** Atualiza HUD e capability local do jogador no cliente. */
    public static void handle(SyncPlayerLevelPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CraftLevelHUD.updateCache(packet.lp, packet.lpNeeded, packet.level);

            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(cap -> {
                    cap.setLevel(packet.level);
                    cap.setLp(packet.lp);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
