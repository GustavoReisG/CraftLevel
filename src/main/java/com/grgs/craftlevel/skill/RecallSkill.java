package com.grgs.craftlevel.skill;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.glory.GloryKillsService;
import com.grgs.craftlevel.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Function;

/**
 * Recall: gera uma Recall Rune. Bind no chão (sneak + face TOP).
 * Segurar a runa carrega um portal e teleporta. CD 10 min a partir do teleporte.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class RecallSkill {

    public static final int COOLDOWN_TICKS = 10 * 60 * 20;
    public static final int CHARGE_TICKS = 80;

    private RecallSkill() {}

    public static int cooldownSeconds() {
        return COOLDOWN_TICKS / 20;
    }

    public static int cooldownMinutes() {
        return cooldownSeconds() / 60;
    }

    public static int skillLevel(Player player) {
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getSkillLevel(SkillCatalog.RECALL))
                .orElse(0);
    }

    public static boolean isReady(ServerPlayer player) {
        if (skillLevel(player) <= 0) {
            return false;
        }
        return player.getCapability(PlayerGloryProvider.PLAYER_GLORY)
                .map(glory -> glory.getCooldownReadyAt(SkillCatalog.RECALL)
                        <= player.serverLevel().getGameTime())
                .orElse(false);
    }

    public static boolean activate(ServerPlayer player, int level) {
        ItemStack rune = new ItemStack(ModItems.RECALL_RUNE.get());
        if (!player.addItem(rune)) {
            player.drop(rune, false);
        }
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS,
                0.7F,
                1.25F
        );
        return true;
    }

    public static void startCooldown(ServerPlayer player) {
        long readyAt = player.serverLevel().getGameTime() + COOLDOWN_TICKS;
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory ->
                glory.setCooldownReadyAt(SkillCatalog.RECALL, readyAt));
        ActiveSkillTracker.put(player, SkillCatalog.RECALL, readyAt);
        GloryKillsService.sync(player);
    }

    public static void teleport(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
        ServerLevel targetLevel = player.server.getLevel(dimension);
        if (targetLevel == null) {
            return;
        }
        Vec3 dest = safePos(targetLevel, pos);
        if (player.serverLevel().dimension() == dimension) {
            player.teleportTo(dest.x, dest.y, dest.z);
        } else {
            player.changeDimension(targetLevel, new ITeleporter() {
                @Override
                public PortalInfo getPortalInfo(
                        net.minecraft.world.entity.Entity entity,
                        ServerLevel destLevel,
                        Function<ServerLevel, PortalInfo> defaultPortalInfo
                ) {
                    return new PortalInfo(dest, Vec3.ZERO, player.getYRot(), player.getXRot());
                }
            });
        }
        targetLevel.playSound(
                null,
                BlockPos.containing(dest),
                SoundEvents.PORTAL_TRAVEL,
                SoundSource.PLAYERS,
                0.6F,
                1.15F
        );
        startCooldown(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(PlayerGloryProvider.PLAYER_GLORY).ifPresent(glory -> {
            long readyAt = glory.getCooldownReadyAt(SkillCatalog.RECALL);
            long now = player.serverLevel().getGameTime();
            if (readyAt > now && glory.getSkillLevel(SkillCatalog.RECALL) > 0) {
                ActiveSkillTracker.put(player, SkillCatalog.RECALL, readyAt);
            }
        });
    }

    private static Vec3 safePos(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 8; i++) {
            if (level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()
                    && level.getBlockState(cursor.above()).getCollisionShape(level, cursor.above()).isEmpty()) {
                return new Vec3(cursor.getX() + 0.5D, cursor.getY(), cursor.getZ() + 0.5D);
            }
            cursor.move(0, 1, 0);
        }
        return new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }
}
