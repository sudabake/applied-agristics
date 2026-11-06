package io.github.sudabake.appliedagristics.block;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.logic.Amounts;
import io.github.sudabake.appliedagristics.logic.StoredItemCounts;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.*;

public final class SeedStorage extends GridMachine implements appeng.api.storage.IStorageProvider, io.github.sudabake.appliedagristics.multiblock.SeedBank {
    public static boolean isStorage(BlockState state) { return state.getBlock() instanceof MachineBlock block && block.kind()==MachineBlock.Kind.SEEDS; }
    private io.github.sudabake.appliedagristics.multiblock.PlanterStructure structure;
    public void bind(io.github.sudabake.appliedagristics.multiblock.PlanterStructure structure) { this.structure=structure; }
    public void unbind(io.github.sudabake.appliedagristics.multiblock.PlanterStructure previous) { if (structure==previous) { structure=null; leases.clear(); } }
    public boolean belongsTo(NetworkPlanter planter) { return structure != null && structure.controller()==planter && structure.formed(); }
    @Override protected boolean ownsNode() { return false; }
    @Override protected NetworkAccess accessModule() { return structure==null ? null : structure.access(); }
    @Override public void release() { if (structure!=null) structure.invalidate(); super.release(); }
    private final Map<AEItemKey, Long> seeds = new LinkedHashMap<>();
    private final Map<AEItemKey, Map<NetworkPlanter, Long>> leases = new HashMap<>();
    private boolean collecting;
    private boolean autoCollect;
    private boolean ejecting;
    private int collectFilter;
    public void setCollectFilter(int filter) {if(filter>=0 && filter<4){collectFilter=filter;changed();}}
    public boolean acceptsCollection(AEItemKey key) {
        return switch(collectFilter) {
            case 1 -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(key.getItem()).getNamespace().equals("minecraft");
            case 2 -> Crops.seed(key.toStack(1))!=null;
            case 3 -> io.github.sudabake.appliedagristics.biology.BioData.seed(key.toStack(1));
            default -> true;
        };
    }
    public void ejectSeeds() {
        autoCollect=false;setChanged();
        var grid=getMainNode().getGrid();if(grid==null || !getMainNode().isActive()) return;
        for(var key:List.copyOf(seeds.keySet())) {
            var owners=leases.get(key);
            if(owners!=null) for(var owner:List.copyOf(owners.keySet())) owner.pauseForSeedRemoval(key);
        }
        ejecting=true;
        try {
            var storage=grid.getStorageService().getInventory();var source=IActionSource.ofMachine(this);
            for(var key:List.copyOf(seeds.keySet())) {
                long sent=storage.insert(key,count(key),Actionable.MODULATE,source);
                if(sent>0) {long left=count(key)-sent;if(left==0) seeds.remove(key);else seeds.put(key,left);changed(key,-sent);}
            }
        } finally {ejecting=false;changed();}
    }
    public void toggleAutoCollect() {autoCollect=!autoCollect;changed();}
    private ListTag unresolved = new ListTag();
    private final appeng.api.storage.MEStorage networkInventory = new appeng.api.storage.MEStorage() {
        @Override public net.minecraft.network.chat.Component getDescription() { return getDisplayName(); }
        @Override public long insert(appeng.api.stacks.AEKey what, long amount, Actionable mode, IActionSource source) {
            if (!autoCollect || ejecting || source.machine().orElse(null) instanceof SeedStorage bank && bank.ejecting || !(what instanceof AEItemKey key) || amount <= 0 || !acceptsCollection(key) || !io.github.sudabake.appliedagristics.logic.Harvests.seed(key.toStack(1),level)) return 0;
            return mode == Actionable.SIMULATE ? Amounts.insertable(count(key), amount) : add(key, amount);
        }
        @Override public long extract(appeng.api.stacks.AEKey what, long amount, Actionable mode, IActionSource source) {
            return ejecting || collecting || source.machine().orElse(null) instanceof SeedStorage bank && bank.collecting || !(what instanceof AEItemKey key) ? 0 : extractFree(key, amount, mode);
        }
        @Override public void getAvailableStacks(appeng.api.stacks.KeyCounter out) {
            if (!collecting && !ejecting) seeds.forEach((key,count) -> { long free=count-used(key); if (free>0) out.add(key,free); });
        }
        @Override public boolean isPreferredStorageFor(appeng.api.stacks.AEKey key, IActionSource source) { return autoCollect && key instanceof AEItemKey item && acceptsCollection(item) && io.github.sudabake.appliedagristics.logic.Harvests.seed(item.toStack(1),level); }
    };
    @Override public void mountInventories(appeng.api.storage.IStorageMounts mounts) { mounts.mount(networkInventory, 100); }
    private void changed() { storedItemsChanged(null,0); }
    private void changed(AEItemKey key,long delta) { storedItemsChanged(key,delta); }
    public final IItemHandler port = items;
    public SeedStorage(BlockPos pos, BlockState state) { super(Agristics.SEED_BE.get(), pos, state, 1); }
    @Override protected boolean valid(int slot, ItemStack stack) { return io.github.sudabake.appliedagristics.logic.Harvests.seed(stack,level); }
    public Set<AEItemKey> keys() { return Set.copyOf(seeds.keySet()); }
    public long count(AEItemKey key) { return seeds.getOrDefault(key, 0L); }
    public long used(AEItemKey key) {
        var users = leases.get(key);
        if (users == null) return 0;
        users.keySet().removeIf(owner -> !owner.leaseValid(this, key));
        return users.values().stream().mapToLong(Long::longValue).sum();
    }
    public boolean claim(AEItemKey key, NetworkPlanter owner) {
        return claim(key, owner, 1) > 0;
    }
    public long claim(AEItemKey key, NetworkPlanter owner, long requested) {
        if (!belongsTo(owner)) return 0;
        used(key);
        var users = leases.computeIfAbsent(key, k -> new HashMap<>());
        long existing = users.getOrDefault(owner, 0L);
        long others = users.values().stream().mapToLong(Long::longValue).sum() - existing;
        long accepted = Math.min(Math.max(0, requested), Math.max(0, count(key) - others));
        if (accepted == 0) users.remove(owner); else users.put(owner, accepted);
        if (accepted != existing) changed(key,existing-accepted);
        return accepted;
    }
    public void unclaim(AEItemKey key, NetworkPlanter owner) {
        var users = leases.get(key);
        if (users != null) {
            var released=users.remove(owner);
            if (released!=null) changed(key,released);
        }
    }
    public long add(AEItemKey key, long requested) {
        if(!acceptsCollection(key)) return 0;
        long accepted = Amounts.insertable(count(key), requested);
        if (accepted > 0) { seeds.merge(key, accepted, Long::sum); changed(key,accepted); }
        return accepted;
    }
    public long extractFree(AEItemKey key, long amount, Actionable mode) {
        long taken = amount <= 0 ? 0 : Amounts.extractable(count(key), used(key), amount);
        if (taken > 0 && mode == Actionable.MODULATE) { long left = count(key) - taken; if (left == 0) seeds.remove(key); else seeds.put(key, left); changed(key,-taken); }
        return taken;
    }
    public void collect(Player player) { collect(IActionSource.ofPlayer(player,this)); }
    private void collect(IActionSource source) {
        var grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive()) return;
        var storage = grid.getStorageService().getInventory();
        collecting = true;
        try { for (var entry : storage.getAvailableStacks()) {
            if (entry.getKey() instanceof AEItemKey key && acceptsCollection(key) && io.github.sudabake.appliedagristics.logic.Harvests.seed(key.toStack(1),level)) {
                long room = Amounts.insertable(count(key), entry.getLongValue());
                long extracted = storage.extract(key, room, Actionable.MODULATE, source);
                add(key, extracted);
            }
        } } finally { collecting = false; changed(); }
    }
    public void withdraw(AEItemKey key, Player player) {
        int amount = (int) Amounts.extractable(count(key), used(key), key.getMaxStackSize());
        if (amount == 0) return;
        var stack = key.toStack(amount);
        player.getInventory().add(stack);
        // Inventory.add mutates the remainder; only remove what was actually accepted.
        int accepted = amount - stack.getCount();
        if (accepted > 0) { seeds.put(key, count(key) - accepted); if (count(key) == 0) seeds.remove(key); changed(key,-accepted); }
    }
    @Override public void step() {
        var stack = items.getStackInSlot(0);
        if (!stack.isEmpty()) {
            var key = AEItemKey.of(stack); long accepted = add(key, stack.getCount());
            if (accepted > 0) items.extractItem(0, (int) accepted, false);
        }
        if(autoCollect) collect(IActionSource.ofMachine(this));
        status = structure==null || !structure.formed() ? "unformed" : getMainNode().isActive() ? "online" : "offline";
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("seeds",StoredItemCounts.save(seeds,unresolved,registries));
        tag.putBoolean("autoCollect",autoCollect); tag.putInt("collectFilter",collectFilter);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        autoCollect=tag.getBoolean("autoCollect"); collectFilter=Math.clamp(tag.getInt("collectFilter"),0,3);
        seeds.clear(); leases.clear();
        unresolved=StoredItemCounts.load(tag.getList("seeds",Tag.TAG_COMPOUND),seeds,registries);
    }
    @Override public CompoundTag snapshot(int page, String search) {
        var tag = super.snapshot(page, search);
        tag.putInt("unresolved",unresolved.size());
        tag.putBoolean("autoCollect",autoCollect); tag.putInt("collectFilter",collectFilter);
        var filtered = seeds.keySet().stream().filter(k -> count(k) > 0)
            .sorted(Comparator.comparing(k -> Cultivations.id(k.toStack(1),level).toString())).toList();
        int scrollRows=Math.max(1,(filtered.size()+2)/3-3);
        var rows = new ListTag(); int start = Math.min(scrollRows-1,Math.max(0,page))*3;
        for (var key : filtered.subList(start, Math.min(start + 12, filtered.size()))) {
            var row = new CompoundTag(); row.put("seed", key.toStack(1).save(level.registryAccess())); row.putString("id", Cultivations.id(key.toStack(1),level).toString());
            row.putString("name", key.getDisplayName().getString()); row.putLong("count", count(key)); row.putLong("used", used(key)); rows.add(row);
        }
        tag.put("rows", rows); tag.putInt("pages",scrollRows); return tag;
    }
}
