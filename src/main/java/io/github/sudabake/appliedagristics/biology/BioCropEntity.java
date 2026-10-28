package io.github.sudabake.appliedagristics.biology;

import io.github.sudabake.appliedagristics.Agristics;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** No ticking or inventory: only the planted species is saved and synchronized. */
public final class BioCropEntity extends BlockEntity {
    private ResourceLocation identity;
    public BioCropEntity(BlockPos pos,BlockState state) { super(Agristics.BIO_CROP_BE.get(),pos,state); }
    public ResourceLocation identity() { return identity; }
    public void setIdentity(ResourceLocation id) {
        identity=id; setChanged();
        if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries); if (identity!=null) tag.putString("identity",identity.toString());
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); identity=ResourceLocation.tryParse(tag.getString("identity"));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
