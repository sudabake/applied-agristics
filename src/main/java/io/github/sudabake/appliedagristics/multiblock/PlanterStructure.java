package io.github.sudabake.appliedagristics.multiblock;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.*;

/** Structure state is derived, not persisted; item inventories remain in their own block entities. */
@EventBusSubscriber(modid = Agristics.ID)
public final class PlanterStructure {
    private static final Map<Level, Set<PlanterStructure>> LOADED = new WeakHashMap<>();
    private final NetworkPlanter controller;
    private List<BlockPos> positions = List.of();
    private List<SeedStorage> seeds = List.of();
    private List<ItemPort> ports = List.of();
    private List<EssenceVault> vaults = List.of();
    private NetworkAccess access;
    private boolean dirty = true, checking;
    private PlanterLayout.Result result = new PlanterLayout.Result(false, "unformed", 0, 0, 0);
    public PlanterStructure(NetworkPlanter controller) { this.controller = controller; }
    public NetworkPlanter controller() { return controller; }
    public String status() { refresh(); return result.status(); }
    public boolean formed() { refresh(); return result.formed(); }
    public int speedModules() { refresh(); return result.speed(); }
    public int parallelModules() { refresh(); return result.parallel(); }
    public boolean conversion() { refresh(); return result.conversion() > 0; }
    public List<SeedStorage> seeds() { refresh(); return seeds; }
    public List<ItemPort> ports() { refresh(); return ports; }
    public List<EssenceVault> vaults() { refresh(); return vaults; }
    public List<SeedBank> seedBanks() {
        refresh();
        if (!seeds.isEmpty()) return List.copyOf(seeds);
        return ports.stream().filter(ItemPort::isInput).map(port -> (SeedBank) port).toList();
    }
    private final appeng.api.storage.MEStorage outputs = new appeng.api.storage.MEStorage() {
        @Override public net.minecraft.network.chat.Component getDescription() { return controller.getDisplayName(); }
        @Override public long insert(appeng.api.stacks.AEKey key,long amount,appeng.api.config.Actionable mode,appeng.api.networking.security.IActionSource source) {
            if (!formed() || !(key instanceof appeng.api.stacks.AEItemKey item) || amount<=0) return 0;
            if (controller.directNetworkOutput() && !io.github.sudabake.appliedagristics.Crops.essence(item.toStack(1))) {
                var node=controller.getMainNode();var grid=node.getGrid();
                return grid==null || !node.isActive() ? 0 : grid.getStorageService().getInventory().insert(key,amount,mode,source);
            }
            boolean simulate=mode==appeng.api.config.Actionable.SIMULATE; long left=amount;
            for (var vault:vaults.stream().sorted(Comparator.comparingInt(EssenceVault::getPriority).reversed()).toList()) { left-=vault.storeProduced(item,left,simulate); if (left==0) break; }
            for (var port:ports) if (!port.isInput() && left>0) left-=port.insertLocal(item,left,simulate);
            return amount-left;
        }
        @Override public long extract(appeng.api.stacks.AEKey key,long amount,appeng.api.config.Actionable mode,appeng.api.networking.security.IActionSource source) { return 0; }
        @Override public void getAvailableStacks(appeng.api.stacks.KeyCounter out) { }
    };
    public appeng.api.storage.MEStorage outputs() { return outputs; }
    public NetworkAccess access() { refresh(); return access; }

