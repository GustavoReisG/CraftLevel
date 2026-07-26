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

/** Anexa {@link IPlayerEmeraldBank} ao player e serializa no save. */
public class PlayerEmeraldBankProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerEmeraldBank> EMERALD_BANK =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerEmeraldBank bank = new PlayerEmeraldBank();
    private final LazyOptional<IPlayerEmeraldBank> optional = LazyOptional.of(() -> bank);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == EMERALD_BANK ? optional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("balance", bank.getBalance());
        tag.putBoolean("initialized", bank.isInitialized());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        bank.setBalance(tag.getLong("balance"));
        bank.setInitialized(tag.getBoolean("initialized"));
    }
}
