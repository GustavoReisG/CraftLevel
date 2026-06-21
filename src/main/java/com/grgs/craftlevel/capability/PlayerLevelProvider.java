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

public class PlayerLevelProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerLevel> PLAYER_LEVEL =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerLevel playerLevel = new PlayerLevel();
    private final LazyOptional<IPlayerLevel> optional = LazyOptional.of(() -> playerLevel);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_LEVEL ? optional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", playerLevel.getLevel());
        tag.putInt("lp", playerLevel.getLp());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        playerLevel.setLevel(tag.getInt("level"));
        playerLevel.setLp(tag.getInt("lp"));
    }
}