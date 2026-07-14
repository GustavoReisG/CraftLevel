package com.grgs.craftlevel.capability;

import com.grgs.craftlevel.config.EconomyParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Estado de economia persistido no villager. */
public class VillagerEconomy implements IVillagerEconomy {

    private int balance;
    private long lastRefreshTick;
    private String professionId = "minecraft:none";
    private int[] tradeUses = new int[0];
    private boolean initialized;

    @Override
    public int getBalance() {
        return balance;
    }

    @Override
    public void setBalance(int balance) {
        this.balance = Math.max(0, balance);
    }

    @Override
    public void addBalance(int amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    @Override
    public boolean trySpendBalance(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }

    @Override
    public void ensureInitialized(String professionId, int starting, long gameTime) {
        if (!initialized) {
            this.professionId = professionId;
            balance = Math.max(0, starting);
            lastRefreshTick = gameTime;
            tradeUses = new int[0];
            initialized = true;
            return;
        }

        if (!this.professionId.equals(professionId)) {
            this.professionId = professionId;
            tradeUses = new int[0];
        }
    }

    @Override
    public void resetBalance(int starting) {
        balance = Math.max(0, starting);
    }

    @Override
    public String getProfessionId() {
        return professionId;
    }

    @Override
    public long getLastRefreshTick() {
        return lastRefreshTick;
    }

    @Override
    public void setLastRefreshTick(long gameTime) {
        lastRefreshTick = gameTime;
    }

    @Override
    public int getTradeUses(int tradeIndex) {
        if (tradeIndex < 0 || tradeIndex >= tradeUses.length) {
            return 0;
        }
        return tradeUses[tradeIndex];
    }

    @Override
    public void incrementTradeUses(int tradeIndex) {
        if (tradeIndex < 0) {
            return;
        }
        if (tradeUses.length <= tradeIndex) {
            int[] expanded = new int[tradeIndex + 1];
            System.arraycopy(tradeUses, 0, expanded, 0, tradeUses.length);
            tradeUses = expanded;
        }
        tradeUses[tradeIndex]++;
    }

    @Override
    public void clearTradeUses() {
        tradeUses = new int[0];
    }

    @Override
    public void applyEconomyConfig(String professionId) {
        this.professionId = professionId;
        balance = Math.max(0, EconomyParser.startingBalanceForProfession(professionId));
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("initialized", initialized);
        tag.putInt("balance", balance);
        tag.putLong("last_refresh", lastRefreshTick);
        tag.putString("profession", professionId);
        tag.put("trade_uses", writeTradeUses(tradeUses));
        return tag;
    }

    void deserializeNBT(CompoundTag tag) {
        initialized = tag.getBoolean("initialized");
        balance = Math.max(0, tag.getInt("balance"));
        lastRefreshTick = tag.getLong("last_refresh");
        professionId = tag.contains("profession", Tag.TAG_STRING)
                ? tag.getString("profession")
                : "minecraft:none";
        tradeUses = readTradeUses(tag);
    }

    private static ListTag writeTradeUses(int[] uses) {
        ListTag list = new ListTag();
        if (uses != null) {
            for (int use : uses) {
                list.add(net.minecraft.nbt.IntTag.valueOf(use));
            }
        }
        return list;
    }

    private static int[] readTradeUses(CompoundTag tag) {
        if (!tag.contains("trade_uses", Tag.TAG_LIST)) {
            return new int[0];
        }
        ListTag list = tag.getList("trade_uses", Tag.TAG_INT);
        int[] uses = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            uses[i] = list.getInt(i);
        }
        return uses;
    }
}