    private static Set<PlanterStructure> structures(Level level) { return LOADED.computeIfAbsent(level, l -> new LinkedHashSet<>()); }
    public void register() {
        var level = controller.getLevel();
        if (level != null && !level.isClientSide && !controller.isRemoved() && level.getBlockEntity(controller.getBlockPos())==controller) structures(level).add(this);
    }
    public void unload() {
        invalidate();
        var level = controller.getLevel();
        var entries = LOADED.get(level); if (entries != null) entries.remove(this);
    }
    public static PlanterStructure ownerAt(Level level, BlockPos pos) {
        for (var structure : List.copyOf(LOADED.getOrDefault(level, Set.of())))
            if (structure.positions.contains(pos) && structure.result.formed()) return structure;
        return null;
    }
    public static void changed(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (var structure : List.copyOf(LOADED.getOrDefault(level, Set.of()))) {
            // Notifications request validation; they do not prove that the structure broke.
            if (structure.contains(pos)) structure.dirty=true;
        }
    }
    private boolean contains(BlockPos pos) {
        var facing=controller.getBlockState().getValue(PlanterPartBlock.FACING);
        var right=facing.getClockWise(); var back=facing.getOpposite();
        var delta=pos.subtract(controller.getBlockPos());
        int x=delta.getX()*right.getStepX()+delta.getZ()*right.getStepZ()+1;
        int z=delta.getX()*back.getStepX()+delta.getZ()*back.getStepZ();
        int y=delta.getY()+1;
        return x>=0 && x<PlanterLayout.WIDTH && z>=0 && z<PlanterLayout.DEPTH && y>=0 && y<PlanterLayout.HEIGHT;
    }
    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event) { chunkChanged(event); }
    @SubscribeEvent public static void chunkUnloaded(ChunkEvent.Unload event) { chunkChanged(event); }
    private static void chunkChanged(ChunkEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        var chunk = event.getChunk().getPos();
        for (var structure : List.copyOf(LOADED.getOrDefault(level, Set.of()))) {
            var c = structure.controller.getBlockPos();
            var facing=structure.controller.getBlockState().getValue(PlanterPartBlock.FACING);
            var origin=c.relative(facing.getClockWise(),-1);
            var far=origin.relative(facing.getClockWise(),PlanterLayout.WIDTH-1).relative(facing.getOpposite(),PlanterLayout.DEPTH-1);
            if (chunk.x >= (Math.min(origin.getX(),far.getX()) >> 4) && chunk.x <= (Math.max(origin.getX(),far.getX()) >> 4)
                && chunk.z >= (Math.min(origin.getZ(),far.getZ()) >> 4) && chunk.z <= (Math.max(origin.getZ(),far.getZ()) >> 4)) {
                if (event instanceof ChunkEvent.Unload) structure.invalidate();
                else structure.dirty=true;
            }
        }
    }
    @SubscribeEvent public static void levelUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            for (var structure : List.copyOf(LOADED.getOrDefault(level, Set.of()))) structure.invalidate();
            LOADED.remove(level);
        }
    }
    public void invalidate() {
        if (checking) return;
        dirty = true;
        if (!result.formed()) return;
        checking = true;
        try {
            clearFormation();
        } finally { checking=false; }
    }
    private void clearFormation() {
        if (!result.formed()) return;
        result = new PlanterLayout.Result(false, "unformed", 0, 0, 0);
        var previous = access; access = null;
        controller.stopForStructure();
        if (previous != null) previous.unbind(this);
        seeds.forEach(seed -> seed.unbind(this)); seeds = List.of();
        ports.forEach(port -> port.unbind(this)); ports=List.of();
        vaults.forEach(vault -> vault.unbind(this)); vaults=List.of();
        var level = controller.getLevel();
        if (level != null) for (var pos : positions) {
            if (!level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (state.hasProperty(PlanterPartBlock.FORMED) && state.getValue(PlanterPartBlock.FORMED))
                level.setBlock(pos, state.setValue(PlanterPartBlock.FORMED, false).setValue(PlanterPartBlock.CELL,0), Block.UPDATE_CLIENTS);
        }
        positions = List.of();
    }
    private void reject(String status) { clearFormation(); result=invalid(status); }
    public void refresh() {
        var level = controller.getLevel();
        if (checking || level == null || level.isClientSide || controller.isRemoved() || level.getBlockEntity(controller.getBlockPos())!=controller) return;
        register(); if (!dirty) return;
        checking = true;
        try {
            dirty = false;
            var facing = controller.getBlockState().getValue(PlanterPartBlock.FACING);
            Direction right = facing.getClockWise(), back = facing.getOpposite();
            var origin = controller.getBlockPos().below().relative(right, -1);
            var cells = new PlanterLayout.Role[PlanterLayout.WIDTH * PlanterLayout.DEPTH * PlanterLayout.HEIGHT];
            var members = new ArrayList<BlockPos>(); var foundSeeds = new ArrayList<SeedStorage>();
            var foundPorts = new ArrayList<ItemPort>(); var foundVaults = new ArrayList<EssenceVault>(); NetworkAccess foundAccess = null;
            for (int y=0; y<PlanterLayout.HEIGHT; y++) for (int z=0; z<PlanterLayout.DEPTH; z++) for (int x=0; x<PlanterLayout.WIDTH; x++) {
                var pos = origin.relative(right,x).relative(back,z).above(y);
                if (!level.hasChunkAt(pos)) { reject("structure_unloaded"); return; }
                var owner = ownerAt(level,pos);
                if (owner != null && owner != this) { reject("structure_overlap"); return; }
                var state = level.getBlockState(pos); cells[PlanterLayout.index(x,y,z)] = role(state); members.add(pos);
                if (SeedStorage.isStorage(state)) {
                    if (!(level.getBlockEntity(pos) instanceof SeedStorage seed)) { dirty=true; reject("unformed"); return; }
                    foundSeeds.add(seed);
                }
                if (state.is(Agristics.NETWORK_ACCESS.get())) {
                    if (!(level.getBlockEntity(pos) instanceof NetworkAccess node)) { dirty=true; reject("unformed"); return; }
                    foundAccess=node;
                }
                if (state.is(Agristics.INPUT_PORT.get()) || state.is(Agristics.OUTPUT_PORT.get())) {
                    if (!(level.getBlockEntity(pos) instanceof ItemPort port)) { dirty=true; reject("unformed"); return; }
                    foundPorts.add(port);
                }
                if (state.is(Agristics.ESSENCE_VAULT.get())) {
                    if (!(level.getBlockEntity(pos) instanceof EssenceVault vault)) { dirty=true; reject("unformed"); return; }
                    foundVaults.add(vault);
                }
            }
            var validated = PlanterLayout.validate(i -> cells[i]);
            if (!validated.formed()) { reject(validated.status()); return; }
            boolean sameBindings=result.formed() && access==foundAccess && seeds.equals(foundSeeds) && ports.equals(foundPorts) && vaults.equals(foundVaults);
            if (result.formed() && !sameBindings) clearFormation();
            result=validated;
            positions=List.copyOf(members); seeds=List.copyOf(foundSeeds); ports=List.copyOf(foundPorts); vaults=List.copyOf(foundVaults); access=foundAccess;
            if (!sameBindings) { seeds.forEach(seed -> seed.bind(this)); ports.forEach(port -> port.bind(this)); vaults.forEach(vault -> vault.bind(this)); access.bind(this); }
            for (int cell=0; cell<positions.size(); cell++) {
                var pos=positions.get(cell);
                var state = level.getBlockState(pos);
                if (state.hasProperty(PlanterPartBlock.FORMED)) {
                    var formed=state.setValue(PlanterPartBlock.FORMED,true).setValue(PlanterPartBlock.CELL,cell).setValue(PlanterPartBlock.FACING,facing);
                    if (state!=formed) level.setBlock(pos,formed,Block.UPDATE_CLIENTS);
                }
            }
        } finally { checking=false; }
    }
    private static PlanterLayout.Result invalid(String status) { return new PlanterLayout.Result(false,status,0,0,0); }
    public static PlanterLayout.Role role(BlockState state) {
        if (state.is(Agristics.NETWORK.get())) return PlanterLayout.Role.CONTROLLER;
        if (SeedStorage.isStorage(state)) return PlanterLayout.Role.SEEDS;
        if (state.is(Agristics.NETWORK_ACCESS.get())) return PlanterLayout.Role.ACCESS;
        if (state.is(Agristics.INPUT_PORT.get())) return PlanterLayout.Role.INPUT;
        if (state.is(Agristics.OUTPUT_PORT.get())) return PlanterLayout.Role.OUTPUT;
        if (state.is(Agristics.ESSENCE_VAULT.get())) return PlanterLayout.Role.VAULT;
        return state.getBlock() instanceof PlanterPartBlock part ? part.role() : PlanterLayout.Role.OTHER;
    }
}
