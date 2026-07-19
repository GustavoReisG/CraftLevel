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

/** Anexa {@link IPlayerSouls} ao player e serializa no save. */
public class PlayerSoulsProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerSouls> PLAYER_SOULS =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerSouls souls = new PlayerSouls();
    private final LazyOptional<IPlayerSouls> optional = LazyOptional.of(() -> souls);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_SOULS ? optional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("souls", souls.getSouls());
        tag.putInt("restoreProgressTicks", souls.getRestoreProgressTicks());
        tag.putBoolean("initialized", souls.isInitialized());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        souls.setSouls(tag.getInt("souls"));
        souls.setRestoreProgressTicks(tag.getInt("restoreProgressTicks"));
        souls.setInitialized(tag.getBoolean("initialized"));
    }
}
