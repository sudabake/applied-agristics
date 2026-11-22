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
    private net.minecraftforge.common.util.LazyOptional<GridMachine> gridHost = net.minecraftforge.common.util.LazyOptional.of(() -> this);
    protected GridMachine(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots); node = makeNode();
    }
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
        net.minecraftforge.common.capabilities.Capability<T> capability, net.minecraft.core.Direction side) {
        if (!isRemoved() && ownsNode() && (!(this instanceof NetworkAccess access) || access.connectedStructure())
            && (capability == appeng.capabilities.Capabilities.IN_WORLD_GRID_NODE_HOST
                || capability == appeng.capabilities.Capabilities.CRAFTING_MACHINE && this instanceof AEProcessingMachine)) {
            if (!gridHost.isPresent()) gridHost = net.minecraftforge.common.util.LazyOptional.of(() -> this);
            return gridHost.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override public IGridNode getGridNode(net.minecraft.core.Direction side) {
        if (!ownsNode() || (this instanceof NetworkAccess access && !access.connectedStructure())) return null;
        return IGridConnectedBlockEntity.super.getGridNode(side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        gridHost.invalidate();
    }

    private IManagedGridNode makeNode() {
        var result = GridHelper.createManagedNode(this, new IGridNodeListener<GridMachine>() {
            @Override public void onSaveChanges(GridMachine owner, IGridNode node) { owner.setChanged(); }
        }).setInWorldNode(true).setFlags(new GridFlags[0]).setIdlePowerUsage(0).setVisualRepresentation(getBlockState().getBlock());
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
    protected final void storedItemsChanged(appeng.api.stacks.AEKey key,long delta) {
        setChanged();
        var access=accessModule();
        if (access!=null) access.storageChanged(key,delta);
        else {
            var grid=getMainNode().getGrid();
            if (grid!=null) grid.getStorageService().invalidateCache();
        }
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
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (destroyed) tag.merge(unloadedNode); else node.saveToNBT(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); node.loadFromNBT(tag);
    }
}
