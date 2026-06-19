package com.pocketrepose.block;

import com.pocketrepose.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Stores which pocket dimension (by sanitized name) a placed suitcase is linked to. */
public class SuitcaseBlockEntity extends BlockEntity {
    private static final String KEY_LINKED = "LinkedDimension";

    @Nullable
    private String linkedDimension;

    public SuitcaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUITCASE.get(), pos, state);
    }

    @Nullable
    public String getLinkedDimension() {
        return linkedDimension;
    }

    public void setLinkedDimension(@Nullable String linkedDimension) {
        this.linkedDimension = (linkedDimension == null || linkedDimension.isEmpty()) ? null : linkedDimension;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isLinked() {
        return linkedDimension != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (linkedDimension != null) {
            tag.putString(KEY_LINKED, linkedDimension);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.linkedDimension = tag.contains(KEY_LINKED) ? tag.getString(KEY_LINKED) : null;
    }
}
