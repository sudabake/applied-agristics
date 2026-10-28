package io.github.sudabake.appliedagristics.block;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.logic.Amounts;
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
    private final appeng.api.storage.MEStorage networkInventory = new appeng.api.storage.MEStorage() {
        @Override public net.minecraft.network.chat.Component getDescription() { return getDisplayName(); }
        @Override public long insert(appeng.api.stacks.AEKey what, long amount, Actionable mode, IActionSource source) {
            if (!(what instanceof AEItemKey key) || amount <= 0 || !io.github.sudabake.appliedagristics.logic.Harvests.seed(key.toStack(1),level)) return 0;
            return mode == Actionable.SIMULATE ? Amounts.insertable(count(key), amount) : add(key, amount);
        }
        @Override public long extract(appeng.api.stacks.AEKey what, long amount, Actionable mode, IActionSource source) {
            return collecting || !(what instanceof AEItemKey key) ? 0 : extractFree(key, amount, mode);
        }
        @Override public void getAvailableStacks(appeng.api.stacks.KeyCounter out) {
            if (!collecting) seeds.forEach((key,count) -> { long free=count-used(key); if (free>0) out.add(key,free); });
        }
        @Override public boolean isPreferredStorageFor(appeng.api.stacks.AEKey key, IActionSource source) { return key instanceof AEItemKey item && io.github.sudabake.appliedagristics.logic.Harvests.seed(item.toStack(1),level); }
    };
    @Override public void mountInventories(appeng.api.storage.IStorageMounts mounts) { mounts.mount(networkInventory, 100); }
    private void changed() { setChanged(); var grid = getMainNode().getGrid(); if (grid != null) grid.getStorageService().invalidateCache(); }
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
        if (accepted != existing) changed();
        return accepted;
    }
    public void unclaim(AEItemKey key, NetworkPlanter owner) {
        var users = leases.get(key); if (users != null && users.remove(owner) != null) changed();
    }
    public long add(AEItemKey key, long requested) {
        long accepted = Amounts.insertable(count(key), requested);
        if (accepted > 0) { seeds.merge(key, accepted, Long::sum); changed(); }
        return accepted;
    }
    public long extractFree(AEItemKey key, long amount, Actionable mode) {
        long taken = amount <= 0 ? 0 : Amounts.extractable(count(key), used(key), amount);
        if (taken > 0 && mode == Actionable.MODULATE) { long left = count(key) - taken; if (left == 0) seeds.remove(key); else seeds.put(key, left); changed(); }
        return taken;
    }
    public void collect(Player player) {
        var grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive()) return;
        var storage = grid.getStorageService().getInventory();
        var source = IActionSource.ofPlayer(player, this);
        collecting = true;
        try { for (var entry : storage.getAvailableStacks()) {
            if (entry.getKey() instanceof AEItemKey key && io.github.sudabake.appliedagristics.logic.Harvests.seed(key.toStack(1),level)) {
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
        if (accepted > 0) { seeds.put(key, count(key) - accepted); if (count(key) == 0) seeds.remove(key); changed(); }
    }
    @Override public void step() {
        if(!getBlockState().is(Agristics.SEEDS.get()) && level!=null && !level.isClientSide) {
            var saved=saveWithFullMetadata(level.registryAccess());var state=Agristics.SEEDS.get().withPropertiesOf(getBlockState());
            level.setBlockAndUpdate(worldPosition,state);
            if(level.getBlockEntity(worldPosition) instanceof SeedStorage replacement){replacement.loadWithComponents(saved,level.registryAccess());replacement.setChanged();}
            return;
        }
        var stack = items.getStackInSlot(0);
        if (!stack.isEmpty()) {
            var key = AEItemKey.of(stack); long accepted = add(key, stack.getCount());
            if (accepted > 0) items.extractItem(0, (int) accepted, false);
        }
        status = structure==null || !structure.formed() ? "unformed" : getMainNode().isActive() ? "online" : "offline";
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); var list = new ListTag();
        seeds.forEach((key, count) -> { var e = new CompoundTag(); e.put("key", key.toTag(registries)); e.putLong("count", count); list.add(e); });
        tag.put("seeds", list);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); seeds.clear(); leases.clear();
        // Read the old map or merge every Alpha20 cell and overflow entry, without imposing a new limit.
        for(var name:tag.getInt("seedStorageVersion")==2?List.of("seedCells","overflow"):List.of("seeds"))
            for(var raw:tag.getList(name,Tag.TAG_COMPOUND)) {
                var e=(CompoundTag)raw;var key=AEItemKey.fromTag(registries,e.getCompound("key"));long count=e.getLong("count");
                if(key!=null && count>0)seeds.merge(key,count,Math::addExact);
            }
    }
    @Override public CompoundTag snapshot(int page, String search) {
        var tag = super.snapshot(page, search);
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
