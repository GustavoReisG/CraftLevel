package com.grgs.craftlevel.arena;

import com.grgs.craftlevel.CraftLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Abre uma cópia da arena, trava blocos e apaga a cópia quando o jogador sai. */
public final class ArenaService {

    private static final String DATA_ID = "craftlevel_arena";

    private ArenaService() {}

    public static void enter(ServerPlayer player) {
        MinecraftServer server = player.server;
        ArenaData data = data(server);
        if (data.sessions.containsKey(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.craftlevel.arena.busy"), false);
            return;
        }
        ServerLevel arena = server.getLevel(ArenaDimension.LEVEL);
        if (arena == null) {
            player.displayClientMessage(Component.translatable("message.craftlevel.arena.no_dimension"), false);
            return;
        }

        int slot = data.freeSlot();
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("message.craftlevel.arena.full"), false);
            return;
        }

        ArenaCatalog.Entry template = ArenaCatalog.pick(player.getRandom());
        ArenaSlots.Slot point = ArenaSlots.get(slot);
        try {
            ArenaSchematic.Bounds bounds = ArenaSchematic.paste(
                    arena, template.resource(), point.x(), point.y(), point.z()
            );
            ArenaBiomes.fill(arena, bounds, ArenaWaves.biome(arena, template));
            Session session = new Session(template.id(), player.gameMode.getGameModeForPlayer().getName(), slot, bounds);
            BlockPos feet = player.blockPosition();
            session.returnDim = player.level().dimension().location().toString();
            session.returnX = feet.getX();
            session.returnY = feet.getY();
            session.returnZ = feet.getZ();
            session.returnYaw = player.getYRot();
            data.sessions.put(player.getUUID(), session);
            data.setDirty();
            player.teleportTo(
                    arena,
                    point.x() + 0.5D,
                    point.y() + ArenaSchematic.PLAYER_Y,
                    point.z() + 0.5D,
                    player.getYRot(),
                    player.getXRot()
            );
            ArenaWaves.begin(arena, player, session, point);
        } catch (Exception e) {
            CraftLevel.LOGGER.error("[CraftLevel] Falha ao colar a arena", e);
            player.displayClientMessage(Component.translatable("message.craftlevel.arena.failed"), false);
        }
    }

    public static boolean locks(Player player) {
        return player.level().dimension().equals(ArenaDimension.LEVEL);
    }

    public static boolean locks(LevelAccessor level) {
        return level instanceof Level world && world.dimension().equals(ArenaDimension.LEVEL);
    }

    public static boolean pendingExit(ServerPlayer player) {
        return data(player.server).pendingExit.contains(player.getUUID());
    }

    public static void markExit(ServerPlayer player) {
        if (!player.level().dimension().equals(ArenaDimension.LEVEL)) {
            return;
        }
        data(player.server).pendingExit.add(player.getUUID());
        data(player.server).setDirty();
    }

    public static void finishExit(ServerPlayer player) {
        if (!data(player.server).pendingExit.contains(player.getUUID())) {
            return;
        }
        boolean stillInArena = player.level().dimension().equals(ArenaDimension.LEVEL);
        leave(player, stillInArena ? Exit.SPAWNPOINT : Exit.STAY);
    }

    /** Saiu do mundo ou do servidor: volta ao bloco de entrada e libera o ponto. */
    public static void disconnect(ServerPlayer player) {
        if (!data(player.server).sessions.containsKey(player.getUUID())) {
            return;
        }
        leave(player, Exit.ENTRY);
    }

    /** Se o jogador já saiu da dimensão sem passar pelo respawn, apaga a cópia. */
    public static void recover(ServerPlayer player) {
        ArenaData data = data(player.server);
        Session session = data.sessions.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (player.level().dimension().equals(ArenaDimension.LEVEL)) {
            return;
        }
        leave(player, Exit.STAY);
    }

    static void leaveToEntry(ServerPlayer player) {
        leave(player, Exit.ENTRY);
    }

    private enum Exit { STAY, ENTRY, SPAWNPOINT }

    private static void leave(ServerPlayer player, Exit exit) {
        ArenaWaves.dismiss(player);
        ArenaData data = data(player.server);
        Session session = data.sessions.remove(player.getUUID());
        data.pendingExit.remove(player.getUUID());
        data.setDirty();
        if (session == null) {
            return;
        }

        if (exit == Exit.ENTRY) {
            teleportToEntry(player, session);
        } else if (exit == Exit.SPAWNPOINT) {
            teleportToSpawnPoint(player);
        }

        ServerLevel arena = player.server.getLevel(ArenaDimension.LEVEL);
        if (arena != null) {
            ArenaSchematic.clear(arena, session.bounds);
            sweepEntities(arena, session.bounds);
            ArenaBiomes.fill(arena, session.bounds, Biomes.THE_VOID);
        }
    }

    private static void teleportToEntry(ServerPlayer player, Session session) {
        if (session.returnDim == null || session.returnDim.isBlank()) {
            teleportToSpawnPoint(player);
            return;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(session.returnDim));
        ServerLevel level = player.server.getLevel(key);
        if (level == null || level.dimension().equals(ArenaDimension.LEVEL)) {
            teleportToSpawnPoint(player);
            return;
        }
        player.teleportTo(
                level,
                session.returnX + 0.5D,
                session.returnY,
                session.returnZ + 0.5D,
                session.returnYaw,
                0.0F
        );
    }

    private static void teleportToSpawnPoint(ServerPlayer player) {
        ResourceKey<Level> key = player.getRespawnDimension();
        BlockPos pos = player.getRespawnPosition();
        float angle = player.getRespawnAngle();
        ServerLevel level = key == null ? null : player.server.getLevel(key);
        if (level == null || pos == null || level.dimension().equals(ArenaDimension.LEVEL)) {
            level = player.server.overworld();
            pos = level.getSharedSpawnPos();
            angle = level.getSharedSpawnAngle();
        }
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, angle, 0.0F);
    }

    /** Tira mobs, itens e blocos caindo. A caixa passa um pouco do schematic, porque o drop espalha. */
    private static void sweepEntities(ServerLevel arena, ArenaSchematic.Bounds bounds) {
        AABB box = new AABB(
                bounds.minX() - 8.0D, bounds.minY() - 16.0D, bounds.minZ() - 8.0D,
                bounds.maxX() + 9.0D, bounds.maxY() + 8.0D, bounds.maxZ() + 9.0D
        );
        for (Entity entity : arena.getEntitiesOfClass(Entity.class, box, entity -> !(entity instanceof Player))) {
            entity.discard();
        }
    }

    static ArenaData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ArenaData::load, ArenaData::new, DATA_ID);
    }

    static final class Session {
        final String arenaId;
        private final String gameMode;
        private final int slot;
        final ArenaSchematic.Bounds bounds;
        final List<UUID> waveMobs = new ArrayList<>();
        List<ArenaSettings.Wave> waves = List.of();
        int waveIndex;
        long nextWaveAt = -1L;
        float waveHealth;
        boolean fighting;
        boolean victorious;
        String returnDim = "";
        int returnX;
        int returnY;
        int returnZ;
        float returnYaw;
        int centerX;
        int centerZ;

        private Session(String arenaId, String gameMode, int slot, ArenaSchematic.Bounds bounds) {
            this.arenaId = arenaId;
            this.gameMode = gameMode;
            this.slot = slot;
            this.bounds = bounds;
        }
    }

    public static final class ArenaData extends SavedData {
        private final Map<UUID, Session> sessions = new HashMap<>();
        private final Set<UUID> pendingExit = new HashSet<>();

        public ArenaData() {}

        Map<UUID, Session> sessions() {
            return sessions;
        }

        /** Primeiro ponto livre, começando em {@code 0, 62, 0}. {@code -1} se os 1089 pontos estão ocupados. */
        private int freeSlot() {
            Set<Integer> used = new HashSet<>();
            for (Session session : sessions.values()) {
                used.add(session.slot);
            }
            for (int slot = 0; slot < ArenaSlots.count(); slot++) {
                if (!used.contains(slot)) {
                    return slot;
                }
            }
            return -1;
        }

        public static ArenaData load(CompoundTag tag) {
            ArenaData data = new ArenaData();
            ListTag list = tag.getList("sessions", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                UUID id = entry.getUUID("player");
                ArenaSchematic.Bounds bounds = new ArenaSchematic.Bounds(
                        entry.getInt("minX"), entry.getInt("minY"), entry.getInt("minZ"),
                        entry.getInt("maxX"), entry.getInt("maxY"), entry.getInt("maxZ")
                );
                Session session = new Session(entry.getString("arena"), entry.getString("gameMode"), entry.getInt("slot"), bounds);
                session.returnDim = entry.getString("returnDim");
                session.returnX = entry.getInt("returnX");
                session.returnY = entry.getInt("returnY");
                session.returnZ = entry.getInt("returnZ");
                session.returnYaw = entry.getFloat("returnYaw");
                data.sessions.put(id, session);
                if (entry.getBoolean("pendingExit")) {
                    data.pendingExit.add(id);
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
                Session session = entry.getValue();
                CompoundTag nbt = new CompoundTag();
                nbt.putUUID("player", entry.getKey());
                nbt.putString("arena", session.arenaId);
                nbt.putInt("slot", session.slot);
                nbt.putString("gameMode", session.gameMode);
                nbt.putBoolean("pendingExit", pendingExit.contains(entry.getKey()));
                nbt.putInt("minX", session.bounds.minX());
                nbt.putInt("minY", session.bounds.minY());
                nbt.putInt("minZ", session.bounds.minZ());
                nbt.putInt("maxX", session.bounds.maxX());
                nbt.putInt("maxY", session.bounds.maxY());
                nbt.putInt("maxZ", session.bounds.maxZ());
                nbt.putString("returnDim", session.returnDim == null ? "" : session.returnDim);
                nbt.putInt("returnX", session.returnX);
                nbt.putInt("returnY", session.returnY);
                nbt.putInt("returnZ", session.returnZ);
                nbt.putFloat("returnYaw", session.returnYaw);
                list.add(nbt);
            }
            tag.put("sessions", list);
            return tag;
        }
    }
}
