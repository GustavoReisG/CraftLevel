package com.grgs.craftlevel.scaling;

import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.ConfigParser;
import com.grgs.craftlevel.config.ConfigParser.StatWiringConfig;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.network.SyncPlayerLevelPacket;
import com.grgs.craftlevel.util.DebugLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Reaplica modifiers de attributes vanilla com base nos escaladores do config.
 * <p>
 * Chamado em login, respawn, level up, troca de equipamento, spawn de mob, reload e comandos admin.
 * Hurt/heal não passam por aqui — são calculados on-demand em {@link com.grgs.craftlevel.damage.EventScalingService}.
 */
public final class StatRefreshService {

    private static final UUID MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String MODIFIER_NAME = "craftlevel_scaling";

    private StatRefreshService() {}

    /** Percorre todos os players online e mobs carregados (usado por /craftlevel reload). */
    public static RefreshResult refreshAllLoaded(MinecraftServer server) {
        int players = 0;
        int mobs = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (refreshAndSyncPlayer(player)) {
                players++;
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob mob && mob.isAlive() && refreshMobFromCap(mob)) {
                    mobs++;
                }
            }
        }

        DebugLog.info("Refresh global: {} player(s), {} mob(s)", players, mobs);
        return new RefreshResult(players, mobs);
    }

    /** Reaplica stats e sincroniza HUD do player. */
    public static boolean refreshAndSyncPlayer(ServerPlayer player) {
        return player.getCapability(PlayerLevelProvider.PLAYER_LEVEL)
                .map(cap -> {
                    refreshPlayer(player, cap.getLevel());
                    PacketHandler.sendToPlayer(player, new SyncPlayerLevelPacket(
                            cap.getLevel(),
                            cap.getLp(),
                            cap.getLpToNextLevel()
                    ));
                    return true;
                })
                .orElse(false);
    }

    public static boolean refreshMobFromCap(Mob mob) {
        return mob.getCapability(MobLevelProvider.MOB_LEVEL)
                .map(cap -> {
                    if (cap.getLevel() <= 0) {
                        return false;
                    }
                    refreshMob(mob, cap.getLevel());
                    return true;
                })
                .orElse(false);
    }

    /** Aplica todos os attributes do wiring da entidade. */
    public static void refresh(LivingEntity entity, int level) {
        refresh(entity, level, true);
    }

    /** Aplica attributes; {@code fullHealOnMaxHealth=false} preserva HP ao mudar o máximo (ex.: equipamento). */
    public static void refresh(LivingEntity entity, int level, boolean fullHealOnMaxHealth) {
        if (level <= 0) {
            return;
        }

        StatWiringConfig wiring = resolveWiring(entity);
        wiring.attribute().forEach((attributeId, scalerName) ->
                applyAttribute(entity, attributeId, scalerName, level, fullHealOnMaxHealth));
    }

    /** True se algum attribute wired usa escalador com {@code include_modifiers}. */
    public static boolean wiringUsesIncludeModifiers(LivingEntity entity) {
        StatWiringConfig wiring = resolveWiring(entity);
        for (String scalerName : wiring.attribute().values()) {
            ScalingDefinition def = ScalingRegistry.get(scalerName);
            if (def != null && def.includeModifiers()) {
                return true;
            }
        }
        return false;
    }

    public static void refreshPlayer(Player player, int level) {
        refresh(player, level, true);
    }

    /** Reaplica stats após troca de equipamento (não cura ao mudar max health). */
    public static void refreshPlayerFromEquipment(Player player, int level) {
        refresh(player, level, false);
    }

    public static void refreshMob(Mob mob, int level) {
        refresh(mob, level);
    }

    /** Player usa player_stats; mob usa override isolado ou mob_stats default. */
    private static StatWiringConfig resolveWiring(LivingEntity entity) {
        if (entity instanceof Player) {
            return ConfigParser.getPlayerWiring();
        }
        if (entity instanceof Mob mob) {
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            return ConfigParser.resolveMobWiring(mobId);
        }
        return StatWiringConfig.empty();
    }

    /**
     * Remove modifier anterior, calcula valor escalado e aplica ADDITIVE no attribute.
     * Max health: ajusta HP atual para o novo máximo após o modifier.
     */
    private static void applyAttribute(
            LivingEntity entity,
            String attributeId,
            String scalerName,
            int level,
            boolean fullHealOnMaxHealth
    ) {
        ScalingDefinition def = ScalingRegistry.require(scalerName, "attribute:" + attributeId);
        if (def == null) {
            return;
        }

        try {
            Attribute attr = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(attributeId));
            if (attr == null) {
                return;
            }

            var instance = entity.getAttribute(attr);
            if (instance == null) {
                return;
            }

            instance.removeModifier(MODIFIER_UUID);

            double scaledValue = ScalingEngine.computeScaledValue(def, level);
            double currentBase = instance.getBaseValue();
            double currentTotal = instance.getValue();
            ScalingEngine.AttributeModifierSpec modifierSpec = ScalingEngine.toAttributeModifier(
                    currentBase,
                    currentTotal,
                    scaledValue,
                    def.applyMode(),
                    def.includeModifiers()
            );

            if (modifierSpec.amount() != 0) {
                instance.addPermanentModifier(new AttributeModifier(
                        MODIFIER_UUID,
                        MODIFIER_NAME,
                        modifierSpec.amount(),
                        modifierSpec.operation()
                ));
            }

            DebugLog.info(
                    "Attribute {} em {}: escalador={} base={} total={} modifier={} op={} final={}",
                    attributeId,
                    describeEntity(entity),
                    scalerName,
                    currentBase,
                    currentTotal,
                    modifierSpec.amount(),
                    modifierSpec.operation(),
                    instance.getValue()
            );

            if (attr == Attributes.MAX_HEALTH) {
                float newMax = (float) instance.getValue();
                if (fullHealOnMaxHealth) {
                    entity.setHealth(newMax);
                } else {
                    entity.setHealth(Math.min(entity.getHealth(), newMax));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String describeEntity(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getGameProfile().getName();
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    public record RefreshResult(int players, int mobs) {}
}
