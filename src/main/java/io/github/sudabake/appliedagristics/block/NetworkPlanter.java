package io.github.sudabake.appliedagristics.block;

import appeng.api.config.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.logic.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class NetworkPlanter extends GridMachine {
    private boolean poweredVisual;
    public boolean poweredVisual() { return poweredVisual; }
    @Override public void updateWorkVisual() {
        boolean next=level!=null && !level.isClientSide && structure.formed() && getMainNode().isActive();
        boolean changed=poweredVisual!=next; poweredVisual=next;
        super.updateWorkVisual();
        if(changed && level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        var tag=super.getUpdateTag(registries);tag.putBoolean("poweredVisual",poweredVisual);return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries) {
        super.handleUpdateTag(tag,registries);poweredVisual=tag.getBoolean("poweredVisual");
    }
    public final io.github.sudabake.appliedagristics.multiblock.PlanterStructure structure = new io.github.sudabake.appliedagristics.multiblock.PlanterStructure(this);
    @Override protected boolean ownsNode() { return false; }
    @Override protected NetworkAccess accessModule() { return structure.access(); }
    @Override public void ensureNode() { structure.refresh(); }
    @Override public void onLoad() { super.onLoad(); structure.register(); }
    public java.util.List<io.github.sudabake.appliedagristics.multiblock.SeedBank> seedStores() { return structure.seedBanks(); }
    public void stopForStructure() {
        tasks.values().forEach(t -> { releaseTask(t); t.status="unformed"; });
        status="unformed";
        updateWorkVisual();
    }
    private record SeedLocation(io.github.sudabake.appliedagristics.multiblock.SeedBank drive, AEItemKey key) {}
    public static final class Task {
        public boolean enabled; public long target = -1; public double progress; public String status = "disabled";
        public long occupied; public double power;
        long heartbeat;
        final Map<SeedLocation, Long> allocations = new LinkedHashMap<>();
        final Map<AEItemKey, Long> pending = new LinkedHashMap<>();
        final Map<AEItemKey, Double> fractions = new LinkedHashMap<>();
    }
    public final Map<ResourceLocation, Task> tasks = new LinkedHashMap<>();
    private int fairnessCursor;
    private boolean directNetworkOutput;
    public boolean directNetworkOutput() { return directNetworkOutput; }
    public void toggleDirectNetworkOutput() { directNetworkOutput=!directNetworkOutput; setChanged(); }
    public NetworkPlanter(BlockPos pos, BlockState state) { super(Agristics.NETWORK_BE.get(), pos, state, 4); }
    @Override protected boolean valid(int slot, ItemStack stack) {
        return false;
    }
    public int parallel() { return ProductionMath.moduleParallel(structure.parallelModules(), Balance.PARALLEL_MAX.get()); }
    public double speed() { return ProductionMath.moduleSpeed(structure.speedModules(), Balance.SPEED_MAX.get()); }
    public boolean conversion() { return structure.conversion(); }
    @Override public void open(net.minecraft.server.level.ServerPlayer player) {
        // Old saves keep their inventory. Return legacy cards/soil on opening instead of hiding or deleting them.
        for (int i=0; i<items.getSlots(); i++) {
            var stack=items.extractItem(i,items.getStackInSlot(i).getCount(),false);
            if (!stack.isEmpty()) { player.getInventory().add(stack); if (!stack.isEmpty()) player.drop(stack,false); }
        }
        super.open(player);
    }
    public void configure(ResourceLocation id, boolean enabled, long target) {
        boolean owned=seedStores().stream().anyMatch(bank->bank.keys().stream().anyMatch(key->bank.count(key)>0 && Cultivations.id(key.toStack(1),level).equals(id) && Cultivations.seed(key.toStack(1),(ServerLevel)level,worldPosition)!=null));
        if (!owned) return;
        if (target < -1) return;
        var task = tasks.computeIfAbsent(id, k -> new Task()); task.enabled = enabled; task.target = target;
        if (!enabled) { releaseTask(task); task.status = "disabled"; } setChanged();
    }
    public void configureAll(boolean enabled) {
        var owned = new HashSet<ResourceLocation>();
        for (var drive : seedStores()) for (var key : drive.keys())
            if (drive.count(key) > 0) owned.add(Cultivations.id(key.toStack(1),level));
        for (var id : owned) configure(id, enabled, tasks.containsKey(id) ? tasks.get(id).target : -1);
    }
    private void releaseTask(Task task) {
        task.allocations.keySet().forEach(a -> a.drive.unclaim(a.key, this));
        task.allocations.clear(); task.occupied = 0; task.power = 0;
    }
    public boolean leaseValid(io.github.sudabake.appliedagristics.multiblock.SeedBank drive, AEItemKey seed) {
        var t = tasks.get(Cultivations.id(seed.toStack(1),level));
        return t != null && !isRemoved() && getMainNode().isActive()
            && drive.belongsTo(this) && t.enabled
            && t.allocations.containsKey(new SeedLocation(drive, seed)) && level.getGameTime() - t.heartbeat <= 40;
    }
    @Override public void release() { structure.unload(); tasks.values().forEach(this::releaseTask); super.release(); }
    @Override public void dropContents() {
        super.dropContents();
        var server=(ServerLevel)level;
        for(var task:tasks.values()) if(!task.pending.isEmpty()) {
            DeferredDrops.get(server).enqueue(server,worldPosition,task.pending);
            task.pending.clear();
        }
        setChanged();
    }
    private void reserve(Task task, List<SeedLocation> locations) {
        var previous = new HashSet<>(task.allocations.keySet()); task.heartbeat = level.getGameTime();
        long left = parallel();
        for (var location : locations) {
            task.allocations.putIfAbsent(location, 0L);
            long count = location.drive.claim(location.key, this, left);
            if (count > 0) task.allocations.put(location, count); else task.allocations.remove(location);
            previous.remove(location); left -= count;
        }
        for (var obsolete : previous) { obsolete.drive.unclaim(obsolete.key, this); task.allocations.remove(obsolete); }
        task.occupied = parallel() - left;
    }
    @Override public void step() {
        if (!structure.formed()) { stopForStructure(); status=structure.status(); return; }
        var grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive()) { status = "offline"; tasks.values().forEach(t -> { releaseTask(t); t.status = "offline"; }); return; }
        var storage = grid.getStorageService().getInventory(); var source = IActionSource.ofMachine(this);
        var destination=structure.outputs();
        var available = storage.getAvailableStacks(); var energy = grid.getEnergyService();
        var enabled = tasks.entrySet().stream().filter(e -> e.getValue().enabled || !e.getValue().pending.isEmpty()).toList();
        Map<ResourceLocation, List<SeedLocation>> locations = new HashMap<>();
        for (var drive : seedStores()) for (var key : drive.keys()) {
            locations.computeIfAbsent(Cultivations.id(key.toStack(1),level), id -> new ArrayList<>()).add(new SeedLocation(drive, key));
        }
        int running = 0;
        for (int i = 0; i < enabled.size(); i++) {
            var entry = enabled.get(Math.floorMod(i + fairnessCursor, enabled.size())); var task = entry.getValue(); task.power = 0;
            if (!task.pending.isEmpty()) {
                if (!flush(task, destination, source)) { task.status = blockedOutput(task.pending.keySet()); continue; }
                available = storage.getAvailableStacks();
            }
            if (!task.enabled) { task.status = "disabled"; continue; }
            var inputs=locations.getOrDefault(entry.getKey(),List.of());
            var crop = inputs.isEmpty()?null:Cultivations.seed(inputs.getFirst().key.toStack(1),(ServerLevel)level,worldPosition);
            if (crop == null) { task.status = "missing_inputs"; releaseTask(task); continue; }
            reserve(task, locations.getOrDefault(crop.getId(), List.of()));
            if (task.occupied == 0) { task.status = "missing_seed"; continue; }
            var seed=task.allocations.keySet().iterator().next().key.toStack(1);
            if (!Harvests.canGrowNetwork((ServerLevel)level,worldPosition,seed)) { task.status="wrong_farmland"; continue; }
            int duration=CropSources.ticks(level,worldPosition,seed,Harvests.networkSoil(level,seed),Balance.LATE_TICKS.get());
            long batchLimit=Math.min(Harvests.maximumBatch(level,seed),structure.vaults().isEmpty() || !Crops.essence(crop.essenceStack()) ? 64L : Integer.MAX_VALUE);
            var output = AEItemKey.of(crop.essenceStack()); long stock = available.get(output);
            if (Amounts.harvestSize(stock, task.target, 1) == 0) { task.status = "target_reached"; continue; }
            if (destination.insert(output, 1, Actionable.SIMULATE, source) == 0) { task.status = blockedOutput(List.of(output)); continue; }
            double perTick = ProductionMath.power(task.occupied, speed(), Balance.POWER.get(), (double)Balance.PARALLEL_MAX.get() * Balance.SPEED_MAX.get(), Balance.FULL_POWER.get());
            double requestedWork=5*task.occupied*speed();
            double work=Math.min(requestedWork,batchLimit*(double)duration);
            double cost = perTick * 5 * work/requestedWork;
            if (energy.extractAEPower(cost, Actionable.SIMULATE, PowerMultiplier.CONFIG) + 0.000001 < cost) { task.status = "no_power"; continue; }
            double paid = energy.extractAEPower(cost, Actionable.MODULATE, PowerMultiplier.CONFIG);
            task.progress += work * Math.clamp(paid / cost, 0, 1);
            task.power = paid / 5; task.status = "growing"; running++;
            long cycles = Math.min((long)(task.progress / duration),batchLimit);
            if (cycles > 0) {
                task.progress -= cycles * (double)duration;
                Harvests.networkBatch((ServerLevel)level, worldPosition, seed, cycles, task.fractions).forEach((key, count) -> task.pending.merge(key, count, Math::addExact));
                {
                    var key = AEItemKey.of(seed); long extra = task.pending.getOrDefault(key, 0L);
                    // Only this new batch is eligible for conversion. Stored seeds are never consumed.
                    long needed = Math.max(0, parallel() - task.occupied);
                    long toStore = conversion() ? Math.min(needed, extra) : extra;
                    for (var location : locations.getOrDefault(crop.getId(), List.of())) {
                        long accepted = location.drive.add(key, toStore); extra -= accepted; toStore -= accepted; if (toStore == 0) break;
                    }
                    reserve(task, locations.getOrDefault(crop.getId(), List.of()));
                    // Edible seeds can also be the product. Their remaining net harvest
                    // already has the output key and must not be merged into itself then removed.
                    if (conversion() && task.occupied == parallel() && !output.equals(key)) {
                        if (extra > 0) task.pending.merge(output, extra, Math::addExact);
                        extra = 0;
                    }
                    if (extra == 0) task.pending.remove(key); else task.pending.put(key, extra);
                }
                if (!flush(task, destination, source)) task.status = blockedOutput(task.pending.keySet());
                available = storage.getAvailableStacks();
            }
        }
        if (!enabled.isEmpty()) fairnessCursor = (fairnessCursor + 1) % enabled.size();
        status = running > 0 ? "growing" : "idle"; setChanged();
    }
    private boolean flush(Task task, appeng.api.storage.MEStorage storage, IActionSource source) {
        for (var e : task.pending.entrySet()) e.setValue(e.getValue() - storage.insert(e.getKey(), e.getValue(), Actionable.MODULATE, source));
        task.pending.values().removeIf(n -> n <= 0); setChanged(); return task.pending.isEmpty();
    }
    private String blockedOutput(java.util.Collection<AEItemKey> outputs) {
        boolean ordinary=outputs.stream().anyMatch(k->!Crops.essence(k.toStack(1)));
        if (ordinary && directNetworkOutput) return "network_output_full";
        return structure.ports().stream().noneMatch(p->!p.isInput()) && ordinary ? "requires_output_port" : "output_full";
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.putBoolean("directNetworkOutput",directNetworkOutput); var list = new ListTag();
        tasks.forEach((id, t) -> {
            var e = new CompoundTag(); e.putString("id", id.toString()); e.putBoolean("enabled", t.enabled); e.putLong("target", t.target); e.putDouble("work", t.progress);
            var pending = new ListTag(); t.pending.forEach((key, count) -> { var p = new CompoundTag(); p.put("key", key.toTag(registries)); p.putLong("count", count); pending.add(p); }); e.put("batch", pending);
            var fractions = new ListTag(); t.fractions.forEach((key, amount) -> { var f = new CompoundTag(); f.put("key", key.toTag(registries)); f.putDouble("fraction", amount); fractions.add(f); }); e.put("fractions", fractions); list.add(e);
        }); tag.put("tasks", list);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        directNetworkOutput=tag.getBoolean("directNetworkOutput");
        if (Crops.farmland(items.getStackInSlot(0)) && items.getStackInSlot(3).isEmpty()) {
            items.setStackInSlot(3, items.getStackInSlot(0)); items.setStackInSlot(0, ItemStack.EMPTY);
        }
        tasks.clear();
        for (var raw : tag.getList("tasks", Tag.TAG_COMPOUND)) {
            var e = (CompoundTag)raw; var id = ResourceLocation.tryParse(e.getString("id")); if (id == null) continue;
            var t = new Task(); t.enabled = e.getBoolean("enabled"); t.target = e.contains("target") ? Math.max(-1, e.getLong("target")) : -1; t.progress = Math.max(0, e.contains("work") ? e.getDouble("work") : e.getInt("progress"));
            for (var item : e.getList("batch", 10)) { var p = (CompoundTag)item; var key = AEItemKey.fromTag(registries, p.getCompound("key")); if (key != null && p.getLong("count") > 0) t.pending.put(key, p.getLong("count")); }
            for (var item : e.getList("pending", 10)) { var stack = ItemStack.parseOptional(registries, (CompoundTag)item); if (!stack.isEmpty()) t.pending.merge(AEItemKey.of(stack), (long)stack.getCount(), Math::addExact); }
            for (var item : e.getList("fractions", 10)) { var f = (CompoundTag)item; var key = AEItemKey.fromTag(registries, f.getCompound("key")); if (key != null) t.fractions.put(key, Math.clamp(f.getDouble("fraction"), 0, Math.nextDown(1.0))); }
            tasks.put(id, t);
        }
    }
    @Override public CompoundTag snapshot(int page, String search) {
        var tag = super.snapshot(page, ""); var grid = getMainNode().getGrid();
        var inventory = grid == null ? null : grid.getStorageService().getInventory().getAvailableStacks();
        var owned = new LinkedHashMap<ResourceLocation, Cultivations.Entry>();
        for (var drive : seedStores()) for (var key : drive.keys()) if (drive.count(key) > 0) {
            var crop = Cultivations.seed(key.toStack(1), (ServerLevel)level, worldPosition); if (crop != null) owned.put(crop.getId(), crop);
        }
        // The filter belongs to the viewer/menu; it must never disable hidden crops.
        boolean hideOrdinary="hide_ordinary".equals(search);
        var crops = owned.values().stream().filter(c -> !hideOrdinary || Crops.essence(c.essenceStack())).sorted(Comparator.comparing(c -> c.getId().toString())).toList();
        var rows = new ListTag(); int start = Math.min(Math.max(0, crops.size() - 4), Math.max(0, page));
        for (var crop : crops.subList(start, Math.min(start + 4, crops.size()))) {
            var t = tasks.getOrDefault(crop.getId(), new Task()); var row = new CompoundTag();
            row.putString("id", crop.getId().toString()); row.putString("name", crop.getDisplayName().getString());
            row.put("seed", crop.seedStack().save(level.registryAccess())); row.put("output", crop.essenceStack().save(level.registryAccess()));
            row.putLong("stock", inventory == null ? 0 : inventory.get(AEItemKey.of(crop.essenceStack())));
            row.putLong("target", t.target); row.putBoolean("enabled", t.enabled); row.putString("status", t.status);
            long usable = 0;
            for (var drive : seedStores()) for (var key : drive.keys()) if (Cultivations.id(key.toStack(1),level).equals(crop.getId())) {
                long freeForThis = drive.count(key) - drive.used(key) + t.allocations.getOrDefault(new SeedLocation(drive, key), 0L);
                usable += Math.min(Long.MAX_VALUE - usable, Math.max(0, freeForThis));
            }
            long nextUsed = Math.min(usable, ProductionMath.moduleParallel(structure.parallelModules() + 1, Balance.PARALLEL_MAX.get()));
            row.putDouble("requiredPower", ProductionMath.power(t.occupied, speed(), Balance.POWER.get(), (double)Balance.PARALLEL_MAX.get() * Balance.SPEED_MAX.get(), Balance.FULL_POWER.get()));
            row.putDouble("nextSpeedPower", ProductionMath.power(t.occupied, ProductionMath.moduleSpeed(structure.speedModules() + 1, Balance.SPEED_MAX.get()), Balance.POWER.get(), (double)Balance.PARALLEL_MAX.get() * Balance.SPEED_MAX.get(), Balance.FULL_POWER.get()));
            row.putDouble("nextParallelPower", ProductionMath.power(nextUsed, speed(), Balance.POWER.get(), (double)Balance.PARALLEL_MAX.get() * Balance.SPEED_MAX.get(), Balance.FULL_POWER.get()));
            row.putLong("count", parallel()); row.putLong("used", t.occupied); row.putDouble("power", t.power); rows.add(row);
        }
        tag.put("rows", rows); tag.putInt("pages", Math.max(1, crops.size() - 3)); tag.putInt("parallel", parallel()); tag.putDouble("speed", speed());
        tag.putBoolean("hideOrdinary",hideOrdinary);tag.putBoolean("directNetworkOutput",directNetworkOutput);
        tag.putDouble("power", tasks.values().stream().mapToDouble(t -> t.power).sum());
        tag.putBoolean("formed",structure.formed()); tag.putInt("speedModules",structure.speedModules()); tag.putInt("parallelModules",structure.parallelModules()); tag.putBoolean("conversion",conversion());
        if (!structure.formed()) tag.putString("status",structure.status());
        return tag;
    }
}

