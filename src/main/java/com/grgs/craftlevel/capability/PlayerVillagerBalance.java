package com.grgs.craftlevel.capability;

import com.grgs.craftlevel.config.EconomyParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Implementação mutável do saldo de trade por villager (entity UUID). */
public class PlayerVillagerBalance implements IPlayerVillagerBalance {

    private record EntityEntry(int balance, long lastRefreshTick, String professionId) {}

    private final Map<UUID, EntityEntry> entities = new HashMap<>();
    private boolean initialized;

    @Override
    public int getBalance(UUID entityId) {
        EntityEntry entry = entities.get(entityId);
        return entry != null ? entry.balance() : 0;
    }

    @Override
    public void setBalance(UUID entityId, int balance) {
        EntityEntry entry = entities.get(entityId);
        if (entry == null) {
            return;
        }
        entities.put(entityId, new EntityEntry(Math.max(0, balance), entry.lastRefreshTick(), entry.professionId()));
    }

    @Override
    public void addBalance(UUID entityId, int amount) {
        if (amount <= 0) {
            return;
        }
        EntityEntry entry = entities.get(entityId);
        if (entry == null) {
            return;
        }
        entities.put(entityId, new EntityEntry(entry.balance() + amount, entry.lastRefreshTick(), entry.professionId()));
    }

    @Override
    public boolean trySpendBalance(UUID entityId, int amount) {
        if (amount <= 0) {
            return true;
        }
        EntityEntry entry = entities.get(entityId);
        if (entry == null || entry.balance() < amount) {
            return false;
        }
        entities.put(entityId, new EntityEntry(entry.balance() - amount, entry.lastRefreshTick(), entry.professionId()));
        return true;
    }

    @Override
    public void ensureEntity(UUID entityId, String professionId, int starting, long gameTime) {
        EntityEntry existing = entities.get(entityId);
        if (existing == null) {
            entities.put(entityId, new EntityEntry(Math.max(0, starting), gameTime, professionId));
            return;
        }
        if (!existing.professionId().equals(professionId)) {
            entities.put(entityId, new EntityEntry(existing.balance(), existing.lastRefreshTick(), professionId));
        }
    }

    @Override
    public void resetEntity(UUID entityId, int starting) {
        EntityEntry entry = entities.get(entityId);
        if (entry == null) {
            return;
        }
        entities.put(entityId, new EntityEntry(Math.max(0, starting), entry.lastRefreshTick(), entry.professionId()));
    }

    @Override
    public String getProfession(UUID entityId) {
        EntityEntry entry = entities.get(entityId);
        return entry != null ? entry.professionId() : "minecraft:none";
    }

    @Override
    public long getLastRefreshTick(UUID entityId) {
        EntityEntry entry = entities.get(entityId);
        return entry != null ? entry.lastRefreshTick() : 0L;
    }

    @Override
    public void setLastRefreshTick(UUID entityId, long gameTime) {
        EntityEntry entry = entities.get(entityId);
        if (entry == null) {
            return;
        }
        entities.put(entityId, new EntityEntry(entry.balance(), gameTime, entry.professionId()));
    }

    @Override
    public Iterable<UUID> trackedEntities() {
        return Set.copyOf(entities.keySet());
    }

    @Override
    public void applyEconomyConfig() {
        entities.replaceAll((entityId, entry) -> new EntityEntry(
                Math.max(0, EconomyParser.startingBalanceForProfession(entry.professionId())),
                entry.lastRefreshTick(),
                entry.professionId()
        ));
    }

    @Override
    public void pruneEntity(UUID entityId) {
        entities.remove(entityId);
    }

    @Override
    public void copyFrom(IPlayerVillagerBalance other) {
        entities.clear();
        if (other instanceof PlayerVillagerBalance typed) {
            entities.putAll(typed.entities);
        }
        setInitialized(other.isInitialized());
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    void clearEntities() {
        entities.clear();
    }

    void loadEntity(UUID entityId, String professionId, int balance, long lastRefreshTick) {
        entities.put(entityId, new EntityEntry(Math.max(0, balance), lastRefreshTick, professionId));
    }

    ListTag writeEntitiesNbt() {
        ListTag list = new ListTag();
        entities.forEach((entityId, entry) -> {
            CompoundTag entityTag = new CompoundTag();
            entityTag.putUUID("entity_id", entityId);
            entityTag.putString("profession", entry.professionId());
            entityTag.putInt("balance", entry.balance());
            entityTag.putLong("last_refresh", entry.lastRefreshTick());
            list.add(entityTag);
        });
        return list;
    }
}
