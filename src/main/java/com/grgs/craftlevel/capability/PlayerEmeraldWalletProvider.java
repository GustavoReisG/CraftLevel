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

/** Anexa {@link IPlayerEmeraldWallet} ao player e serializa no save. */
public class PlayerEmeraldWalletProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerEmeraldWallet> EMERALD_WALLET =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerEmeraldWallet wallet = new PlayerEmeraldWallet();
    private final LazyOptional<IPlayerEmeraldWallet> optional = LazyOptional.of(() -> wallet);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == EMERALD_WALLET ? optional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("balance", wallet.getBalance());
        tag.putBoolean("initialized", wallet.isInitialized());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        wallet.setBalance(tag.getLong("balance"));
        wallet.setInitialized(tag.getBoolean("initialized"));
    }
}
