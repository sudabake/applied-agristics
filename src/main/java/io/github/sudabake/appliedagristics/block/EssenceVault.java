package io.github.sudabake.appliedagristics.block;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.logic.Amounts;
import io.github.sudabake.appliedagristics.logic.StoredItemCounts;
import io.github.sudabake.appliedagristics.multiblock.PlanterStructure;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Crop output storage; retains its original registry ID and NBT format for upgrades. */
public final class EssenceVault extends GridMachine implements IStorageProvider {
    private PlanterStructure structure;
    private ListTag unresolved = new ListTag();
    private int priority;
    private final Map<AEItemKey,Long> contents=new LinkedHashMap<>();
    private final MEStorage networkInventory=new MEStorage() {
        @Override public net.minecraft.network.chat.Component getDescription() { return getDisplayName(); }
        @Override public long insert(AEKey key,long amount,Actionable mode,IActionSource source) { return 0; }
        @Override public long extract(AEKey key,long amount,Actionable mode,IActionSource source) {
            if (!(key instanceof AEItemKey item) || amount<=0) return 0;
            long taken=Math.min(amount,contents.getOrDefault(item,0L));
            if (mode==Actionable.MODULATE && taken>0) { long left=contents.get(item)-taken; if (left==0) contents.remove(item); else contents.put(item,left); changed(item,-taken); }
            return taken;
        }
        @Override public void getAvailableStacks(KeyCounter out) { contents.forEach(out::add); }
    };
    public EssenceVault(BlockPos pos,BlockState state) { super(Agristics.VAULT_BE.get(),pos,state,0); }
    @Override protected boolean ownsNode() { return false; }
    @Override protected NetworkAccess accessModule() { return structure==null ? null : structure.access(); }
    public void bind(PlanterStructure owner) { structure=owner; }
    public void unbind(PlanterStructure owner) { if (structure==owner) structure=null; }
    @Override public void release() { if (structure!=null) structure.invalidate(); super.release(); }
    @Override public void mountInventories(IStorageMounts mounts) { mounts.mount(networkInventory,priority); }
    public int getPriority() { return priority; }
    public void setPriority(int value) {
        if (priority==value) return;
        priority=value; setChanged();
        IStorageProvider.requestUpdate(getMainNode());
    }
    public long storeProduced(AEItemKey key,long amount,boolean simulate) {
        if (amount<=0 || key.toStack(1).isEmpty()) return 0;
        long accepted=Amounts.insertable(contents.getOrDefault(key,0L),amount);
        if (!simulate && accepted>0) { contents.merge(key,accepted,Long::sum); changed(key,accepted); }
        return accepted;
    }
    private void changed(AEItemKey key,long delta) { storedItemsChanged(key,delta); }
    @Override public void step() { status=structure==null || !structure.formed() ? "unformed" : getMainNode().isActive() ? "online" : "offline"; }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        tag.putInt("priority",priority);
        tag.put("essences",StoredItemCounts.save(contents,unresolved,registries));
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        priority=tag.getInt("priority");
        contents.clear();
        unresolved=StoredItemCounts.load(tag.getList("essences",Tag.TAG_COMPOUND),contents,registries);
    }
    @Override public CompoundTag snapshot(int offset,String search) {
        var tag=super.snapshot(offset,search); tag.putInt("unresolved",unresolved.size()); var entries=List.copyOf(contents.entrySet());
        int start=Math.clamp(offset,0,Math.max(0,entries.size()-4)); var rows=new ListTag();
        for (var entry:entries.subList(start,Math.min(start+4,entries.size()))) { var row=new CompoundTag(); row.put("item",entry.getKey().toStack(1).save(level.registryAccess())); row.putLong("count",entry.getValue()); rows.add(row); }
        tag.put("rows",rows); tag.putInt("pages",Math.max(1,entries.size()-3)); tag.putInt("types",contents.size()); tag.putInt("priority",priority); return tag;
    }
}
