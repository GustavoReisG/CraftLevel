package com.grgs.craftlevel.souls;

import com.grgs.craftlevel.block.ModBlocks;
import com.grgs.craftlevel.block.PlayerGravestoneBlock;
import com.grgs.craftlevel.block.PlayerGravestoneBlockEntity;
import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.capability.PlayerSoulsProvider;
import com.grgs.craftlevel.config.SoulsParser;
import com.grgs.craftlevel.item.ItemRunes;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncEmeraldWalletPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Perda na morte quando souls == 0, e preservação quando souls &gt; 0.
 * <p>
 * Classifica slots no {@code LivingDeathEvent} (inventário ainda cheio) e
 * entrega itens no {@code LivingDropsEvent}.
 */
public final class DeathLossService {

    private static final Map<UUID, List<ItemStack>> KEPT_ITEMS = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ItemStack>> PENDING_LOST = new ConcurrentHashMap<>();
    private static final Map<UUID, DeathPlan> PLANS = new ConcurrentHashMap<>();
    private static final Map<UUID, XpSnapshot> KEPT_XP = new ConcurrentHashMap<>();

    private DeathLossService() {}

    public record XpSnapshot(int level, float progress, int total) {}

    private enum DeathPlan {
        KEEP_ALL,
        VANILLA_DROPS,
        CUSTOM_LOST,
        KEEP_INVENTORY_RULE
    }

    /** Morte na arena: devolve inventário e XP, sem perda. */
    public static void keepEverything(ServerPlayer player) {
        KEPT_XP.put(player.getUUID(), new XpSnapshot(
                player.experienceLevel,
                player.experienceProgress,
                player.totalExperience
        ));
        PLANS.put(player.getUUID(), DeathPlan.KEEP_ALL);
    }

    /** Chamado no LivingDeathEvent — inventário ainda intacto. */
    public static void onPlayerDeathStart(ServerPlayer player) {
        int souls = player.getCapability(PlayerSoulsProvider.PLAYER_SOULS)
                .map(s -> s.isInitialized() ? s.getSouls() : SoulsParser.maxSouls)
                .orElse(SoulsParser.maxSouls);

        if (souls > 0) {
            KEPT_XP.put(player.getUUID(), new XpSnapshot(
                    player.experienceLevel,
                    player.experienceProgress,
                    player.totalExperience
            ));
            PLANS.put(player.getUUID(), DeathPlan.KEEP_ALL);
            return;
        }

        applyEmeraldLoss(player);

        SoulsParser.LostItemsMode mode = SoulsParser.lostItems;
        boolean keepInventory = player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);

        if (mode == SoulsParser.LostItemsMode.ALL_ITEMS && keepInventory) {
            PLANS.put(player.getUUID(), DeathPlan.KEEP_INVENTORY_RULE);
            return;
        }

        if (mode == SoulsParser.LostItemsMode.ALL_ITEMS) {
            PLANS.put(player.getUUID(), DeathPlan.VANILLA_DROPS);
            return;
        }

        if (mode == SoulsParser.LostItemsMode.NONE) {
            PLANS.put(player.getUUID(), DeathPlan.KEEP_ALL);
            return;
        }

