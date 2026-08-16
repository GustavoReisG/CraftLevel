package com.grgs.craftlevel.souls;

import com.grgs.craftlevel.capability.PlayerSoulsProvider;
import com.grgs.craftlevel.config.SoulsParser;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncPlayerSoulsPacket;
import com.grgs.craftlevel.skill.SoulAnchorSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.grgs.craftlevel.CraftLevel;

/** Eventos de souls, restore e perda na morte. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class SoulsEvents {

    private SoulsEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ensureInitialized(player);
        DeathLossService.onPlayerDeathStart(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DeathLossService.onPlayerDrops(player, event);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        Player clone = event.getEntity();
        Player original = event.getOriginal();

        for (ItemStack stack : DeathLossService.takeKeptItems(original.getUUID())) {
            if (!stack.isEmpty()) {
                clone.getInventory().add(stack);
            }
        }
        DeathLossService.XpSnapshot xp = DeathLossService.takeKeptXp(original.getUUID());
        if (xp != null) {
            clone.experienceLevel = xp.level();
            clone.experienceProgress = xp.progress();
            clone.totalExperience = xp.total();
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.isEndConquered()) {
            sync(player);
            return;
        }
        ensureInitialized(player);
        if (SoulAnchorSkill.trySaveSoul(player)) {
            player.displayClientMessage(Component.translatable("gui.craftlevel.skill.soul_anchor.saved"), true);
        } else {
            player.getCapability(PlayerSoulsProvider.PLAYER_SOULS).ifPresent(souls -> {
                souls.setSouls(Math.max(0, souls.getSouls() - 1));
            });
        }
        sync(player);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ensureInitialized(player);
        sync(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        ensureInitialized(player);
        player.getCapability(PlayerSoulsProvider.PLAYER_SOULS).ifPresent(souls -> {
            if (souls.getSouls() >= SoulsParser.maxSouls) {
                souls.setRestoreProgressTicks(0);
                return;
            }
            int progress = souls.getRestoreProgressTicks() + 1;
            int required = SoulsParser.restoreTicksRequired();
            if (progress >= required) {
                souls.setSouls(Math.min(SoulsParser.maxSouls, souls.getSouls() + 1));
                souls.setRestoreProgressTicks(0);
                sync(player);
            } else {
                souls.setRestoreProgressTicks(progress);
            }
        });
    }

    public static void ensureInitialized(Player player) {
        player.getCapability(PlayerSoulsProvider.PLAYER_SOULS).ifPresent(souls -> {
            if (!souls.isInitialized()) {
                souls.setSouls(SoulsParser.maxSouls);
                souls.setRestoreProgressTicks(0);
                souls.setInitialized(true);
            }
            // Clamp se max_souls baixou na config.
            if (souls.getSouls() > SoulsParser.maxSouls) {
                souls.setSouls(SoulsParser.maxSouls);
            }
        });
    }

    public static void sync(ServerPlayer player) {
        player.getCapability(PlayerSoulsProvider.PLAYER_SOULS).ifPresent(souls ->
                PacketHandler.sendToPlayer(player, new SyncPlayerSoulsPacket(souls.getSouls())));
    }
}
