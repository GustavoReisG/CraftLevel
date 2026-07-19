package com.grgs.craftlevel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Guarda os itens perdidos na lápide do player. */
public class PlayerGravestoneBlockEntity extends BlockEntity {

    private final List<ItemStack> items = new ArrayList<>();
    private String ownerName = "";

    public PlayerGravestoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLAYER_GRAVESTONE.get(), pos, state);
    }

    public void setStoredItems(List<ItemStack> stacks) {
        items.clear();
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        setChanged();
    }

    public List<ItemStack> getStoredItems() {
        return List.copyOf(items);
    }

    public List<ItemStack> takeStoredItems() {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                copy.add(stack.copy());
            }
        }
        items.clear();
        setChanged();
        return copy;
    }

    public void clearStoredItems() {
        items.clear();
        setChanged();
    }

    public boolean hasStoredItems() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName != null ? ownerName : "";
        setChanged();
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                list.add(stack.save(new CompoundTag()));
            }
        }
        tag.put("Items", list);
        tag.putString("OwnerName", ownerName);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        ownerName = tag.getString("OwnerName");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
