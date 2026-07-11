package com.grgs.craftlevel.commands;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.IPlayerLevel;
import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.ConfigManager;
import com.grgs.craftlevel.config.ConfigParser;
import com.grgs.craftlevel.economy.EconomyService;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncPlayerLevelPacket;
import com.grgs.craftlevel.scaling.StatRefreshService;
import com.grgs.craftlevel.util.DebugLog;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/** Comandos admin: /craftlevel lp|level|reload (requer OP 2). */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public class CraftLevelCommands {

    private CraftLevelCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("craftlevel")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("lp")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> setLp(
                                                                ctx.getSource(),
                                                                EntityArgument.getEntity(ctx, "target"),
                                                                IntegerArgumentType.getInteger(ctx, "amount")
                                                        ))))))
                        .then(Commands.literal("level")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> setLevel(
                                                                ctx.getSource(),
                                                                EntityArgument.getEntity(ctx, "target"),
                                                                IntegerArgumentType.getInteger(ctx, "level")
                                                        ))))))
                        .then(Commands.literal("reload")
                                .executes(ctx -> reload(ctx.getSource())))
        );
    }

    private static int reload(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        try {
            ConfigManager.reload(server);
            EconomyService.onEconomyConfigReload(server);
        } catch (IOException e) {
            CraftLevel.LOGGER.error("[CraftLevel] Failed to reload config", e);
            source.sendFailure(Component.literal("[CraftLevel] Erro ao recarregar configs. Veja o log do servidor."));
            return 0;
        }

        StatRefreshService.RefreshResult result = StatRefreshService.refreshAllLoaded(server);

        DebugLog.info("Reload por {}: {} players, {} mobs", source.getTextName(), result.players(), result.mobs());

        source.sendSuccess(() -> Component.literal(
                "[CraftLevel] Config recarregado. Stats reaplicados em "
                        + result.players() + " jogador(es) e " + result.mobs() + " mob(s)."
        ), true);

        return 1;
    }

    private static int setLp(CommandSourceStack source, Entity target, int amount) throws CommandSyntaxException {
        if (!(target instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("[CraftLevel] LP só pode ser definido em jogadores."));
            return 0;
        }

        IPlayerLevel cap = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL)
                .orElse(null);
        if (cap == null) {
            source.sendFailure(Component.literal("[CraftLevel] Capability de level não encontrada."));
            return 0;
        }

        cap.setLp(amount);
        syncPlayer(player, cap);

        DebugLog.info("Comando LP: {} = {}", player.getGameProfile().getName(), amount);

        source.sendSuccess(() -> Component.literal(
                "[CraftLevel] LP de " + player.getGameProfile().getName() + " definido para " + amount + "."
        ), true);

        return 1;
    }

    private static int setLevel(CommandSourceStack source, Entity target, int level) throws CommandSyntaxException {
        int maxLevel = ConfigParser.getMaxLevel();
        if (level > maxLevel) {
            source.sendFailure(Component.literal(
                    "[CraftLevel] Level máximo é " + maxLevel + " (config: lp_scaling.lvl_max)."
            ));
            return 0;
        }

        if (target instanceof ServerPlayer player) {
            IPlayerLevel cap = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL)
                    .orElse(null);
            if (cap == null) {
                source.sendFailure(Component.literal("[CraftLevel] Capability de level não encontrada."));
                return 0;
            }

            cap.setLevel(level);
            StatRefreshService.refreshPlayer(player, level);
            syncPlayer(player, cap);

            DebugLog.info("Comando level (player): {} = {}", player.getGameProfile().getName(), level);

            source.sendSuccess(() -> Component.literal(
                    "[CraftLevel] Level de " + player.getGameProfile().getName() + " definido para " + level + "."
            ), true);

            return 1;
        }

        if (target instanceof Mob mob) {
            return mob.getCapability(MobLevelProvider.MOB_LEVEL)
                    .map(cap -> {
                        cap.setLevel(level);
                        StatRefreshService.refreshMob(mob, level);

                        DebugLog.info("Comando level (mob): {} = {}", mob.getType(), level);

                        source.sendSuccess(() -> Component.literal(
                                "[CraftLevel] Level do mob definido para " + level + "."
                        ), true);

                        return 1;
                    })
                    .orElseGet(() -> {
                        source.sendFailure(Component.literal("[CraftLevel] Capability de level não encontrada."));
                        return 0;
                    });
        }

        source.sendFailure(Component.literal("[CraftLevel] Level só pode ser definido em jogadores ou mobs."));
        return 0;
    }

    private static void syncPlayer(ServerPlayer player, IPlayerLevel cap) {
        PacketHandler.sendToPlayer(player, new SyncPlayerLevelPacket(
                cap.getLevel(),
                cap.getLp(),
                cap.getLpToNextLevel()
        ));
    }
}
