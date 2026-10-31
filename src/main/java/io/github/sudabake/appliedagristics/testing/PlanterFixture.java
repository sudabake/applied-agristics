package io.github.sudabake.appliedagristics.testing;

import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.*;

/** Shared disposable-world fixture. Never called unless a developer test is explicitly enabled. */
public record PlanterFixture(NetworkPlanter controller,SeedStorage seeds,ItemPort input,ItemPort output,
    EssenceVault vault,NetworkAccess access,DriveBlockEntity drive,BlockPos power) {
    public static PlanterFixture install(Level level,BlockPos center,boolean bulkStorage) {
        var origin=center.offset(-1,-1,0);
        for (int y=0;y<4;y++) for (int z=0;z<3;z++) for (int x=0;x<3;x++) {
            var block=x==1 && z==1 && y>0 && y<3 ? Agristics.CULTIVATION_CHAMBER.get() : Agristics.PLANTER_FRAME.get();
            level.setBlockAndUpdate(origin.offset(x,y,z),block.defaultBlockState());
        }
        set(level,center,Agristics.NETWORK.get());
        set(level,center.east(),Agristics.NETWORK_ACCESS.get());
        set(level,center.west().below(),Agristics.INPUT_PORT.get());
        set(level,center.below(),Agristics.OUTPUT_PORT.get());
        if (bulkStorage) { set(level,center.west(),Agristics.SEEDS.get()); set(level,center.east().below(),Agristics.ESSENCE_VAULT.get()); }
        set(level,center.east(2),AEBlocks.CREATIVE_ENERGY_CELL.block());
        set(level,center.east(3),AEBlocks.DRIVE.block());
        var controller=(NetworkPlanter)level.getBlockEntity(center);
        controller.structure.refresh();
        var access=(NetworkAccess)level.getBlockEntity(center.east()); access.ensureNode();
        return new PlanterFixture(controller,bulkStorage ? (SeedStorage)level.getBlockEntity(center.west()) : null,
            (ItemPort)level.getBlockEntity(center.west().below()),(ItemPort)level.getBlockEntity(center.below()),
            bulkStorage ? (EssenceVault)level.getBlockEntity(center.east().below()) : null,access,
            (DriveBlockEntity)level.getBlockEntity(center.east(3)),center.east(2));
    }
    private static void set(Level level,BlockPos pos,Block block) { level.setBlockAndUpdate(pos,block.defaultBlockState()); }
    public long producedCount(appeng.api.stacks.AEItemKey key) {
        long[] count={output.count(key)};
        if(vault!=null && controller.getLevel().getBlockEntity(vault.getBlockPos())==vault)
            vault.mountInventories((storage,priority)->{
                var available=new appeng.api.stacks.KeyCounter();storage.getAvailableStacks(available);count[0]+=available.get(key);
            });
        return count[0];
    }
    public void modules(int speed,int parallel,boolean conversion) {
        var level=controller.getLevel(); var origin=controller.getBlockPos().offset(-1,-1,0); var candidates=new ArrayList<BlockPos>();
        for (int y=0;y<2;y++) for (int z=0;z<3;z++) for (int x=0;x<3;x++) {
            var pos=origin.offset(x,y,z); var block=level.getBlockState(pos).getBlock();
            if (block==Agristics.PLANTER_FRAME.get() || block==Agristics.SPEED_MODULE.get() || block==Agristics.PARALLEL_MODULE.get() || block==Agristics.CONVERSION_MODULE.get()) {
                set(level,pos,Agristics.PLANTER_FRAME.get()); candidates.add(pos);
            }
        }
        if (speed+parallel+(conversion?1:0)>candidates.size()) throw new IllegalArgumentException("Fixture module count exceeds shell");
        int i=0; for (int n=0;n<speed;n++) set(level,candidates.get(i++),Agristics.SPEED_MODULE.get());
        for (int n=0;n<parallel;n++) set(level,candidates.get(i++),Agristics.PARALLEL_MODULE.get());
        if (conversion) set(level,candidates.get(i),Agristics.CONVERSION_MODULE.get());
        controller.structure.refresh(); access.ensureNode();
    }
}

