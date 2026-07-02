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

/**
 * Anexa {@link IMobLevel} ao mob e serializa level no NBT da entidade.
 */
public class MobLevelProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IMobLevel> MOB_LEVEL =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final MobLevel mobLevel = new MobLevel();
    private final LazyOptional<IMobLevel> optional = LazyOptional.of(() -> mobLevel);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == MOB_LEVEL ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", mobLevel.getLevel());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        mobLevel.setLevel(tag.getInt("level"));
    }
}
