package com.grgs.craftlevel.arena;

import com.grgs.craftlevel.CraftLevel;
import com.grgs.craftlevel.capability.MobLevelProvider;
import com.grgs.craftlevel.capability.PlayerLevelProvider;
import com.grgs.craftlevel.config.GloryKillsParser;
import com.grgs.craftlevel.network.PacketHandler;
import com.grgs.craftlevel.scaling.StatRefreshService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.loading.FMLPaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Spawna as levas da horda nos quatro cantos do quadrado de 21 blocos. */
final class ArenaWaves {

    private static final int SPAWN_Y = 64;
    private static final int OFFSET = 10;
    private static final int COUNTDOWN_TICKS = 100;
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<UUID, Integer> SHOWN = new HashMap<>();
    private static final int[][] CORNERS = {{OFFSET, OFFSET}, {OFFSET, -OFFSET}, {-OFFSET, OFFSET}, {-OFFSET, -OFFSET}};

    private ArenaWaves() {}

    static void begin(ServerLevel arena, ServerPlayer player, ArenaService.Session session, ArenaSlots.Slot point) {
        session.centerX = point.x();
        session.centerZ = point.z();
        session.waves = choose(player, session);
        session.waveIndex = 0;
        if (session.waves.isEmpty()) {
            CraftLevel.LOGGER.warn("[CraftLevel] Arena sem horda para {}", player.getScoreboardName());
            session.nextWaveAt = -1L;
            return;
        }
        armCountdown(arena, player.getUUID(), session);
    }

    static void tick(ServerLevel arena) {
        long time = arena.getGameTime();
        List<UUID> finished = new ArrayList<>();
        for (var entry : ArenaService.data(arena.getServer()).sessions().entrySet()) {
            ArenaService.Session session = entry.getValue();
            if (session.victorious) {
                if (session.nextWaveAt >= 0L && time >= session.nextWaveAt) {
                    finished.add(entry.getKey());
                } else {
                    showVictory(arena, entry.getKey());
                }
                continue;
            }
            if (session.nextWaveAt >= 0L) {
                long left = session.nextWaveAt - time;
                if (left > 0L) {
                    showCountdown(arena, entry.getKey(), left);
                    continue;
                }
                session.nextWaveAt = -1L;
                clearTitle(arena, entry.getKey());
                if (spawnOrFinish(arena, entry.getKey(), session)) {
                    finished.add(entry.getKey());
                } else {
                    refreshBar(arena, entry.getKey(), session);
                }
                continue;
            }
            if (!session.fighting) {
                continue;
            }
            refreshBar(arena, entry.getKey(), session);
            if (session.waveMobs.isEmpty()) {
                session.fighting = false;
                if (advance(arena, entry.getKey(), session)) {
                    finished.add(entry.getKey());
                }
            }
        }
        for (UUID id : finished) {
            ServerPlayer player = arena.getServer().getPlayerList().getPlayer(id);
            if (player != null) {
                ArenaService.leaveToEntry(player);
            }
        }
    }

    static void mobDied(ServerLevel level, UUID mobId) {
        for (var entry : ArenaService.data(level.getServer()).sessions().entrySet()) {
            ArenaService.Session session = entry.getValue();
            if (!session.waveMobs.remove(mobId)) {
                continue;
            }
            if (session.waveMobs.isEmpty() && session.nextWaveAt < 0L && !session.victorious) {
                session.fighting = false;
                advance(level, entry.getKey(), session);
            } else if (!session.victorious) {
                refreshBar(level, entry.getKey(), session);
            }
            break;
        }
    }

