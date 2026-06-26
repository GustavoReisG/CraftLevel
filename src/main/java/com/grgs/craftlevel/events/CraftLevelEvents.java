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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Eventos centrais do mod: capabilities, progressão, spawn de mobs, combate e sincronização.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public class CraftLevelEvents {

    private static final Random RANDOM = new Random();

    /** Fração do caminho alvo→jogador onde o número aparece (0 = centro do mob, 1 = jogador). */
    private static final double INDICATOR_TOWARDS_PLAYER = 0.55;

    /** Anexa capabilities de level em jogadores e mobs. */
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

    /** Define level do mob no spawn e aplica scaling de stats. */
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob mob = event.getEntity();

        mob.getCapability(MobLevelProvider.MOB_LEVEL).ifPresent(cap -> {
            int level = calculateMobLevel(mob);
            cap.setLevel(level);
            MobStatScaler.apply(mob, level);
        });
    }

    /** Jogador ganha LP ao matar mob; sobe de level e sincroniza com o cliente. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(playerCap -> {
            int lp = Math.max(1, (int) mob.getMaxHealth());
            playerCap.addLp(lp);

            while (playerCap.canLevelUp()) {
                playerCap.levelUp();
                PlayerStatScaler.apply(
                        player,
                        playerCap.getLevel()
                );
            }

            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                PacketHandler.sendToPlayer(serverPlayer, new SyncPlayerLevelPacket(
                        playerCap.getLevel(),
                        playerCap.getLp(),
                        playerCap.getLpToNextLevel()
                ));
            }
        });
    }

    /** Copia level/LP do jogador original ao morrer (keep inventory / clone). */
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
    }

    /** Reaplica stats e sincroniza HUD após respawn. */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        applyPlayerState(player);
    }

    /** Reaplica stats e envia dados iniciais ao entrar no servidor. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;
        applyPlayerState(serverPlayer);
    }

    /** Aplica scaling de hurt só para dano causado por uma entidade (level do atacante). */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        LivingEntity attacker = resolveAttacker(source);

        if (attacker != null) {
            String damageTypeId = DamageClassifier.getDamageTypeId(source);
            double multiplier = EventScalingService.getHurtMultiplier(attacker, damageTypeId);
            if (multiplier != 1.0) {
                event.setAmount((float) (event.getAmount() * multiplier));
            }
        }

        if (ConfigParser.debug) {
            sendHurtDebugMessage(source, target, attacker, event.getAmount());
        }

        Player attackingPlayer = resolveAttackingPlayer(source);
        if (attackingPlayer != null && event.getAmount() > 0) {
            sendDamageIndicator(attackingPlayer, target, event.getAmount());
        }
    }

    /** Aplica scaling de heal com base em heal.default (ou chave específica). */
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity entity = event.getEntity();
        double multiplier = EventScalingService.getHealMultiplier(entity, ConfigParser.DEFAULT_KEY);
        if (multiplier != 1.0) {
            event.setAmount((float) (event.getAmount() * multiplier));
        }

        if (ConfigParser.debug) {
            sendHealDebugMessage(entity, event.getAmount());
        }
    }

    /** Level do mob = level do jogador mais próximo ± variação, clamped por mob_levels.json. */
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

    private static void applyPlayerState(Player player) {
        player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(cap -> {
            PlayerStatScaler.apply(player, cap.getLevel());

            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                PacketHandler.sendToPlayer(serverPlayer, new SyncPlayerLevelPacket(
                        cap.getLevel(),
                        cap.getLp(),
                        cap.getLpToNextLevel()
                ));
            }
        });
    }

    /** Resolve o atacante a partir da fonte de dano (melee, projétil com owner ou entidade causadora). */
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

    /** Só jogadores — usado para indicador visual de dano. */
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
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;

        double[] pos = indicatorPosition(player, target);
        PacketHandler.sendToPlayer(serverPlayer, new DamageIndicatorPacket(
                pos[0], pos[1], pos[2], damage
        ));
    }

    /** Posição entre o alvo e o jogador, mais próxima do lado que encara o player. */
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

    private static void sendHealDebugMessage(LivingEntity entity, float amount) {
        String message = String.format(
                "[CraftLevel] Cura | Entidade: %s | Cura: %.1f",
                describeEntity(entity),
                amount
        );

        Component component = Component.literal(message);
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(component);
        }
    }

    private static void sendHurtDebugMessage(
            DamageSource source,
            LivingEntity target,
            LivingEntity attacker,
            float amount
    ) {
        String damageType = DamageClassifier.getDamageTypeId(source);
        Entity causer = attacker != null ? attacker : source.getEntity();
        String causerLabel = causer != null ? describeEntity(causer) : "nenhuma";
        String message = String.format(
                "[CraftLevel] Tipo: %s | Causador: %s | Alvo: %s | Dano: %.1f",
                damageType,
                causerLabel,
                describeEntity(target),
                amount
        );

        Component component = Component.literal(message);
        if (causer instanceof ServerPlayer causerPlayer) {
            causerPlayer.sendSystemMessage(component);
            return;
        }
        if (target instanceof ServerPlayer targetPlayer) {
            targetPlayer.sendSystemMessage(component);
        }
    }

    private static String describeEntity(Entity entity) {
        if (entity instanceof Player player) {
            return player.getGameProfile().getName();
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }
}