        // Modos seletivos: separa slots agora e esvazia inventário para vanilla não dropar.
        List<ItemStack> lost = new ArrayList<>();
        List<ItemStack> kept = new ArrayList<>();
        splitInventory(player, mode, lost, kept);
        keepSoulbound(lost, kept);
        clearPlayerInventory(player);
        PENDING_LOST.put(player.getUUID(), lost);
        KEPT_ITEMS.put(player.getUUID(), kept);
        PLANS.put(player.getUUID(), DeathPlan.CUSTOM_LOST);
    }

    public static void onPlayerDrops(ServerPlayer player, LivingDropsEvent event) {
        DeathPlan plan = PLANS.remove(player.getUUID());
        if (plan == null) {
            plan = DeathPlan.VANILLA_DROPS;
        }

        switch (plan) {
            case KEEP_ALL -> {
                List<ItemStack> kept = drainDrops(event);
                KEPT_ITEMS.merge(player.getUUID(), kept, DeathLossService::mergeLists);
            }
            case KEEP_INVENTORY_RULE -> event.getDrops().clear();
            case VANILLA_DROPS -> {
                List<ItemStack> lost = drainDrops(event);
                List<ItemStack> soulbound = new ArrayList<>();
                keepSoulbound(lost, soulbound);
                if (!soulbound.isEmpty()) {
                    KEPT_ITEMS.merge(player.getUUID(), soulbound, DeathLossService::mergeLists);
                }
                deliverLostItems(player, event, lost);
            }
            case CUSTOM_LOST -> {
                // Inventário já foi limpo; limpa drops residuais e entrega PENDING_LOST.
                event.getDrops().clear();
                List<ItemStack> lost = PENDING_LOST.remove(player.getUUID());
                if (lost != null) {
                    List<ItemStack> soulbound = new ArrayList<>();
                    keepSoulbound(lost, soulbound);
                    if (!soulbound.isEmpty()) {
                        KEPT_ITEMS.merge(player.getUUID(), soulbound, DeathLossService::mergeLists);
                    }
                    deliverLostItems(player, event, lost);
                }
            }
        }
    }

    /** Move itens com Rune of Soulbound de {@code lost} para {@code kept}. */
    private static void keepSoulbound(List<ItemStack> lost, List<ItemStack> kept) {
        if (lost == null || lost.isEmpty()) {
            return;
        }
        Iterator<ItemStack> it = lost.iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (ItemRunes.isSoulbound(stack)) {
                kept.add(stack);
                it.remove();
            }
        }
    }

    private static List<ItemStack> mergeLists(List<ItemStack> a, List<ItemStack> b) {
        List<ItemStack> merged = new ArrayList<>(a);
        merged.addAll(b);
        return merged;
    }

    private static void splitInventory(
            Player player,
            SoulsParser.LostItemsMode mode,
            List<ItemStack> lost,
            List<ItemStack> kept
    ) {
        Inventory inv = player.getInventory();

        switch (mode) {
            case RANDOM -> {
                List<SlotRef> nonEmpty = new ArrayList<>();
                collectNonEmpty(inv, nonEmpty);
                if (!nonEmpty.isEmpty()) {
                    SlotRef pick = nonEmpty.get(player.getRandom().nextInt(nonEmpty.size()));
                    lost.add(pick.stack().copy());
                    pick.clear();
                }
                collectAllRemaining(inv, kept);
            }
            case ARMOR -> {
                takeArmor(inv, lost);
                collectAllRemaining(inv, kept);
            }
            case EQUIPMENT -> {
                takeHands(inv, lost);
                collectAllRemaining(inv, kept);
            }
            case ARMOR_EQUIPMENT -> {
                takeArmor(inv, lost);
                takeHands(inv, lost);
                collectAllRemaining(inv, kept);
            }
            case HOTBAR -> {
                for (int i = 0; i < 9; i++) {
                    int slot = i;
                    moveSlot(inv.items.get(i), lost, () -> inv.items.set(slot, ItemStack.EMPTY));
                }
                collectAllRemaining(inv, kept);
            }
            case INVENTORY -> {
                for (int i = 9; i < 36; i++) {
                    int slot = i;
                    moveSlot(inv.items.get(i), lost, () -> inv.items.set(slot, ItemStack.EMPTY));
                }
                collectAllRemaining(inv, kept);
            }
            default -> collectAllRemaining(inv, kept);
        }
    }

    private static void takeArmor(Inventory inv, List<ItemStack> lost) {
        for (int i = 0; i < inv.armor.size(); i++) {
            int slot = i;
            moveSlot(inv.armor.get(i), lost, () -> inv.armor.set(slot, ItemStack.EMPTY));
        }
    }

    private static void takeHands(Inventory inv, List<ItemStack> lost) {
        moveSlot(inv.getItem(inv.selected), lost, () -> inv.setItem(inv.selected, ItemStack.EMPTY));
        moveSlot(inv.offhand.get(0), lost, () -> inv.offhand.set(0, ItemStack.EMPTY));
    }

    private static void moveSlot(ItemStack stack, List<ItemStack> target, Runnable clearer) {
        if (!stack.isEmpty()) {
            target.add(stack.copy());
            clearer.run();
        }
    }

    private static void collectNonEmpty(Inventory inv, List<SlotRef> out) {
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (!stack.isEmpty()) {
                int slot = i;
                out.add(new SlotRef(stack, () -> inv.items.set(slot, ItemStack.EMPTY)));
            }
        }
        for (int i = 0; i < inv.armor.size(); i++) {
            ItemStack stack = inv.armor.get(i);
            if (!stack.isEmpty()) {
                int slot = i;
                out.add(new SlotRef(stack, () -> inv.armor.set(slot, ItemStack.EMPTY)));
            }
        }
        ItemStack off = inv.offhand.get(0);
        if (!off.isEmpty()) {
            out.add(new SlotRef(off, () -> inv.offhand.set(0, ItemStack.EMPTY)));
        }
    }

    private static void collectAllRemaining(Inventory inv, List<ItemStack> kept) {
        for (int i = 0; i < inv.items.size(); i++) {
            int slot = i;
            moveSlot(inv.items.get(i), kept, () -> inv.items.set(slot, ItemStack.EMPTY));
        }
        for (int i = 0; i < inv.armor.size(); i++) {
            int slot = i;
            moveSlot(inv.armor.get(i), kept, () -> inv.armor.set(slot, ItemStack.EMPTY));
        }
        moveSlot(inv.offhand.get(0), kept, () -> inv.offhand.set(0, ItemStack.EMPTY));
    }

    private static void clearPlayerInventory(Player player) {
        player.getInventory().clearContent();
    }

    private record SlotRef(ItemStack stack, Runnable clearer) {
        void clear() {
            clearer.run();
        }
    }

    private static void deliverLostItems(ServerPlayer player, LivingDropsEvent event, List<ItemStack> lost) {
        if (lost == null || lost.isEmpty()) {
            return;
        }
        if (SoulsParser.gravestone) {
            placeGravestone(player, lost);
        } else {
            for (ItemStack stack : lost) {
                if (!stack.isEmpty()) {
                    event.getDrops().add(new ItemEntity(
                            player.level(),
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            stack
                    ));
                }
            }
        }
    }

    private static List<ItemStack> drainDrops(LivingDropsEvent event) {
        List<ItemStack> stacks = new ArrayList<>();
        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity entity = it.next();
            ItemStack stack = entity.getItem();
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
            it.remove();
        }
        return stacks;
    }

    public static void placeGravestone(ServerPlayer player, List<ItemStack> items) {
        if (!(player.level() instanceof ServerLevel level) || items.isEmpty()) {
            return;
        }
        BlockPos placePos = findGravestonePos(level, player.blockPosition());
        Direction facing = player.getDirection().getOpposite();
        BlockState state = ModBlocks.PLAYER_GRAVESTONE.get().defaultBlockState()
                .setValue(PlayerGravestoneBlock.FACING, facing);

        level.setBlock(placePos, state, 3);
        if (level.getBlockEntity(placePos) instanceof PlayerGravestoneBlockEntity grave) {
            grave.setStoredItems(items);
            grave.setOwnerName(player.getGameProfile().getName());
        }
    }

    /**
     * Air nos pés → sobe a partir do primeiro sólido abaixo.
     * Bloco/líquido nos pés → sobe até achar air; se não houver, substitui o bloco da morte.
     */
    private static BlockPos findGravestonePos(ServerLevel level, BlockPos start) {
        BlockState atFeet = level.getBlockState(start);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        if (atFeet.isAir()) {
            BlockPos.MutableBlockPos cursor = start.mutable();
            for (int y = start.getY() - 1; y >= minY; y--) {
                cursor.setY(y);
                if (!level.getBlockState(cursor).isAir()) {
                    BlockPos above = cursor.above();
                    if (above.getY() < maxY) {
                        return above;
                    }
                    return cursor.immutable();
                }
            }
            return start;
        }

        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int y = start.getY() + 1; y < maxY; y++) {
            cursor.setY(y);
            if (level.getBlockState(cursor).isAir()) {
                return cursor.immutable();
            }
        }
        return start;
    }

    public static void applyEmeraldLoss(Player player) {
        player.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(wallet -> {
            long balance = wallet.getBalance();
            if (balance <= 0) {
                return;
            }
            long loss;
            if (SoulsParser.lostEmeraldsType == SoulsParser.LostEmeraldsType.QUANTITY) {
                loss = (long) Math.floor(SoulsParser.lostEmeraldsQuantity);
            } else {
                loss = (long) Math.floor(balance * (SoulsParser.lostEmeraldsQuantity / 100.0));
            }
            loss = Math.min(Math.max(0L, loss), balance);
            if (loss > 0) {
                wallet.setBalance(balance - loss);
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketHandler.sendToPlayer(serverPlayer, new SyncEmeraldWalletPacket(wallet.getBalance()));
                }
            }
        });
    }

    public static List<ItemStack> takeKeptItems(UUID playerId) {
        List<ItemStack> items = KEPT_ITEMS.remove(playerId);
        return items != null ? items : List.of();
    }

    public static XpSnapshot takeKeptXp(UUID playerId) {
        return KEPT_XP.remove(playerId);
    }

    public static void clearPending(UUID playerId) {
        KEPT_ITEMS.remove(playerId);
        PENDING_LOST.remove(playerId);
        PLANS.remove(playerId);
        KEPT_XP.remove(playerId);
    }
}
