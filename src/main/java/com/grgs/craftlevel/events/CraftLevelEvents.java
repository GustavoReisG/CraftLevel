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
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import java.util.function.Supplier;
import java.util.function.Consumer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;

import java.util.Random;

@Mod.EventBusSubscriber(modid = CraftLevel.MODID)
public class CraftLevelEvents {


    private static final Random RANDOM = new Random();

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

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob mob = event.getEntity();

        mob.getCapability(MobLevelProvider.MOB_LEVEL).ifPresent(cap -> {
            int level = calculateMobLevel(mob);
            cap.setLevel(level);
            MobStatScaler.apply(mob, level);
        });
    }

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

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.MAINHAND) return;
        if (!(event.getEntity() instanceof Player player)) return;

        WeaponScaler.apply(player);
    }

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

        if (isEnvironmental) {
            double multiplier = getHealthScalingMultiplier(target);
            if (multiplier > 1.0) {
                event.setAmount((float)(event.getAmount() * (multiplier)));
            }
            return;
        }

        if (!(source.getEntity() instanceof Player player)) return;
        if (source.isIndirect()) return;

        player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).ifPresent(cap -> {
            double bonus = 0;

            if (target.getMobType() == MobType.UNDEAD) {
                bonus = cap.getUndeadBonus();
            } else if (target.getMobType() == MobType.ARTHROPOD) {
                bonus = cap.getArthropodBonus();
            }

            if (bonus > 0) {
                event.setAmount(event.getAmount() + (float) bonus);
            }
        });
    }

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

    @SubscribeEvent
    public static void onProjectileSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();

        if (entity instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity owner) {
            double before = arrow.getBaseDamage();
            applyProjectileScaling(owner, arrow::getBaseDamage, arrow::setBaseDamage);
            CraftLevel.LOGGER.info("[CraftLevel] Projectile {} damage: {} -> {}", entity.getType(), before, arrow.getBaseDamage());
        } else if (entity instanceof ThrownTrident trident && trident.getOwner() instanceof LivingEntity owner) {
            applyProjectileScaling(owner, trident::getBaseDamage, trident::setBaseDamage);
        }
    }

    private static void applyProjectileScaling(LivingEntity owner, Supplier<Double> getDamage, Consumer<Double> setDamage) {
        double multiplier = 1.0;

        if (owner instanceof Player player) {
            var cap = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).resolve();
            if (cap.isEmpty()) return;

            ConfigParser.TierValues tiers = ConfigParser.playerStats.getOrDefault(
                    "minecraft:generic.attack_damage",
                    new ConfigParser.TierValues(1.0, 1.0, 1.0, 1.0, "addition")
            );
            multiplier = ConfigParser.getScaledMultiplier(tiers, cap.get().getLevel());

        } else if (owner instanceof Mob mob) {
            var cap = mob.getCapability(MobLevelProvider.MOB_LEVEL).resolve();
            if (cap.isEmpty()) return;

            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            ConfigParser.TierValues tiers = ConfigParser.getStatForMob(mobId, "minecraft:generic.attack_damage");
            multiplier = ConfigParser.getScaledMultiplier(tiers, cap.get().getLevel());

        } else {
            return; // owner não é player nem mob conhecido, ignora
        }

        setDamage.accept(getDamage.get() * multiplier);
    }
}