package io.github.sudabake.appliedagristics.block;

import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.multiblock.PlanterStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** The only physical AE node of a formed planter. */
public final class NetworkAccess extends GridMachine implements IStorageProvider {
    private PlanterStructure structure;
    private java.util.Map<appeng.api.stacks.AEKey,Long> productionInventory;
    private boolean storageDirty;

    /** Server-thread production scope. Reflect local mutations before the next crop's target check. */
    public void beginProduction(java.util.Map<appeng.api.stacks.AEKey,Long> inventory) {
        if (productionInventory!=null) throw new IllegalStateException("Production batch already active");
        productionInventory=inventory;
        storageDirty=false;
    }
    void storageChanged(appeng.api.stacks.AEKey key,long delta) {
        if (productionInventory!=null) {
            if (key!=null && delta!=0) productionInventory.merge(key,delta,com.google.common.math.LongMath::saturatedAdd);
            storageDirty=true;
        } else {
            var grid=getMainNode().getGrid();
            if (grid!=null) grid.getStorageService().invalidateCache();
        }
    }
    public void endProduction() {
        productionInventory=null;
        if (storageDirty) {
            storageDirty=false;
            var grid=getMainNode().getGrid();
            if (grid!=null) grid.getStorageService().invalidateCache();
        }
    }
    public long insertOverflow(appeng.api.stacks.AEItemKey item, long amount,
            appeng.api.config.Actionable mode) {
        var grid=getMainNode().getGrid();
        if (grid==null || !getMainNode().isActive()) return 0;
        long inserted=grid.getStorageService().getInventory().insert(item,amount,mode,
                appeng.api.networking.security.IActionSource.ofMachine(this));
        if (mode==appeng.api.config.Actionable.MODULATE && inserted>0) storageChanged(item,inserted);
        return inserted;
    }
    public NetworkAccess(BlockPos pos, BlockState state) { super(Agristics.ACCESS_BE.get(), pos, state, 0); }
    public void bind(PlanterStructure structure) { this.structure = structure; invalidateCaps(); reviveCaps(); }
    public void unbind(PlanterStructure previous) {
        if (structure == previous) { structure = null; invalidateCaps(); reviveCaps(); super.release(); }
    }
    public boolean connectedStructure() { return structure != null && structure.formed(); }
    @Override public void ensureNode() {
        if (connectedStructure()) { reviveNode(); super.ensureNode(); }
    }
    @Override public void mountInventories(IStorageMounts mounts) {
        if (connectedStructure()) {
            structure.seeds().forEach(seed -> seed.mountInventories(mounts));
            structure.ports().forEach(port -> port.mountInventories(mounts));
            structure.vaults().forEach(vault -> vault.mountInventories(mounts));
        }
    }
    @Override public void step() { status = !connectedStructure() ? "unformed" : getMainNode().isActive() ? "online" : "offline"; }
    @Override public void open(ServerPlayer player) {
        // Connection-only component; planter management belongs to the controller.
    }
    @Override public void release() {
        var previous = structure;
        if (previous != null) previous.invalidate();
        super.release();
    }
}
