package com.grgs.craftlevel.events;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerEmeraldBankProvider;
import com.grgs.craftlevel.capability.PlayerEmeraldWalletProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.capability.PlayerSoulsProvider;
import com.grgs.craftlevel.capability.VillagerEconomyProvider;
import com.grgs.craftlevel.config.ConfigParser;
import com.grgs.craftlevel.damage.DamageClassifier;
import com.grgs.craftlevel.damage.EventScalingService;
import com.grgs.craftlevel.economy.EconomyService;
import com.grgs.craftlevel.economy.KillRewardService;
import com.grgs.craftlevel.network.DamageIndicatorPacket;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.PlayLevelUpSoundPacket;
import com.grgs.craftlevel.network.SyncEmeraldBankPacket;
import com.grgs.craftlevel.network.SyncEmeraldWalletPacket;
import com.grgs.craftlevel.network.SyncPlayerLevelPacket;
import com.grgs.craftlevel.scaling.StatRefreshService;
import com.grgs.craftlevel.util.DebugLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Hub de eventos Forge do mod.
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Anexar capabilities de level em players e mobs</li>
 *   <li>Calcular level de mobs no spawn</li>
 *   <li>Progressão de LP ao matar mobs</li>
 *   <li>Scaling de dano/cura nos eventos de combate</li>
 *   <li>Reaplicar stats em login, respawn, clone, troca de equipamento e reload</li>
 *   <li>Enviar indicador visual de dano ao cliente</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public class CraftLevelEvents {

    private static final Random RANDOM = new Random();

    /** Mobs carregados do disco entram na fila para não travar o loading (0%). */
    private static final Queue<Mob> MOB_REFRESH_QUEUE = new ConcurrentLinkedQueue<>();
    private static final int MOB_REFRESH_PER_TICK = 40;
    private static volatile boolean worldReady = false;

    /** Posição do número flutuante entre mob e jogador (0 = no mob, 1 = no player). */
    private static final double INDICATOR_TOWARDS_PLAYER = 0.55;

    /** Anexa {@link PlayerLevelProvider} e {@link MobLevelProvider} quando a entidade é criada. */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation(CraftLevel.MODID, "player_level"),
                    new PlayerLevelProvider()
            );
            event.addCapability(
                    new ResourceLocation(CraftLevel.MODID, "emerald_wallet"),
                    new PlayerEmeraldWalletProvider()
            );
            event.addCapability(
                    new ResourceLocation(CraftLevel.MODID, "emerald_bank"),
                    new PlayerEmeraldBankProvider()
            );
            event.addCapability(
                    new ResourceLocation(CraftLevel.MODID, "player_souls"),
                    new PlayerSoulsProvider()
            );
        }

        if (event.getObject() instanceof AbstractVillager) {
            event.addCapability(
                    new ResourceLocation(CraftLevel.MODID, "villager_economy"),
                    new VillagerEconomyProvider()
            );
        }

        if (event.getObject() instanceof Mob) {
            event.addCapability(
                    new ResourceLocation(CraftLevel.MODID, "mob_level"),
                    new MobLevelProvider()
            );
        }
    }

    /**
     * No spawn natural: calcula level do mob com base no player mais próximo,
     * aplica variação e limites de mob_levels.json, depois reaplica attributes.
     * Summons e spawns de mods que não passam por aqui são cobertos em {@link #onEntityJoinLevel}.
     */
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity().level().isClientSide()) return;
        ensureMobLevel(event.getEntity(), "spawn");
    }

    /**
     * Dispara uma vez por entrada no mundo (spawn, summon, chunk load) — não roda por tick.
     * Inicializa level se a capability ainda for 0 (summon, boss de mod, etc.)
     * ou reaplica stats se o mob já tinha level (chunk reload).
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        if (!worldReady || event.loadedFromDisk()) {
            enqueueMobRefresh(mob);
            return;
        }

        ensureMobLevel(mob, "join");
    }

    private static void enqueueMobRefresh(Mob mob) {
        if (mob instanceof AbstractVillager) {
            return;
        }
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        if (ConfigParser.getMobMinLevel(mobId) == ConfigParser.getMobMaxLevel(mobId)) {
            return;
        }
        MOB_REFRESH_QUEUE.offer(mob);
    }

    /** Reaplica level/stats de mob — usado na interação com villager. */
    public static void refreshMobLevel(Mob mob) {
        ensureMobLevel(mob, "interact");
    }

    public static void markWorldReady() {
        worldReady = true;
    }

    public static boolean isWorldReady() {
        return worldReady;
    }

    public static void markWorldNotReady() {
        worldReady = false;
        MOB_REFRESH_QUEUE.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !worldReady) {
            return;
        }

        int budget = MOB_REFRESH_PER_TICK;
        while (budget-- > 0) {
            Mob mob = MOB_REFRESH_QUEUE.poll();
            if (mob == null) {
                break;
            }
            if (mob.isRemoved() || !mob.isAlive()) {
                continue;
            }
            ensureMobLevel(mob, "chunk");
        }
    }

    /** Calcula level na primeira vez; depois só reaplica stats. */
    private static void ensureMobLevel(Mob mob, String reason) {
        boolean silent = "chunk".equals(reason);
        mob.getCapability(MobLevelProvider.MOB_LEVEL).ifPresent(cap -> {
            if (cap.getLevel() <= 0) {
                int level = calculateMobLevel(mob);
                cap.setLevel(level);
                StatRefreshService.refreshMob(mob, level, silent);
                if (!"chunk".equals(reason)) {
                    DebugLog.info(
                            "Mob {}: {} level {} em {}",
                            reason,
                            BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
                            level,
                            mob.blockPosition()
                    );
                }
                return;
            }

            StatRefreshService.refreshMob(mob, cap.getLevel(), silent);
            if (!"chunk".equals(reason)) {
                DebugLog.info(
                        "Mob {}: {} level {} reaplicado",
                        reason,
                        BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
                        cap.getLevel()
                );
            }
        });
    }

    /**
     * Morte de mob por player: concede LP (baseado na vida máxima do mob),
     * processa level ups encadeados, sincroniza HUD e credita kill rewards na wallet.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        KillRewardService.rewardKill(player, mob);

        player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(playerCap -> {
            int lp = Math.max(1, (int) mob.getMaxHealth());
            int levelBefore = playerCap.getLevel();
            playerCap.addLp(lp);

            DebugLog.info(
                    "LP +{} de {} (total LP: {})",
                    lp,
                    BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
                    playerCap.getLp()
            );

            int levelsGained = 0;
            while (playerCap.canLevelUp()) {
                playerCap.levelUp();
                StatRefreshService.refreshPlayer(player, playerCap.getLevel());
                DebugLog.info("Level up: {} -> {}", levelBefore, playerCap.getLevel());
                levelBefore = playerCap.getLevel();
                levelsGained++;
            }

            if (player instanceof ServerPlayer serverPlayer) {
                PacketHandler.sendToPlayer(serverPlayer, new SyncPlayerLevelPacket(
                        playerCap.getLevel(),
                        playerCap.getLp(),
                        playerCap.getLpToNextLevel()
                ));
                if (levelsGained > 0) {
                    PacketHandler.sendToPlayer(serverPlayer, new PlayLevelUpSoundPacket());
                }
            }
        });
    }

    /** Preserva level/LP ao clonar player (morte, dimensões, etc.). */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();

        original.reviveCaps();

        original.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(oldCap -> {
            clone.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(newCap -> {
                newCap.setLevel(oldCap.getLevel());
                newCap.setLp(oldCap.getLp());
            });
        });

        original.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(oldCap -> {
            clone.getCapability(PlayerEmeraldWalletProvider.EMERALD_WALLET).ifPresent(newCap -> {
                newCap.setBalance(oldCap.getBalance());
                newCap.setInitialized(oldCap.isInitialized());
            });
        });

        original.getCapability(PlayerEmeraldBankProvider.EMERALD_BANK).ifPresent(oldCap -> {
            clone.getCapability(PlayerEmeraldBankProvider.EMERALD_BANK).ifPresent(newCap -> {
                newCap.setBalance(oldCap.getBalance());
                newCap.setInitialized(oldCap.isInitialized());
            });
        });

        original.getCapability(PlayerSoulsProvider.PLAYER_SOULS).ifPresent(oldCap -> {
            clone.getCapability(PlayerSoulsProvider.PLAYER_SOULS).ifPresent(newCap -> {
                newCap.setSouls(oldCap.getSouls());
                newCap.setRestoreProgressTicks(oldCap.getRestoreProgressTicks());
                newCap.setInitialized(oldCap.isInitialized());
            });
        });

        original.invalidateCaps();
        applyPlayerState(clone);
        DebugLog.info("Player clone: level {} LP {}", clone.getName().getString(), clone.getCapability(PlayerLevelProvider.PLAYER_LEVEL).map(c -> c.getLp()).orElse(-1));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        applyPlayerState(event.getEntity());
        DebugLog.info("Player respawn: {}", event.getEntity().getName().getString());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        markWorldReady();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        EconomyService.ensurePlayerEconomy(serverPlayer);
        applyPlayerState(serverPlayer);
        PacketHandler.sendToPlayer(serverPlayer, new SyncEmeraldWalletPacket(EconomyService.getWalletBalance(serverPlayer)));
        PacketHandler.sendToPlayer(serverPlayer, new SyncEmeraldBankPacket(EconomyService.getBankBalance(serverPlayer)));
        DebugLog.info("Player login: {}", serverPlayer.getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        markWorldNotReady();
    }

    /**
     * Reaplica attributes quando mão ou armadura mudam, se algum escalador wired usa
     * {@code include_modifiers}.
     */
    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isEquipmentRefreshSlot(event.getSlot())) return;
        if (!StatRefreshService.wiringUsesIncludeModifiers(player)) return;

        player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(cap -> {
            if (cap.getLevel() <= 0) {
                return;
            }
            StatRefreshService.refreshPlayerFromEquipment(player, cap.getLevel());
            DebugLog.info(
                    "Equipment change: {} slot {} - attributes refreshed",
                    player.getName().getString(),
                    event.getSlot().getName()
            );
        });
    }

    private static boolean isEquipmentRefreshSlot(EquipmentSlot slot) {
        return slot.getType() == EquipmentSlot.Type.HAND
                || slot.getType() == EquipmentSlot.Type.ARMOR;
    }

    /**
     * Intercepta dano causado por entidades vivas: resolve damage type e aplica scaling.
     * <ul>
     *   <li>{@code hurt_self} explícito (tipo ou wildcard) → level do alvo</li>
     *   <li>{@code hurt_self.default} → apenas sem atacante (ambiental)</li>
     *   <li>{@code hurt} → atacante externo, level do atacante</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        LivingEntity attacker = resolveAttacker(source);
        String damageTypeId = DamageClassifier.getDamageTypeId(source);
        float originalAmount = event.getAmount();
        float scaled;

        if (hasExplicitHurtSelfMatch(target, damageTypeId)) {
            scaled = EventScalingService.applyHurtSelfExplicitScaling(target, damageTypeId, originalAmount);
            logHurtSelfDebug(source, target, damageTypeId, originalAmount, scaled);
        } else if (attacker == null) {
            scaled = EventScalingService.applyHurtSelfDefaultScaling(target, originalAmount);
            logHurtSelfDebug(source, target, damageTypeId, originalAmount, scaled);
        } else if (attacker != target) {
            scaled = EventScalingService.applyHurtScaling(attacker, damageTypeId, originalAmount);
            logHurtDebug(source, target, attacker, damageTypeId, originalAmount, scaled);
        } else {
            scaled = originalAmount;
            logHurtSelfDebug(source, target, damageTypeId, originalAmount, scaled);
        }

        event.setAmount(scaled);

        Player attackingPlayer = resolveAttackingPlayer(source);
        if (attackingPlayer != null && event.getAmount() > 0) {
            sendDamageIndicator(attackingPlayer, target, event.getAmount());
        }
    }

    /** Escala cura recebida com base no wiring heal do config. */
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity entity = event.getEntity();
        float original = event.getAmount();
        float scaled = EventScalingService.applyHealScaling(entity, ConfigParser.DEFAULT_KEY, original);
        event.setAmount(scaled);

        DebugLog.info(
                "Heal: entidade={} original={} final={}",
                describeEntity(entity),
                original,
                scaled
        );
    }

    /**
     * Level base = player mais próximo (128 blocos).
     * Aplica variação aleatória e clamp por mob_levels.json.
     */
    private static int calculateMobLevel(Mob mob) {
        Player nearest = mob.level().getNearestPlayer(mob, 128);
        int baseLevel = 1;

        if (nearest != null) {
            var cap = nearest.getCapability(PlayerLevelProvider.PLAYER_LEVEL).resolve();
            if (cap.isPresent()) {
                baseLevel = cap.get().getLevel();
            }
        }

        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        int minLevel = ConfigParser.getMobMinLevel(mobId);
        int maxLevel = ConfigParser.getMobMaxLevel(mobId);

        int variation = RANDOM.nextInt(ConfigParser.mobLevelVariation * 2 + 1) - ConfigParser.mobLevelVariation;
        int level = baseLevel + variation;

        return Math.max(minLevel, Math.min(maxLevel, level));
    }

    /** Reaplica modifiers e envia pacote de sync ao cliente. */
    private static void applyPlayerState(Player player) {
        player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(cap -> {
            StatRefreshService.refreshPlayer(player, cap.getLevel());

            if (player instanceof ServerPlayer serverPlayer) {
                PacketHandler.sendToPlayer(serverPlayer, new SyncPlayerLevelPacket(
                        cap.getLevel(),
                        cap.getLp(),
                        cap.getLpToNextLevel()
                ));
            }

            DebugLog.info(
                    "Player state: {} level {} LP {}/{}",
                    player.getName().getString(),
                    cap.getLevel(),
                    cap.getLp(),
                    cap.getLpToNextLevel()
            );
        });
    }

    private static boolean hasExplicitHurtSelfMatch(LivingEntity target, String damageTypeId) {
        if (target instanceof Player) {
            return ConfigParser.hasPlayerExplicitHurtSelfMatch(damageTypeId);
        }
        if (target instanceof Mob mob) {
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            return ConfigParser.hasMobExplicitHurtSelfMatch(mobId, damageTypeId);
        }
        return false;
    }

    /** Projétil usa o owner; melee usa a entidade causadora. */
    private static LivingEntity resolveAttacker(DamageSource source) {
        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        if (source.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static Player resolveAttackingPlayer(DamageSource source) {
        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof Player player) {
            return player;
        }
        if (source.getEntity() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static void sendDamageIndicator(Player player, LivingEntity target, float damage) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        double[] pos = indicatorPosition(player, target);
        PacketHandler.sendToPlayer(serverPlayer, new DamageIndicatorPacket(
                pos[0], pos[1], pos[2], damage
        ));
    }

    private static double[] indicatorPosition(Player player, LivingEntity target) {
        double targetY = target.getY() + target.getBbHeight() * 0.65;
        double playerY = player.getY() + player.getEyeHeight() * 0.75;

        double dx = player.getX() - target.getX();
        double dy = playerY - targetY;
        double dz = player.getZ() - target.getZ();

        double t = INDICATOR_TOWARDS_PLAYER;
        return new double[]{
                target.getX() + dx * t,
                targetY + dy * t,
                target.getZ() + dz * t
        };
    }

    private static void logHurtSelfDebug(
            DamageSource source,
            LivingEntity target,
            String damageTypeId,
            float originalAmount,
            float finalAmount
    ) {
        Entity causer = source.getEntity();
        String causerLabel = causer != null ? describeEntity(causer) : "ambiental";

        DebugLog.info(
                "Hurt self: tipo={} causador={} alvo={} dano={} -> {}",
                damageTypeId,
                causerLabel,
                describeEntity(target),
                originalAmount,
                finalAmount
        );
    }

    private static void logHurtDebug(
            DamageSource source,
            LivingEntity target,
            LivingEntity attacker,
            String damageTypeId,
            float originalAmount,
            float finalAmount
    ) {
        Entity causer = attacker != null ? attacker : source.getEntity();
        String causerLabel = causer != null ? describeEntity(causer) : "nenhuma";

        DebugLog.info(
                "Hurt: tipo={} causador={} alvo={} dano={} -> {}",
                damageTypeId,
                causerLabel,
                describeEntity(target),
                originalAmount,
                finalAmount
        );
    }

    private static String describeEntity(Entity entity) {
        if (entity instanceof Player player) {
            return player.getGameProfile().getName();
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }
}
