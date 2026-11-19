package com.grgs.craftlevel.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client: animação do totem com a textura do item usado. */
public class PlayTotemAnimationPacket {

    private final int entityId;
    private final ItemStack stack;

    public PlayTotemAnimationPacket(int entityId, ItemStack stack) {
        this.entityId = entityId;
        this.stack = stack.copy();
    }

    public static void encode(PlayTotemAnimationPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeItem(packet.stack);
    }

    public static PlayTotemAnimationPacket decode(FriendlyByteBuf buf) {
        return new PlayTotemAnimationPacket(buf.readVarInt(), buf.readItem());
    }

    public static void handle(PlayTotemAnimationPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> Client.handle(packet)
        ));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        private static void handle(PlayTotemAnimationPacket packet) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            Entity entity = mc.level.getEntity(packet.entityId);
            if (entity == null) {
                return;
            }

            mc.level.playLocalSound(
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SoundEvents.TOTEM_USE,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F,
                    false
            );

            for (int i = 0; i < 64; i++) {
                mc.level.addParticle(
                        ParticleTypes.TOTEM_OF_UNDYING,
                        entity.getX(),
                        entity.getY() + 1.0D,
                        entity.getZ(),
                        (mc.level.random.nextDouble() - 0.5D) * 1.5D,
                        mc.level.random.nextDouble() * 1.2D,
                        (mc.level.random.nextDouble() - 0.5D) * 1.5D
                );
            }

            LocalPlayer local = mc.player;
            if (local != null && local.getId() == packet.entityId) {
                mc.gameRenderer.displayItemActivation(packet.stack);
            }
        }
    }
}
