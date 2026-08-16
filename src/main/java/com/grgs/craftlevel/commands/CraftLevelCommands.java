package com.grgs.craftlevel.commands;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.IPlayerEmeraldBank;
import com.grgs.craftlevel.capability.IPlayerEmeraldWallet;
import com.grgs.craftlevel.capability.IPlayerGlory;
import com.grgs.craftlevel.capability.IPlayerLevel;
import com.grgs.craftlevel.capability.IPlayerSouls;
import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerEmeraldBankProvider;
import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.capability.PlayerGloryProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.capability.PlayerSoulsProvider;
import com.grgs.craftlevel.glory.GloryKillsService;
import com.grgs.craftlevel.config.ConfigManager;
import com.grgs.craftlevel.config.ConfigParser;
import com.grgs.craftlevel.config.SoulsParser;
import com.grgs.craftlevel.economy.EconomyService;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncEmeraldBankPacket;
import com.grgs.craftlevel.network.SyncEmeraldWalletPacket;
import com.grgs.craftlevel.network.SyncPlayerLevelPacket;
import com.grgs.craftlevel.scaling.StatRefreshService;
import com.grgs.craftlevel.souls.SoulsEvents;
import com.grgs.craftlevel.util.DebugLog;
import com.grgs.craftlevel.villager.VillagerTradeService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/** Comandos admin: /craftlevel lp|level|souls|emerald_bag|emerald_bank|glory_kills|skill_points|reset_skills|reload (OP 2). */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public class CraftLevelCommands {

    private enum Op { SET, ADD, REMOVE }

    private CraftLevelCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("craftlevel")
                        .requires(source -> source.hasPermission(2))
                        .then(intOps("lp", CraftLevelCommands::mutateLp))
                        .then(intOps("level", CraftLevelCommands::mutateLevel))
                        .then(intOps("souls", CraftLevelCommands::mutateSouls))
                        .then(longOps("emerald_bag", CraftLevelCommands::mutateWallet))
                        .then(longOps("emerald_bank", CraftLevelCommands::mutateBank))
                        .then(intOps("glory_kills", CraftLevelCommands::mutateGloryKills))
                        .then(intOps("skill_points", CraftLevelCommands::mutateSkillPoints))
                        .then(Commands.literal("reset_skills")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> resetSkills(
                                                ctx.getSource(),
                                                EntityArgument.getEntity(ctx, "target")
                                        ))))
                        .then(Commands.literal("reload")
                                .executes(ctx -> reload(ctx.getSource())))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intOps(String name, IntMutator mutator) {
        return Commands.literal(name)
                .then(intOp("set", Op.SET, mutator))
                .then(intOp("add", Op.ADD, mutator))
                .then(intOp("remove", Op.REMOVE, mutator));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intOp(String literal, Op op, IntMutator mutator) {
        return Commands.literal(literal)
                .then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> mutator.run(
                                        ctx.getSource(),
                                        EntityArgument.getEntity(ctx, "target"),
                                        op,
                                        IntegerArgumentType.getInteger(ctx, "amount")
                                ))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> longOps(String name, LongMutator mutator) {
        return Commands.literal(name)
                .then(longOp("set", Op.SET, mutator))
                .then(longOp("add", Op.ADD, mutator))
                .then(longOp("remove", Op.REMOVE, mutator));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> longOp(String literal, Op op, LongMutator mutator) {
        return Commands.literal(literal)
                .then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                .executes(ctx -> mutator.run(
                                        ctx.getSource(),
                                        EntityArgument.getEntity(ctx, "target"),
                                        op,
                                        LongArgumentType.getLong(ctx, "amount")
                                ))));
    }

    private static int reload(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        try {
            ConfigManager.reload(server);
            EconomyService.onEconomyConfigReload(server);
            VillagerTradeService.onReload();
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

    private static int mutateLp(CommandSourceStack source, Entity target, Op op, int amount)
            throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source, target, "LP");
        if (player == null) {
            return 0;
        }

        IPlayerLevel cap = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).orElse(null);
        if (cap == null) {
            source.sendFailure(Component.literal("[CraftLevel] Capability de level não encontrada."));
            return 0;
        }

        int next = applyIntOp(cap.getLp(), op, amount, 0, Integer.MAX_VALUE);
        cap.setLp(next);
        syncPlayerLevel(player, cap);

        DebugLog.info("Comando LP {}: {} = {}", op.name().toLowerCase(), player.getGameProfile().getName(), next);
        return success(source, "LP de " + player.getGameProfile().getName() + " definido para " + next + ".");
    }

    private static int mutateLevel(CommandSourceStack source, Entity target, Op op, int amount)
            throws CommandSyntaxException {
        int maxLevel = ConfigParser.getMaxLevel();

        if (target instanceof ServerPlayer player) {
            IPlayerLevel cap = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).orElse(null);
            if (cap == null) {
                source.sendFailure(Component.literal("[CraftLevel] Capability de level não encontrada."));
                return 0;
            }

            int next = applyIntOp(cap.getLevel(), op, amount, 1, maxLevel);
            cap.setLevel(next);
            StatRefreshService.refreshPlayer(player, next);
            syncPlayerLevel(player, cap);

            DebugLog.info("Comando level (player) {}: {} = {}", op.name().toLowerCase(),
                    player.getGameProfile().getName(), next);
            return success(source, "Level de " + player.getGameProfile().getName() + " definido para " + next + ".");
        }

        if (target instanceof Mob mob) {
            return mob.getCapability(MobLevelProvider.MOB_LEVEL)
                    .map(cap -> {
                        int next = applyIntOp(cap.getLevel(), op, amount, 1, maxLevel);
                        cap.setLevel(next);
                        StatRefreshService.refreshMob(mob, next);
                        PacketHandler.syncMobLevel(mob);

                        DebugLog.info("Comando level (mob) {}: {} = {}", op.name().toLowerCase(), mob.getType(), next);
                        return success(source, "Level do mob definido para " + next + ".");
                    })
                    .orElseGet(() -> {
                        source.sendFailure(Component.literal("[CraftLevel] Capability de level não encontrada."));
                        return 0;
                    });
        }

        source.sendFailure(Component.literal("[CraftLevel] Level só pode ser definido em jogadores ou mobs."));
        return 0;
    }

    private static int mutateSouls(CommandSourceStack source, Entity target, Op op, int amount)
            throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source, target, "Souls");
        if (player == null) {
            return 0;
        }

        SoulsEvents.ensureInitialized(player);
        IPlayerSouls cap = player.getCapability(PlayerSoulsProvider.PLAYER_SOULS).orElse(null);
        if (cap == null) {
            source.sendFailure(Component.literal("[CraftLevel] Capability de souls não encontrada."));
            return 0;
        }

        int next = applyIntOp(cap.getSouls(), op, amount, 0, SoulsParser.maxSouls);
        cap.setSouls(next);
        SoulsEvents.sync(player);

        DebugLog.info("Comando souls {}: {} = {}", op.name().toLowerCase(),
                player.getGameProfile().getName(), next);
        return success(source, "Souls de " + player.getGameProfile().getName() + " definido para " + next + ".");
    }

    private static int mutateWallet(CommandSourceStack source, Entity target, Op op, long amount)
            throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source, target, "Emerald bag");
        if (player == null) {
            return 0;
        }

        EconomyService.ensurePlayerEconomy(player);
        IPlayerEmeraldWallet cap = player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).orElse(null);
        if (cap == null) {
            source.sendFailure(Component.literal("[CraftLevel] Capability de emerald bag não encontrada."));
            return 0;
        }

        long next = applyLongOp(cap.getBalance(), op, amount);
        cap.setBalance(next);
        PacketHandler.sendToPlayer(player, new SyncEmeraldWalletPacket(next));

        DebugLog.info("Comando emerald_bag {}: {} = {}", op.name().toLowerCase(),
                player.getGameProfile().getName(), next);
        return success(source, "Emerald bag de " + player.getGameProfile().getName() + " definida para " + next + ".");
    }

    private static int mutateBank(CommandSourceStack source, Entity target, Op op, long amount)
            throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source, target, "Emerald bank");
        if (player == null) {
            return 0;
        }

        EconomyService.ensurePlayerEconomy(player);
        IPlayerEmeraldBank cap = player.getCapability(PlayerEmeraldBankProvider.EMERALD_BANK).orElse(null);
        if (cap == null) {
            source.sendFailure(Component.literal("[CraftLevel] Capability de emerald bank não encontrada."));
            return 0;
        }

        long next = applyLongOp(cap.getBalance(), op, amount);
        cap.setBalance(next);
        PacketHandler.sendToPlayer(player, new SyncEmeraldBankPacket(next));

        DebugLog.info("Comando emerald_bank {}: {} = {}", op.name().toLowerCase(),
                player.getGameProfile().getName(), next);
        return success(source, "Emerald bank de " + player.getGameProfile().getName() + " definido para " + next + ".");
    }

    private static int mutateGloryKills(CommandSourceStack source, Entity target, Op op, int amount)
            throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source, target, "Glory Kills");
        if (player == null) {
            return 0;
        }

        IPlayerGlory glory = player.getCapability(PlayerGloryProvider.PLAYER_GLORY).orElse(null);
        if (glory == null) {
            source.sendFailure(Component.literal("[CraftLevel] Capability de glory não encontrada."));
            return 0;
        }

        int next = applyIntOp(glory.getGloryKills(), op, amount, 0, Integer.MAX_VALUE);
        glory.setGloryKills(next);
        GloryKillsService.sync(player);

        DebugLog.info("Comando glory_kills {}: {} = {}", op.name().toLowerCase(),
                player.getGameProfile().getName(), next);
        return success(source, "Glory Kills de " + player.getGameProfile().getName() + " definido para " + next + ".");
    }

    private static int mutateSkillPoints(CommandSourceStack source, Entity target, Op op, int amount)
            throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source, target, "Skill Points");
        if (player == null) {
            return 0;
        }

        IPlayerGlory glory = player.getCapability(PlayerGloryProvider.PLAYER_GLORY).orElse(null);
        if (glory == null) {
            source.sendFailure(Component.literal("[CraftLevel] Capability de glory não encontrada."));
            return 0;
        }

        int next = applyIntOp(glory.getSkillPoints(), op, amount, 0, Integer.MAX_VALUE);
        glory.setSkillPoints(next);
        GloryKillsService.sync(player);

        DebugLog.info("Comando skill_points {}: {} = {}", op.name().toLowerCase(),
                player.getGameProfile().getName(), next);
        return success(source, "Skill Points de " + player.getGameProfile().getName() + " definido para " + next + ".");
    }

    private static int resetSkills(CommandSourceStack source, Entity target) {
        ServerPlayer player = requirePlayer(source, target, "Reset skills");
        if (player == null) {
            return 0;
        }

        IPlayerGlory glory = player.getCapability(PlayerGloryProvider.PLAYER_GLORY).orElse(null);
        if (glory == null) {
            source.sendFailure(Component.literal("[CraftLevel] Capability de glory não encontrada."));
            return 0;
        }

        glory.resetSkills();
        GloryKillsService.sync(player);

        DebugLog.info("Comando reset_skills: {}", player.getGameProfile().getName());
        return success(source, "Skills de " + player.getGameProfile().getName() + " resetadas.");
    }

    private static ServerPlayer requirePlayer(CommandSourceStack source, Entity target, String label) {
        if (target instanceof ServerPlayer player) {
            return player;
        }
        source.sendFailure(Component.literal("[CraftLevel] " + label + " só pode ser definido em jogadores."));
        return null;
    }

    private static int applyIntOp(int current, Op op, int amount, int min, int max) {
        return switch (op) {
            case SET -> clampInt(amount, min, max);
            case ADD -> clampInt(current + amount, min, max);
            case REMOVE -> clampInt(current - amount, min, max);
        };
    }

    private static long applyLongOp(long current, Op op, long amount) {
        return switch (op) {
            case SET -> Math.max(0L, amount);
            case ADD -> saturatingAdd(current, amount);
            case REMOVE -> Math.max(0L, current - amount);
        };
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Evita overflow em add de wallet/bank. */
    private static long saturatingAdd(long current, long amount) {
        try {
            return Math.addExact(current, amount);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    private static int success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[CraftLevel] " + message), true);
        return 1;
    }

    private static void syncPlayerLevel(ServerPlayer player, IPlayerLevel cap) {
        PacketHandler.sendToPlayer(player, new SyncPlayerLevelPacket(
                cap.getLevel(),
                cap.getLp(),
                cap.getLpToNextLevel()
        ));
    }

    @FunctionalInterface
    private interface IntMutator {
        int run(CommandSourceStack source, Entity target, Op op, int amount) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private interface LongMutator {
        int run(CommandSourceStack source, Entity target, Op op, long amount) throws CommandSyntaxException;
    }
}
