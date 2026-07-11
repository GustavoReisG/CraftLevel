package com.grgs.craftlevel.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Anexa {@link IPlayerVillagerBalance} ao player e serializa no save. */
public class PlayerVillagerBalanceProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerVillagerBalance> VILLAGER_BALANCE =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerVillagerBalance balance = new PlayerVillagerBalance();
    private final LazyOptional<IPlayerVillagerBalance> optional = LazyOptional.of(() -> balance);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == VILLAGER_BALANCE ? optional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("initialized", balance.isInitialized());
        tag.put("entities", balance.writeEntitiesNbt());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        balance.clearEntities();
        balance.setInitialized(tag.getBoolean("initialized"));

        if (tag.contains("entities", Tag.TAG_LIST)) {
            ListTag list = tag.getList("entities", Tag.TAG_COMPOUND);
            for (Tag element : list) {
                CompoundTag entityTag = (CompoundTag) element;
                balance.loadEntity(
                        entityTag.getUUID("entity_id"),
                        entityTag.getString("profession"),
                        entityTag.getInt("balance"),
                        entityTag.getLong("last_refresh")
                );
            }
            return;
        }

        // Legado: saldo por profissão — descartado ao migrar para saldo por entidade.
        if (tag.contains("balances", Tag.TAG_LIST)) {
            balance.setInitialized(true);
        }
    }
}
