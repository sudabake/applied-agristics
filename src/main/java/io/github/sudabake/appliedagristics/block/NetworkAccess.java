package io.github.sudabake.appliedagristics.block;

import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.multiblock.PlanterStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** The only physical AE node of a formed planter. */
public final class NetworkAccess extends GridMachine implements IStorageProvider {
    private PlanterStructure structure;
    public NetworkAccess(BlockPos pos, BlockState state) { super(Agristics.ACCESS_BE.get(), pos, state, 0); }
    public void bind(PlanterStructure structure) { this.structure = structure; invalidateCapabilities(); }
    public void unbind(PlanterStructure previous) {
        if (structure == previous) { structure = null; invalidateCapabilities(); super.release(); }
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
        if (connectedStructure()) structure.controller().open(player);
        else player.displayClientMessage(Component.translatable("status.appliedagristics.unformed"), true);
    }
    @Override public void release() {
        var previous = structure;
        if (previous != null) previous.invalidate();
        super.release();
    }
}
