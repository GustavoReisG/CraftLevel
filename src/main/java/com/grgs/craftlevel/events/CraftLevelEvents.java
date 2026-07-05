package com.grgs.craftlevel.events;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.ConfigParser;
import com.grgs.craftlevel.damage.DamageClassifier;
import com.grgs.craftlevel.damage.EventScalingService;
import com.grgs.craftlevel.network.DamageIndicatorPacket;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncPlayerLevelPacket;
import com.grgs.craftlevel.scaling.StatRefreshService;
import com.grgs.craftlevel.util.DebugLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Hub de eventos Forge do mod.
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Anexar capabilities de level em players e mobs</li>
 *   <li>Calcular level de mobs no spawn</li>
 *   <li>Progressão de LP ao matar mobs</li>
 *   <li>Scaling de dano/cura nos eventos de combate</li>
 *   <li>Reaplicar stats em login, respawn, clone e reload</li>
 *   <li>Enviar indicador visual de dano ao cliente</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public class CraftLevelEvents {

    private static final Random RANDOM = new Random();

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
     */
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity().level().isClientSide()) return;

        Mob mob = event.getEntity();
        mob.getCapability(MobLevelProvider.MOB_LEVEL).ifPresent(cap -> {
            int level = calculateMobLevel(mob);
            cap.setLevel(level);
            StatRefreshService.refreshMob(mob, level);
            DebugLog.info(
                    "Mob spawn: {} level {} em {}",
                    BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
                    level,
                    mob.blockPosition()
            );
        });
    }

    /** Mobs carregados de chunk recebem refresh de stats se já tiverem level definido. */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        mob.getCapability(MobLevelProvider.MOB_LEVEL).ifPresent(cap -> {
            if (cap.getLevel() > 0) {
                StatRefreshService.refreshMob(mob, cap.getLevel());
                DebugLog.info(
                        "Mob join: {} level {} reaplicado",
                        BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
                        cap.getLevel()
                );
            }
        });
    }

    /**
     * Morte de mob por player: concede LP (baseado na vida máxima do mob),
     * processa level ups encadeados e sincroniza HUD.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

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

            while (playerCap.canLevelUp()) {
                playerCap.levelUp();
                StatRefreshService.refreshPlayer(player, playerCap.getLevel());
                DebugLog.info("Level up: {} -> {}", levelBefore, playerCap.getLevel());
                levelBefore = playerCap.getLevel();
            }

            if (player instanceof ServerPlayer serverPlayer) {
                PacketHandler.sendToPlayer(serverPlayer, new SyncPlayerLevelPacket(
                        playerCap.getLevel(),
                        playerCap.getLp(),
                        playerCap.getLpToNextLevel()
                ));
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
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        applyPlayerState(serverPlayer);
        DebugLog.info("Player login: {}", serverPlayer.getName().getString());
    }

    /**
     * Intercepta dano causado por entidades vivas: resolve damage type,
     * aplica escalador configurado em hurt e envia indicador numérico ao atacante.
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

        if (attacker == null || attacker == target) {
            scaled = EventScalingService.applyHurtSelfScaling(target, damageTypeId, originalAmount);
            logHurtSelfDebug(source, target, damageTypeId, originalAmount, scaled);
        } else {
            scaled = EventScalingService.applyHurtScaling(attacker, damageTypeId, originalAmount);
            logHurtDebug(source, target, attacker, damageTypeId, originalAmount, scaled);
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
