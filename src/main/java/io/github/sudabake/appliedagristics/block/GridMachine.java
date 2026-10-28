package io.github.sudabake.appliedagristics.block;

import appeng.api.networking.*;
import appeng.me.helpers.IGridConnectedBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class GridMachine extends MachineEntity implements IGridConnectedBlockEntity {
    private IManagedGridNode node;
    private CompoundTag unloadedNode = new CompoundTag();
    private boolean destroyed;
    protected GridMachine(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots); node = makeNode();
    }
    private IManagedGridNode makeNode() {
        var result = GridHelper.createManagedNode(this, new IGridNodeListener<GridMachine>() {
            @Override public void onSaveChanges(GridMachine owner, IGridNode node) { owner.setChanged(); }
        }).setInWorldNode(true).setFlags(this instanceof MEInfusionAltar ? new GridFlags[0] : new GridFlags[]{GridFlags.REQUIRE_CHANNEL}).setIdlePowerUsage(0).setVisualRepresentation(getBlockState().getBlock());
        if (this instanceof appeng.api.storage.IStorageProvider storage) result.addService(appeng.api.storage.IStorageProvider.class, storage);
        return result;
    }
    public void ensureNode() {
        if (!ownsNode()) return;
        if (!destroyed && !node.isReady() && level != null && !level.isClientSide) node.create(level, worldPosition);
    }
    protected boolean ownsNode() { return true; }
    protected NetworkAccess accessModule() { return null; }
    @Override public IManagedGridNode getMainNode() {
        var access = ownsNode() ? null : accessModule();
        return access == null ? node : access.getMainNode();
    }
    @Override public void saveChanges() { setChanged(); }
    @Override public boolean networkMachine() { return true; }
    public void release() {
        if (!destroyed) { unloadedNode = new CompoundTag(); node.saveToNBT(unloadedNode); node.destroy(); destroyed = true; }
    }
    @Override public void setRemoved() { release(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { release(); super.onChunkUnloaded(); }
    @Override public void clearRemoved() {
        super.clearRemoved();
        reviveNode();
    }
    protected final void reviveNode() {
        if (destroyed) { node = makeNode(); node.loadFromNBT(unloadedNode); destroyed = false; }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (destroyed) tag.merge(unloadedNode); else node.saveToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); node.loadFromNBT(tag);
    }
}
