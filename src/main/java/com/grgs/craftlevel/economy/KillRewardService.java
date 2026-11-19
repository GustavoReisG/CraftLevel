package com.grgs.craftlevel.economy;

import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.EconomyParser;
import com.grgs.craftlevel.item.ItemRunes;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncEmeraldWalletPacket;
import com.grgs.craftlevel.util.DebugLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/** Crédito de esmeraldas na wallet ao matar mobs (kill_rewards). */
public final class KillRewardService {

    private KillRewardService() {}

    public static void rewardKill(Player player, Mob mob) {
        if (player.level().isClientSide() || mob instanceof AbstractVillager) {
            return;
        }
        String id = entityId(mob);
        if (EconomyParser.isKillBlacklisted(id)) {
            return;
        }

        int playerLevel = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL)
                .map(cap -> Math.max(1, cap.getLevel()))
                .orElse(1);
        int mobLevel = mob.getCapability(MobLevelProvider.MOB_LEVEL)
                .map(cap -> Math.max(0, cap.getLevel()))
                .orElse(0);
        if (mobLevel <= 0) {
            return;
        }

        long amount = applyFortune(player, calculateReward(playerLevel, mobLevel, id));
        if (amount <= 0L) {
            return;
        }

        creditWallet(player, amount, "Kill reward +{}e (playerLvl={} mobLvl={} {}) → wallet {}",
                amount, playerLevel, mobLevel, id);
    }

    /** Arena: 1 a 3 esmeraldas por mob, sem o cálculo de nível. */
    public static void rewardArenaKill(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        long amount = applyFortune(player, 1L + player.getRandom().nextInt(3));
        creditWallet(player, amount, "Arena reward +{}e → wallet {}", amount);
    }

    /** +5% esmeraldas por peça com Rune of Fortune (arredonda para cima se bônus &gt; 0). */
    private static long applyFortune(Player player, long base) {
        if (base <= 0L) {
            return 0L;
        }
        float bonus = ItemRunes.fortuneBonus(player);
        if (bonus <= 0.0F) {
            return base;
        }
        return Math.max(base, (long) Math.ceil(base * (1.0D + bonus)));
    }

    private static void creditWallet(Player player, long amount, String logFmt, Object... logArgs) {
        EconomyService.ensurePlayerEconomy(player);
        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(wallet -> {
            wallet.addEmeralds(amount);
            if (player instanceof ServerPlayer serverPlayer) {
                PacketHandler.sendToPlayer(
                        serverPlayer,
                        new SyncEmeraldWalletPacket(EconomyService.getWalletBalance(player))
                );
            }
            Object[] args = new Object[logArgs.length + 1];
            System.arraycopy(logArgs, 0, args, 0, logArgs.length);
            args[logArgs.length] = wallet.getBalance();
            DebugLog.info(logFmt, args);
        });
    }

    static long calculateReward(int playerLevel, int mobLevel, String entityId) {
        int gap = Math.max(1, EconomyParser.killLevelGap);
        int diff = mobLevel - playerLevel;

        long amount;
        if (diff <= -gap) {
            amount = EconomyParser.killLowLevelEmeralds;
        } else if (diff >= gap) {
            amount = (long) EconomyParser.killSameLevelEmeralds + EconomyParser.killHighLevelBonus;
        } else {
            amount = EconomyParser.killSameLevelEmeralds;
        }

        if (EconomyParser.isKillBoss(entityId)) {
            amount += EconomyParser.killBossBonus;
        }
        return Math.max(0L, amount);
    }

    private static String entityId(Mob mob) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return key != null ? key.toString() : "";
    }
}
