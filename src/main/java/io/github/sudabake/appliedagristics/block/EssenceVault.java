package io.github.sudabake.appliedagristics.block;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.logic.Amounts;
import io.github.sudabake.appliedagristics.multiblock.PlanterStructure;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Machine-owned essence output storage. AE may extract, never insert. Drops as one data-bearing block. */
public final class EssenceVault extends GridMachine implements IStorageProvider {
    private PlanterStructure structure;
    private final Map<AEItemKey,Long> contents=new LinkedHashMap<>();
    private final MEStorage networkInventory=new MEStorage() {
        @Override public net.minecraft.network.chat.Component getDescription() { return getDisplayName(); }
        @Override public long insert(AEKey key,long amount,Actionable mode,IActionSource source) { return 0; }
        @Override public long extract(AEKey key,long amount,Actionable mode,IActionSource source) {
            if (!(key instanceof AEItemKey item) || amount<=0) return 0;
            long taken=Math.min(amount,contents.getOrDefault(item,0L));
            if (mode==Actionable.MODULATE && taken>0) { long left=contents.get(item)-taken; if (left==0) contents.remove(item); else contents.put(item,left); changed(); }
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
    @Override public void mountInventories(IStorageMounts mounts) { mounts.mount(networkInventory,0); }
    public long storeProduced(AEItemKey key,long amount,boolean simulate) {
        if (amount<=0 || !Crops.essence(key.toStack(1))) return 0;
        long accepted=Amounts.insertable(contents.getOrDefault(key,0L),amount);
        if (!simulate && accepted>0) { contents.merge(key,accepted,Long::sum); changed(); }
        return accepted;
    }
    private void changed() { setChanged(); var grid=getMainNode().getGrid(); if (grid!=null) grid.getStorageService().invalidateCache(); }
    @Override public void step() { status=structure==null || !structure.formed() ? "unformed" : getMainNode().isActive() ? "online" : "offline"; }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries); var list=new ListTag();
        contents.forEach((key,count)->{ var entry=new CompoundTag(); entry.put("key",key.toTag(registries)); entry.putLong("count",count); list.add(entry); }); tag.put("essences",list);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); contents.clear();
        for (var raw:tag.getList("essences",10)) { var entry=(CompoundTag)raw; var key=AEItemKey.fromTag(registries,entry.getCompound("key")); long count=entry.getLong("count"); if (key!=null && count>0) contents.put(key,count); }
    }
    @Override public CompoundTag snapshot(int offset,String search) {
        var tag=super.snapshot(offset,search); var entries=List.copyOf(contents.entrySet());
        int start=Math.clamp(offset,0,Math.max(0,entries.size()-4)); var rows=new ListTag();
        for (var entry:entries.subList(start,Math.min(start+4,entries.size()))) { var row=new CompoundTag(); row.put("item",entry.getKey().toStack(1).save(level.registryAccess())); row.putLong("count",entry.getValue()); rows.add(row); }
        tag.put("rows",rows); tag.putInt("pages",Math.max(1,entries.size()-3)); tag.putInt("types",contents.size()); return tag;
    }
}
