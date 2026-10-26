package com.grgs.craftlevel.arena;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;

/** Trava a arena e manda o jogador de volta ao spawn do Overworld quando ele morre. */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public final class ArenaEvents {

    private ArenaEvents() {}

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!ArenaService.locks(event.getEntity())) {
            return;
        }
        event.setUseBlock(Event.Result.DENY);
        if (!keptItem(event.getItemStack())) {
            event.setUseItem(Event.Result.DENY);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!ArenaService.locks(event.getEntity())) {
            return;
        }
        Item item = event.getItemStack().getItem();
        if (item instanceof BlockItem || item instanceof BucketItem || item instanceof SolidBucketItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (ArenaService.locks(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (ArenaService.locks(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (ArenaService.locks(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (ArenaService.locks(event.getLevel())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (ArenaService.locks(event.getLevel())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMobGrief(EntityMobGriefingEvent event) {
        if (ArenaService.locks(event.getEntity().level())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!ArenaService.locks(event.getLevel())) {
            return;
        }
        event.getAffectedBlocks().clear();
        if (event.getExplosion().getDirectSourceEntity() instanceof Creeper) {
            event.getAffectedEntities().removeIf(entity -> !(entity instanceof Player));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (!ArenaService.locks(level)) {
            return;
        }
        ArenaWaves.tick(level);
        ArenaWaves.focusPlayers(level);
        if (level.getGameTime() % 40 != 0) {
            return;
        }
        if (level.isRaining() || level.isThundering()) {
            level.setWeatherParameters(6000, 0, false, false);
        }
    }

    /** Comida, poção, arco, besta, escudo e tridente continuam usáveis. */
    private static boolean keptItem(ItemStack stack) {
        Item item = stack.getItem();
        return stack.isEdible()
                || item instanceof PotionItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof ShieldItem
                || item instanceof TridentItem;
    }

    @SubscribeEvent
    public static void onMobAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player || !ArenaService.locks(event.getEntity().level())) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && !(attacker instanceof Player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !ArenaService.locks(mob.level())) {
            return;
        }
        if (event.getNewTarget() instanceof Player) {
            return;
        }
        ServerPlayer player = ArenaWaves.focusOf(mob);
        if (player != null) {
            event.setNewTarget(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player || !ArenaService.locks(event.getEntity().level())) {
            return;
        }
        event.getDrops().clear();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExperience(LivingExperienceDropEvent event) {
        if (!ArenaService.locks(event.getEntity().level())) {
            return;
        }
        event.setDroppedExperience(0);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            ArenaService.markExit(player);
            return;
        }
        if (event.getEntity().level() instanceof ServerLevel level) {
            ArenaWaves.mobDied(level, event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> ArenaService.finishExit(player));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArenaService.recover(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArenaService.disconnect(player);
        }
    }
}