    /** Mobs da arena, inclusive os invocados, miram o jogador daquela cópia. */
    static void focusPlayers(ServerLevel arena) {
        for (var entry : ArenaService.data(arena.getServer()).sessions().entrySet()) {
            ServerPlayer player = arena.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.level() != arena) {
                continue;
            }
            ArenaSchematic.Bounds bounds = entry.getValue().bounds;
            AABB box = new AABB(
                    bounds.minX() - 16.0D, bounds.minY() - 8.0D, bounds.minZ() - 16.0D,
                    bounds.maxX() + 17.0D, bounds.maxY() + 16.0D, bounds.maxZ() + 17.0D
            );
            for (Mob mob : arena.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
                if (mob.getTarget() != player) {
                    mob.setTarget(player);
                }
            }
        }
    }

    static ServerPlayer focusOf(Mob mob) {
        if (!(mob.level() instanceof ServerLevel arena)) {
            return null;
        }
        for (var entry : ArenaService.data(arena.getServer()).sessions().entrySet()) {
            ArenaSchematic.Bounds bounds = entry.getValue().bounds;
            if (mob.getX() < bounds.minX() - 16.0D || mob.getX() > bounds.maxX() + 16.0D
                    || mob.getZ() < bounds.minZ() - 16.0D || mob.getZ() > bounds.maxZ() + 16.0D) {
                continue;
            }
            ServerPlayer player = arena.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.level() == arena) {
                return player;
            }
        }
        return null;
    }

    static void dismiss(ServerPlayer player) {
        SHOWN.remove(player.getUUID());
        ServerBossEvent bar = BARS.remove(player.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
        player.connection.send(new ClientboundClearTitlesPacket(false));
    }

    private static boolean advance(ServerLevel arena, UUID playerId, ArenaService.Session session) {
        if (session.victorious) {
            return false;
        }
        session.waveIndex++;
        if (session.waveIndex >= session.waves.size()) {
            session.victorious = true;
            session.fighting = false;
            session.nextWaveAt = arena.getGameTime() + COUNTDOWN_TICKS;
            clearTitle(arena, playerId);
            showVictory(arena, playerId);
            return false;
        }
        armCountdown(arena, playerId, session);
        return false;
    }

    private static void showVictory(ServerLevel arena, UUID playerId) {
        ServerPlayer player = arena.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        ServerBossEvent bar = bar(player);
        bar.setName(Component.translatable("event.minecraft.raid.victory"));
        bar.setProgress(0.0F);
    }

    private static void armCountdown(ServerLevel arena, UUID playerId, ArenaService.Session session) {
        SHOWN.remove(playerId);
        session.fighting = false;
        session.nextWaveAt = arena.getGameTime() + COUNTDOWN_TICKS;
    }

    private static void clearTitle(ServerLevel arena, UUID playerId) {
        SHOWN.remove(playerId);
        ServerPlayer player = arena.getServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            player.connection.send(new ClientboundClearTitlesPacket(false));
        }
    }

    /** Igual à raid: o preenchimento é a vida somada dos mobs ainda vivos. */
    private static void refreshBar(ServerLevel arena, UUID playerId, ArenaService.Session session) {
        float health = 0.0F;
        int alive = 0;
        var it = session.waveMobs.iterator();
        while (it.hasNext()) {
            Entity entity = arena.getEntity(it.next());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                it.remove();
                continue;
            }
            health += living.getHealth();
            alive++;
        }
        if (alive == 0) {
            return;
        }
        ServerPlayer player = arena.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        ServerBossEvent bar = bar(player);
        bar.setName(Component.translatable("event.minecraft.raid.raiders_remaining", alive));
        float total = session.waveHealth;
        bar.setProgress(total <= 0.0F ? 0.0F : Mth.clamp(health / total, 0.0F, 1.0F));
    }

    private static ServerBossEvent bar(ServerPlayer player) {
        ServerBossEvent bar = BARS.get(player.getUUID());
        if (bar != null) {
            return bar;
        }
        bar = new ServerBossEvent(
                Component.translatable("event.minecraft.raid"),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.NOTCHED_10
        );
        bar.addPlayer(player);
        BARS.put(player.getUUID(), bar);
        return bar;
    }

    private static void showCountdown(ServerLevel arena, UUID playerId, long ticksLeft) {
        int second = (int) Math.min(5, (ticksLeft + 19) / 20);
        Integer shown = SHOWN.get(playerId);
        if (shown != null && shown == second) {
            return;
        }
        SHOWN.put(playerId, second);
        ServerPlayer player = arena.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        ServerBossEvent bar = bar(player);
        bar.setName(Component.translatable("event.minecraft.raid").append(" " + second));
        bar.setProgress(second / 5.0F);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 0));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(Integer.toString(second))));
    }

    static ResourceKey<Biome> biome(ServerLevel level, ArenaCatalog.Entry template) {
        String raw = "";
        try {
            ArenaSettings settings = ArenaSettings.load(config(), List.of());
            String file = fileName(template.resource());
            for (ArenaSettings.Schematic schematic : settings.schematics()) {
                if (file.equals(schematic.file)) {
                    raw = schematic.biome;
                    break;
                }
            }
        } catch (Exception ignored) {
            raw = "";
        }
        if (raw == null || raw.isBlank()) {
            return template.biome();
        }
        ResourceLocation id = location(raw);
        if (id != null && level.registryAccess().registryOrThrow(Registries.BIOME).containsKey(id)) {
            return ResourceKey.create(Registries.BIOME, id);
        }
        return Biomes.PLAINS;
    }

    /** @return true quando não resta nenhuma leva */
    private static boolean spawnOrFinish(ServerLevel arena, UUID owner, ArenaService.Session session) {
        while (session.waveIndex < session.waves.size()) {
            spawnWave(arena, owner, session);
            if (!session.waveMobs.isEmpty()) {
                return false;
            }
            session.waveIndex++;
        }
        return owner != null;
    }

    private static void spawnWave(ServerLevel arena, UUID owner, ArenaService.Session session) {
        session.waveMobs.clear();
        int minimumLevel = arenaMobLevel(arena, owner);
        ArenaSettings.Wave wave = session.waves.get(session.waveIndex);
        for (ArenaSettings.Mob spec : wave.mobs) {
            int count = count(spec.count);
            if (count <= 0) {
                continue;
            }
            EntityType<?> type = entityType(spec.id);
            if (type == null) {
                continue;
            }
            int base = count / CORNERS.length;
            int extra = count % CORNERS.length;
            for (int corner = 0; corner < CORNERS.length; corner++) {
                int amount = base + (corner < extra ? 1 : 0);
                for (int n = 0; n < amount; n++) {
                    spawn(arena, session, type, spec, corner, n, minimumLevel);
                }
            }
        }
        session.waveHealth = 0.0F;
        for (UUID id : session.waveMobs) {
            if (arena.getEntity(id) instanceof LivingEntity living) {
                session.waveHealth += living.getHealth();
            }
        }
        session.fighting = !session.waveMobs.isEmpty();
        CraftLevel.LOGGER.info("[CraftLevel] Leva {} da arena: {} mobs", session.waveIndex + 1, session.waveMobs.size());
    }

    private static int arenaMobLevel(ServerLevel arena, UUID owner) {
        int playerLevel = 1;
        ServerPlayer player = arena.getServer().getPlayerList().getPlayer(owner);
        if (player != null) {
            playerLevel = player.getCapability(PlayerLevelProvider.PLAYER_LEVEL)
                    .map(cap -> Math.max(1, cap.getLevel()))
                    .orElse(1);
        }
        return playerLevel + Math.max(0, GloryKillsParser.minLevelGap);
    }

    private static void spawn(ServerLevel arena, ArenaService.Session session, EntityType<?> type, ArenaSettings.Mob spec, int corner, int n, int minimumLevel) {
        int x = session.centerX + CORNERS[corner][0];
        int z = session.centerZ + CORNERS[corner][1];
        double ox = (n % 3) * 0.4D;
        double oz = (n / 3) * 0.4D;
        Entity created = type.create(arena);
        if (!(created instanceof Mob mob)) {
            return;
        }
        double px = x + 0.5D + ox;
        double pz = z + 0.5D + oz;
        float yaw = yawToward(px, pz, session.centerX + 0.5D, session.centerZ + 0.5D);
        mob.moveTo(px, SPAWN_Y, pz, yaw, 0.0F);
        mob.finalizeSpawn(arena, arena.getCurrentDifficultyAt(BlockPos.containing(px, SPAWN_Y, pz)), MobSpawnType.EVENT, null, null);
        equip(mob, EquipmentSlot.HEAD, spec.helmet);
        equip(mob, EquipmentSlot.CHEST, spec.chestplate);
        equip(mob, EquipmentSlot.LEGS, spec.leggings);
        equip(mob, EquipmentSlot.FEET, spec.boots);
        equip(mob, EquipmentSlot.MAINHAND, spec.hand);
        effect(mob, spec.effect);
        mob.setPersistenceRequired();
        mob.setYHeadRot(yaw);
        mob.getCapability(MobLevelProvider.MOB_LEVEL).ifPresent(cap -> {
            int level = Math.max(minimumLevel, cap.getLevel());
            cap.setLevel(level);
            StatRefreshService.refreshMob(mob, level);
        });
        if (arena.addFreshEntity(mob)) {
            session.waveMobs.add(mob.getUUID());
            PacketHandler.syncMobLevel(mob);
        }
    }

    private static List<ArenaSettings.Wave> choose(ServerPlayer player, ArenaService.Session session) {
        ArenaSettings settings = ArenaSettings.load(config(), List.of());
        String file = fileName(ArenaCatalog.find(session.arenaId).resource());
        ArenaSettings.Schematic schematic = null;
        for (ArenaSettings.Schematic candidate : settings.schematics()) {
            if (file.equals(candidate.file)) {
                schematic = candidate;
                break;
            }
        }
        List<ArenaSettings.HordeSet> pool = new ArrayList<>();
        if (schematic != null) {
            for (String name : schematic.hordeNames) {
                for (ArenaSettings.HordeSet set : settings.hordes()) {
                    if (name != null && name.equals(set.name)) {
                        pool.add(set);
                    }
                }
            }
        }
        if (pool.isEmpty()) {
            for (ArenaSettings.HordeSet set : settings.hordes()) {
                if ("default_horde".equals(set.name)) {
                    pool.add(set);
                }
            }
        }
        if (pool.isEmpty()) {
            return List.of();
        }
        return pool.get(player.getRandom().nextInt(pool.size())).waves;
    }

    private static void equip(Mob mob, EquipmentSlot slot, String raw) {
        ItemStack stack = item(raw);
        if (stack.isEmpty()) {
            return;
        }
        mob.setItemSlot(slot, stack);
        mob.setDropChance(slot, 0.0F);
    }

    private static void effect(Mob mob, String raw) {
        ResourceLocation id = location(raw);
        if (id == null) {
            return;
        }
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null);
        if (effect == null) {
            return;
        }
        mob.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0));
    }

    private static ItemStack item(String raw) {
        ResourceLocation id = location(raw);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private static EntityType<?> entityType(String raw) {
        ResourceLocation id = location(raw);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    private static int count(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static ResourceLocation location(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        try {
            return new ResourceLocation(id);
        } catch (Exception e) {
            return null;
        }
    }

    private static String fileName(String resource) {
        int slash = resource.lastIndexOf('/');
        return slash < 0 ? resource : resource.substring(slash + 1);
    }

    private static float yawToward(double x, double z, double targetX, double targetZ) {
        double dx = targetX - x;
        double dz = targetZ - z;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
    }

    private static java.nio.file.Path config() {
        return FMLPaths.CONFIGDIR.get().resolve("craftlevel").resolve("arena.json");
    }
}
