package io.github.sudabake.appliedagristics.block;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.logic.Harvests;
import io.github.sudabake.appliedagristics.multiblock.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.*;

/** Finite 16-slot input/output port. No physical AE node; storage belongs to the formed structure. */
public final class ItemPort extends GridMachine implements SeedBank, IStorageProvider {
    private final boolean input;
    private boolean autoOutput;
    private PlanterStructure structure;
    private final Map<AEItemKey,Map<NetworkPlanter,Long>> leases=new HashMap<>();
    private final MEStorage networkInventory=new MEStorage() {
        @Override public net.minecraft.network.chat.Component getDescription() { return getDisplayName(); }
        @Override public long insert(AEKey key,long amount,Actionable mode,IActionSource source) { return 0; }
        @Override public long extract(AEKey key,long amount,Actionable mode,IActionSource source) {
            if (input || !(key instanceof AEItemKey item) || amount<=0) return 0;
            return remove(item,amount,mode==Actionable.SIMULATE);
        }
        @Override public void getAvailableStacks(KeyCounter out) {
            if (!input) for (int i=0;i<items.getSlots();i++) { var stack=items.getStackInSlot(i); if (!stack.isEmpty()) out.add(AEItemKey.of(stack),stack.getCount()); }
        }
    };
    public ItemPort(BlockPos pos,BlockState state,boolean input) { super(input ? Agristics.INPUT_BE.get() : Agristics.OUTPUT_BE.get(),pos,state,16); this.input=input; }
    public boolean isInput() { return input; }
    @Override protected boolean ownsNode() { return false; }
    @Override protected NetworkAccess accessModule() { return structure==null ? null : structure.access(); }
    public void bind(PlanterStructure owner) { structure=owner; }
    public void unbind(PlanterStructure owner) { if (structure==owner) { structure=null; leases.clear(); } }
    @Override public void release() { if (structure!=null) structure.invalidate(); super.release(); }
    @Override public boolean belongsTo(NetworkPlanter owner) { return input && structure!=null && structure.controller()==owner && structure.formed(); }
    @Override protected boolean valid(int slot,ItemStack stack) { return !input || Harvests.seed(stack,level); }
    @Override protected void inventoryChanged(int slot) { changed(); }
    private void changed() { setChanged(); var grid=getMainNode().getGrid(); if (grid!=null) grid.getStorageService().invalidateCache(); }
    @Override public void mountInventories(IStorageMounts mounts) { if (!input) mounts.mount(networkInventory,0); }
    @Override public Set<AEItemKey> keys() {
        var result=new LinkedHashSet<AEItemKey>();
        for (int i=0;i<items.getSlots();i++) if (!items.getStackInSlot(i).isEmpty()) result.add(AEItemKey.of(items.getStackInSlot(i)));
        return result;
    }
    @Override public long count(AEItemKey key) {
        long total=0; for (int i=0;i<items.getSlots();i++) if (key.matches(items.getStackInSlot(i))) total+=items.getStackInSlot(i).getCount(); return total;
    }
    @Override public long used(AEItemKey key) {
        var owners=leases.get(key); if (owners==null) return 0;
        owners.keySet().removeIf(owner -> !owner.leaseValid(this,key)); return owners.values().stream().mapToLong(Long::longValue).sum();
    }
    @Override public long claim(AEItemKey key,NetworkPlanter owner,long requested) {
        if (!belongsTo(owner)) return 0;
        used(key); var owners=leases.computeIfAbsent(key,k->new HashMap<>());
        long existing=owners.getOrDefault(owner,0L),other=owners.values().stream().mapToLong(Long::longValue).sum()-existing;
        long accepted=Math.min(Math.max(0,requested),Math.max(0,count(key)-other));
        if (accepted==0) owners.remove(owner); else owners.put(owner,accepted);
        return accepted;
    }
    @Override public void unclaim(AEItemKey key,NetworkPlanter owner) { var owners=leases.get(key); if (owners!=null) owners.remove(owner); }
    @Override public long add(AEItemKey key,long requested) { return insertLocal(key,requested,false); }
    public long insertLocal(AEItemKey key,long requested,boolean simulate) {
        if (requested<=0 || input && !Harvests.seed(key.toStack(1),level)) return 0;
        long left=requested;
        // Fill existing stacks first, then empty slots, so different crops retain useful slot space.
        for (boolean empty:new boolean[]{false,true}) for (int i=0;i<items.getSlots() && left>0;i++) {
            var stack=items.getStackInSlot(i); if (stack.isEmpty()!=empty || !empty && !key.matches(stack)) continue;
            int offered=(int)Math.min(left,key.getMaxStackSize());
            var remainder=items.insertItem(i,key.toStack(offered),simulate); left-=offered-remainder.getCount();
        }
        return requested-left;
    }
    @Override protected int extractable(int slot,int requested) {
        var stack=items.getStackInSlot(slot);
        return input && !stack.isEmpty() ? (int)Math.min(Math.max(0,requested),Math.max(0,count(AEItemKey.of(stack))-used(AEItemKey.of(stack)))) : requested;
    }
    public boolean slotInUse(int slot) { var stack=items.getStackInSlot(slot); return input && !stack.isEmpty() && used(AEItemKey.of(stack))>0; }
    /** Only actual player removals stop a crop; pipe probes keep their reservation guard. */
    public void releaseForManualExtraction(ItemStack stack) {
        if (input && !stack.isEmpty() && structure!=null && used(AEItemKey.of(stack))>0)
            structure.controller().pauseForSeedRemoval(AEItemKey.of(stack));
    }
    private long remove(AEItemKey key,long requested,boolean simulate) {
        long left=requested;
        for (int i=0;i<items.getSlots() && left>0;i++) if (key.matches(items.getStackInSlot(i))) left-=items.extractItem(i,(int)Math.min(left,Integer.MAX_VALUE),simulate).getCount();
        return requested-left;
    }
    public void toggleAutoOutput() { if (!input) { autoOutput=!autoOutput; setChanged(); } }
    public Direction outputFace() { return getBlockState().getValue(PlanterPartBlock.FACING); }
    public IItemHandler automation(Direction side) {
        return new IItemHandler() {
            @Override public int getSlots() { return items.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
            @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot,ItemStack stack) { return input && valid(slot,stack); }
            @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) { return isItemValid(slot,stack) ? items.insertItem(slot,stack,simulate) : stack; }
            @Override public ItemStack extractItem(int slot,int amount,boolean simulate) {
                return input || side==null || side==outputFace() ? items.extractItem(slot,amount,simulate) : ItemStack.EMPTY;
            }
        };
    }
    @Override public void step() {
        if (structure==null || !structure.formed()) { status="unformed"; return; }
        status=getMainNode().isActive() ? "online" : "offline";
        if (input || !autoOutput || !getMainNode().isActive()) return;
        var side=outputFace(); var pos=worldPosition.relative(side);
        if (!level.hasChunkAt(pos) || PlanterStructure.ownerAt(level,pos)==structure) return;
        var adjacent=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,side.getOpposite());
        if (adjacent!=null) push(adjacent);
    }
    private void push(IItemHandler target) {
        int budget=64;
        for (int i=0;i<items.getSlots() && budget>0;i++) {
            var offered=items.extractItem(i,budget,true);
            for (int j=0;j<target.getSlots() && !offered.isEmpty();j++) {
                var rest=target.insertItem(j,offered.copy(),false); int accepted=offered.getCount()-rest.getCount();
                if (accepted>0) { items.extractItem(i,accepted,false); budget-=accepted; } offered=rest;
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) { super.saveAdditional(tag,registries); tag.putBoolean("autoOutput",autoOutput); }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); leases.clear(); autoOutput=!input && tag.getBoolean("autoOutput");
    }
    @Override public CompoundTag snapshot(int page,String search) {
        var tag=super.snapshot(page,search); tag.putBoolean("autoOutput",autoOutput); tag.putBoolean("inputPort",input);
        var locked=new int[16]; for (int i=0;i<16;i++) locked[i]=slotInUse(i) ? 1 : 0; tag.putIntArray("lockedSlots",locked); return tag;
    }
}
