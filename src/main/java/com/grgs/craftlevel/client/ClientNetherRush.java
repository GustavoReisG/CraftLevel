package com.grgs.craftlevel.client;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.skill.NetherRushSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Aplica a investida no cliente (o movimento do player local é client-authoritative).
 * {@code move(SELF)} colide com blocos — não atravessa paredes.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID, value = Dist.CLIENT)
public final class ClientNetherRush {

    private static Vec3 direction;
    private static int remainingTicks;
    private static boolean hadNoGravity;

    private ClientNetherRush() {}

    public static void start(double dx, double dy, double dz, int ticks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || ticks <= 0) {
            return;
        }
        Vec3 look = new Vec3(dx, dy, dz);
        if (look.lengthSqr() < 1.0E-6D) {
            return;
        }
        direction = look.normalize();
        remainingTicks = ticks;
        hadNoGravity = player.isNoGravity();
        player.setNoGravity(true);
        player.setOnGround(false);
        player.fallDistance = 0.0F;
    }

    public static void stop() {
        LocalPlayer player = Minecraft.getInstance().player;
        remainingTicks = 0;
        if (player != null) {
            player.setNoGravity(hadNoGravity);
            if (direction != null) {
                player.setDeltaMovement(direction.scale(0.2D));
            }
        }
        direction = null;
    }

    public static boolean isDashing() {
        return remainingTicks > 0 && direction != null;
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        if (!isDashing()) {
            return;
        }
        event.getInput().forwardImpulse = 0.0F;
        event.getInput().leftImpulse = 0.0F;
        event.getInput().jumping = false;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!event.player.level().isClientSide() || !isDashing()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (event.player != mc.player) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || !player.isAlive()) {
            stop();
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            // Evita o travel() vanilla somar passo/gravidade em cima da investida.
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }

        player.move(MoverType.SELF, direction.scale(NetherRushSkill.SPEED_PER_TICK));
        player.setDeltaMovement(direction.scale(NetherRushSkill.SPEED_PER_TICK));
        player.setOnGround(false);
        player.fallDistance = 0.0F;
        remainingTicks--;
        if (remainingTicks <= 0) {
            stop();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        remainingTicks = 0;
        direction = null;
    }
}
