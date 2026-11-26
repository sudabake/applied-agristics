package io.github.sudabake.appliedagristics.multiblock;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.block.NetworkPlanter;
import io.github.sudabake.appliedagristics.block.PlanterOrientation;
import io.github.sudabake.appliedagristics.network.Messages;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import static io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role;
import static io.github.sudabake.appliedagristics.multiblock.PlanterLayout.Role.*;

public final class ArrayConstruction {
    private record Cell(BlockPos pos, BlockState state, int index) {}

    private ArrayConstruction() {}

    public static void build(NetworkPlanter controller, ServerPlayer player) {
        var level = player.serverLevel();
        if (controller.getLevel()!=level || !player.getAbilities().mayBuild || controller.structure.formed()) return;
        boolean creative = player.isCreative();
        var facing = PlanterOrientation.front(controller.getBlockState());
        var right = facing.getClockWise();
        var origin = controller.getBlockPos().below().relative(right,-1);
        var template = PlanterLayout.example();
        var counts = new EnumMap<Role,Integer>(Role.class);
        var shell = new ArrayList<Cell>();
        var frames = new ArrayList<BlockPos>();
        var blocked = new ArrayList<BlockPos>();
        var plan = new LinkedHashMap<Cell,Role>();
        for (int y=0; y<PlanterLayout.HEIGHT; y++) {
            for (int z=0; z<PlanterLayout.DEPTH; z++) {
                for (int x=0; x<PlanterLayout.WIDTH; x++) {
                    var pos = origin.relative(right,x).relative(facing.getOpposite(),z).above(y);
                    if (pos.equals(controller.getBlockPos())) continue;
                    if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                            || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player,pos)) {
                        blocked.add(pos);
                        continue;
                    }
                    var state = level.getBlockState(pos);
                    var cell = new Cell(pos,state,PlanterLayout.index(x,y,z));
                    var role = PlanterStructure.role(state);
                    var owner = PlanterStructure.ownerAt(level,pos);
                    if (owner!=null && owner!=controller.structure) blocked.add(pos);
                    else if (PlanterLayout.interior(x,y,z)) {
                        if (state.isAir()) plan.put(cell,CHAMBER);
                        else if (role!=CHAMBER) blocked.add(pos);
                    } else if (state.isAir()) shell.add(cell);
                    else if (role==OTHER || role==CONTROLLER || role==CHAMBER) blocked.add(pos);
                    else {
                        counts.merge(role,1,Integer::sum);
                        if (role==FRAME) frames.add(pos);
                    }
                }
            }
        }
        if (!blocked.isEmpty()) {
            red(player,blocked);
            return;
        }
        if (count(counts,ACCESS)>1 || count(counts,SPEED)>PlanterLayout.MODULE_LIMIT
                || count(counts,PARALLEL)>PlanterLayout.MODULE_LIMIT) {
            if (count(counts,ACCESS)>1) say(player,"status.appliedagristics.multiple_network_access");
            if (count(counts,SPEED)>PlanterLayout.MODULE_LIMIT) say(player,"status.appliedagristics.too_many_speed_modules");
            if (count(counts,PARALLEL)>PlanterLayout.MODULE_LIMIT) say(player,"status.appliedagristics.too_many_parallel_modules");
            return;
        }

        var missing = new ArrayList<Role>();
        if (count(counts,ACCESS)==0) missing.add(ACCESS);
        var inventory = player.getInventory();
        if (creative) {
            for (var role : List.of(INPUT,SEEDS,OUTPUT,VAULT,SPEED,PARALLEL,CONVERSION)) {
                int wanted = (int) java.util.Arrays.stream(template).filter(part -> part==role).count();
                for (int i=count(counts,role); i<wanted; i++) missing.add(role);
            }
        } else {
            if (count(counts,INPUT)+count(counts,SEEDS)==0) {
                if (inventory.countItem(block(INPUT).asItem())==0 && inventory.countItem(block(SEEDS).asItem())==0) {
                    say(player,"gui.appliedagristics.build_need_seed");
                    missing.add(INPUT);
                } else missing.add(inventory.countItem(block(INPUT).asItem())>0 ? INPUT : SEEDS);
            }
            if (count(counts,OUTPUT)+count(counts,VAULT)==0) {
                if (inventory.countItem(block(OUTPUT).asItem())==0 && inventory.countItem(block(VAULT).asItem())==0) {
                    say(player,"gui.appliedagristics.build_need_output");
                    missing.add(OUTPUT);
                } else missing.add(inventory.countItem(block(OUTPUT).asItem())>0 ? OUTPUT : VAULT);
            }
        }
        if (missing.size()>shell.size()) {
            say(player,"gui.appliedagristics.build_no_space",missing.size()-shell.size());
            if (!frames.isEmpty()) red(player,frames);
            return;
        }
        for (var role:missing) {
            // Keep the pictured positions when they are empty, but preserve legal existing parts.
            var cell = shell.stream().filter(candidate -> template[candidate.index()]==role).findFirst()
                    .orElse(shell.get(0));
            shell.remove(cell);
            plan.put(cell,role);
        }
        shell.forEach(cell -> plan.put(cell,FRAME));

        var supplies = new LinkedHashMap<Item,Integer>();
        var states = new LinkedHashMap<Cell,BlockState>();
        plan.forEach((cell,role) -> {
            var part = block(role);
            var state = part.defaultBlockState();
            var outward = outward(cell.index(),facing,PlanterOrientation.property(state)==
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
            state = PlanterOrientation.withFacing(state,outward);
            if (!level.isUnobstructed(state,cell.pos(),CollisionContext.empty())) blocked.add(cell.pos());
            states.put(cell,state);
            supplies.merge(part.asItem(),1,Integer::sum);
        });
        boolean suppliesMissing = false;
        if (!creative) {
            for (var entry:supplies.entrySet()) {
                int needed = entry.getValue()-inventory.countItem(entry.getKey());
                if (needed>0) {
                    suppliesMissing = true;
                    say(player,"gui.appliedagristics.build_need_items",needed,entry.getKey().getDescription());
                }
            }
        }
        if (!blocked.isEmpty()) {
            red(player,blocked);
            return;
        }
        if (suppliesMissing || states.isEmpty()) return;

        var snapshots = new ArrayList<BlockSnapshot>();
        boolean complete = false;
        try {
            for (var entry:states.entrySet()) {
                var pos = entry.getKey().pos();
                if (level.getBlockState(pos)!=entry.getKey().state()) return;
                snapshots.add(BlockSnapshot.create(level.dimension(),level,pos));
                if (!level.setBlock(pos,entry.getValue(),Block.UPDATE_KNOWN_SHAPE)) return;
            }
            boolean denied = snapshots.size()==1 ? ForgeEventFactory.onBlockPlace(player,snapshots.get(0),Direction.UP)
                    : ForgeEventFactory.onMultiBlockPlace(player,snapshots,Direction.UP);
            if (denied) {
                say(player,"gui.appliedagristics.build_denied");
                return;
            }
            complete = true;
        } finally {
            if (!complete) {
                for (int i=snapshots.size()-1;i>=0;i--) snapshots.get(i).restore(true,true);
            }
        }

        if (!creative) {
            for (int i=0; i<inventory.getContainerSize(); i++) {
                var stack = inventory.getItem(i);
                int used = Math.min(stack.getCount(),supplies.getOrDefault(stack.getItem(),0));
                if (used==0) continue;
                supplies.computeIfPresent(stack.getItem(),(item,left) -> left-used);
                stack.shrink(used);
            }
            inventory.setChanged();
        }
        states.forEach((cell,state) -> {
            var pos = cell.pos();
            state.getBlock().setPlacedBy(level,pos,state,player,new ItemStack(state.getBlock()));
            level.sendBlockUpdated(pos,cell.state(),state,Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(pos,Blocks.AIR);
        });
        controller.structure.refresh();
        say(player,"gui.appliedagristics.build_complete");
    }

    private static int count(EnumMap<Role,Integer> counts, Role role) {
        return counts.getOrDefault(role,0);
    }

    private static Direction outward(int index, Direction front, boolean vertical) {
        int x=index%PlanterLayout.WIDTH;
        int z=index/PlanterLayout.WIDTH%PlanterLayout.DEPTH;
        int y=index/(PlanterLayout.WIDTH*PlanterLayout.DEPTH);
        if (z==0) return front;
        if (z==PlanterLayout.DEPTH-1) return front.getOpposite();
        if (x==0) return front.getCounterClockWise();
        if (x==PlanterLayout.WIDTH-1) return front.getClockWise();
        if (vertical && y==0) return Direction.DOWN;
        if (vertical && y==PlanterLayout.HEIGHT-1) return Direction.UP;
        return front;
    }

    private static Block block(Role role) {
        return switch (role) {
            case ACCESS -> Agristics.NETWORK_ACCESS.get();
            case INPUT -> Agristics.INPUT_PORT.get();
            case SEEDS -> Agristics.SEEDS.get();
            case OUTPUT -> Agristics.OUTPUT_PORT.get();
            case VAULT -> Agristics.ESSENCE_VAULT.get();
            case SPEED -> Agristics.SPEED_MODULE.get();
            case PARALLEL -> Agristics.PARALLEL_MODULE.get();
            case CONVERSION -> Agristics.CONVERSION_MODULE.get();
            case CHAMBER -> Agristics.CULTIVATION_CHAMBER.get();
            case FRAME -> Agristics.PLANTER_FRAME.get();
            default -> throw new IllegalArgumentException("Cannot build " + role);
        };
    }

    private static void red(ServerPlayer player, List<BlockPos> blocked) {
        say(player,"gui.appliedagristics.build_blocked",blocked.size());
        Messages.sendToPlayer(player,new Messages.BuildConflicts(player.level().dimension().location(),blocked));
        player.closeContainer();
    }

    private static void say(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key,args));
    }
}
