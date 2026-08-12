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

/** Anexa {@link IPlayerGlory} ao player e serializa no save. */
public class PlayerGloryProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerGlory> PLAYER_GLORY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerGlory glory = new PlayerGlory();
    private final LazyOptional<IPlayerGlory> optional = LazyOptional.of(() -> glory);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_GLORY ? optional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("gloryKills", glory.getGloryKills());
        tag.putInt("skillPoints", glory.getSkillPoints());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        glory.setGloryKills(tag.getInt("gloryKills"));
        glory.setSkillPoints(tag.getInt("skillPoints"));
    }
}
