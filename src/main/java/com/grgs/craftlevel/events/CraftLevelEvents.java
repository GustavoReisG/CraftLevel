package com.grgs.craftlevel.events;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.ConfigParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncPlayerLevelPacket;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import com.grgs.craftlevel.network.DamageIndicatorPacket;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Map;
import java.util.Random;

/**
 * Eventos centrais do mod: capabilities, progressão, spawn de mobs, combate e sincronização.
 */
@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public class CraftLevelEvents {

    private static final Random RANDOM = new Random();

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

    /** Reaplica stats e envia dados iniciais ao entrar no servidor. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;

        serverPlayer.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(cap -> {
            PlayerStatScaler.apply(serverPlayer, cap.getLevel());
            PacketHandler.sendToPlayer(serverPlayer, new SyncPlayerLevelPacket(
                    cap.getLevel(),
                    cap.getLp(),
                    cap.getLpToNextLevel()
            ));
        });
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

    /** Recalcula bônus de dano da arma ao trocar item na mão principal. */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.MAINHAND) return;
        if (!(event.getEntity() instanceof Player player)) return;

        WeaponScaler.apply(player);
    }

    /**
     * Intercepta dano para aplicar scaling de combate, encantamentos condicionais
     * e enviar indicadores visuais de dano ao cliente.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();

        boolean isEnvironmental = source.is(DamageTypes.FALL)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION);

        // Dano ambiental escala com a vida máxima do alvo (level do mob/jogador)
        if (isEnvironmental) {
            double multiplier = getHealthScalingMultiplier(target);
            if (multiplier > 1.0) {
                event.setAmount((float)(event.getAmount() * multiplier));
            }
            return;
        }

        // Identifica quem causou o dano — direto ou via projétil — player OU mob
        LivingEntity attacker = null;
        boolean isProjectile = false;

        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity living) {
            attacker = living;
            isProjectile = true;
        } else if (source.getEntity() instanceof LivingEntity living) {
            attacker = living;
        }

        if (attacker == null) return;

        final LivingEntity finalAttacker = attacker;

        // Projéteis herdam o multiplicador de dano do atirador
        if (isProjectile) {
            double multiplier = getAttackDamageMultiplier(finalAttacker);
            if (multiplier > 1.0) {
                event.setAmount((float)(event.getAmount() * multiplier));
            }
        }

        if (finalAttacker instanceof Player player) {
            applyConditionalEnchantDamage(player, target, event);
        }

        // Indicator de dano — só pra ataques de PLAYER (mobs atacando não precisam de indicator)
        if (finalAttacker instanceof Player player && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            boolean isCrit = !isProjectile
                    && player.fallDistance > 0
                    && !player.onGround()
                    && !player.onClimbable();

            serverLevel.players().forEach(serverPlayer -> {
                PacketHandler.sendToPlayer(serverPlayer, new DamageIndicatorPacket(
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        event.getAmount(), isCrit
                ));
            });
        }
    }

    /** Adiciona dano de Smite/Bane quando o alvo corresponde ao tipo configurado. */
    private static void applyConditionalEnchantDamage(Player player, LivingEntity target, LivingHurtEvent event) {
        player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(cap -> {
            for (Map.Entry<String, Double> entry : cap.getConditionalEnchantDamage().entrySet()) {
                String entityType = entry.getKey();
                double damage = entry.getValue();

                if (matchesEntityType(target, entityType)) {
                    event.setAmount(event.getAmount() + (float) damage);
                }
            }
        });
    }

    /** Mapeia strings de enchantments.json para MobType do Minecraft. */
    private static boolean matchesEntityType(LivingEntity target, String entityType) {
        return switch (entityType.toLowerCase()) {
            case "undead" -> target.getMobType() == MobType.UNDEAD;
            case "arthropod" -> target.getMobType() == MobType.ARTHROPOD;
            case "illager" -> target.getMobType() == MobType.ILLAGER;
            default -> false;
        };
    }

    /** Multiplicador de attack_damage do atacante (jogador ou mob). */
    private static double getAttackDamageMultiplier(LivingEntity attacker) {
        if (attacker instanceof Player player) {
            var cap = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).resolve();
            if (cap.isEmpty()) return 1.0;

            ConfigParser.TierValues tiers = ConfigParser.playerStats.getOrDefault(
                    "minecraft:generic.attack_damage",
                    new ConfigParser.TierValues(1.0, 1.0, 1.0, 1.0, "addition")
            );
            return ConfigParser.getScaledMultiplier(tiers, cap.get().getLevel());

        } else if (attacker instanceof Mob mob) {
            var cap = mob.getCapability(MobLevelProvider.MOB_LEVEL).resolve();
            if (cap.isEmpty()) return 1.0;

            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            ConfigParser.TierValues tiers = ConfigParser.getStatForMob(mobId, "minecraft:generic.attack_damage");
            return ConfigParser.getScaledMultiplier(tiers, cap.get().getLevel());
        }

        return 1.0;
    }

    /** Multiplicador de max_health do alvo — usado para dano ambiental escalar com level. */
    private static double getHealthScalingMultiplier(LivingEntity target) {
        ConfigParser.TierValues healthTiers;
        int level;

        if (target instanceof Player player) {
            var cap = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).resolve();
            if (cap.isEmpty()) return 1.0;
            level = cap.get().getLevel();
            healthTiers = ConfigParser.playerStats.getOrDefault(
                    "minecraft:generic.max_health",
                    new ConfigParser.TierValues(1.0, 1.0, 1.0, 1.0, "addition")
            );
        } else if (target instanceof Mob mob) {
            var cap = mob.getCapability(MobLevelProvider.MOB_LEVEL).resolve();
            if (cap.isEmpty()) return 1.0;
            level = cap.get().getLevel();

            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            healthTiers = ConfigParser.getStatForMob(mobId, "minecraft:generic.max_health");
        } else {
            return 1.0;
        }

        return ConfigParser.getScaledMultiplier(healthTiers, level);
    }
}
