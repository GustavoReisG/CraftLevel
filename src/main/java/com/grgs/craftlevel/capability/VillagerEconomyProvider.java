package com.grgs.craftlevel.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Anexa {@link IVillagerEconomy} ao villager e serializa no NBT da entidade. */
public class VillagerEconomyProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IVillagerEconomy> VILLAGER_ECONOMY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final VillagerEconomy economy = new VillagerEconomy();
    private final LazyOptional<IVillagerEconomy> optional = LazyOptional.of(() -> economy);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == VILLAGER_ECONOMY ? optional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return economy.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        economy.deserializeNBT(tag);
    }
}
