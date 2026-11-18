package io.github.sudabake.appliedagristics.testing;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.menu.MachineMenu;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class OutputAndStorageTests {
    @GameTest(template="empty", templateNamespace=Agristics.ID, timeoutTicks=240)
    public static void outputPortPushesEntireBufferImmediatelyAndOnlyAcceptsHarvests(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),false);
        var port=f.output();
        var chestPos=port.getBlockPos().relative(port.outputFace());
        h.getLevel().setBlockAndUpdate(chestPos,Blocks.CHEST.defaultBlockState());
        h.runAtTickTime(100,()->{
            var wheat=AEItemKey.of(new ItemStack(Items.WHEAT));
            h.assertTrue(port.insertLocal(wheat,1024,false)==1024,"The finite buffer accepts all sixteen stacks");
            port.toggleAutoOutput();
            var chest=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,chestPos,port.outputFace().getOpposite());
            h.assertTrue(port.count(wheat)==0,"Enabling export flushes every stack without a 64-item budget");
            h.assertTrue(port.insertLocal(wheat,128,true)==128 && port.count(wheat)==0,
                    "Simulated harvest insertion has no output side effects");
            h.assertTrue(port.insertLocal(wheat,128,false)==128 && port.count(wheat)==0,
                    "New harvests reach the front container in the same insertion call");
            int received=0;
            for(int slot=0;slot<chest.getSlots();slot++) {
                if(chest.getStackInSlot(slot).is(Items.WHEAT)) received+=chest.getStackInSlot(slot).getCount();
                chest.insertItem(slot,new ItemStack(Items.DIRT,64),false);
            }
            h.assertTrue(received==1152 && port.insertLocal(wheat,7,false)==7 && port.count(wheat)==7,
                    "A full neighbor preserves actual harvests in the buffer");
            for(var side:Direction.values()) {
                var pipe=port.automation(side);
                h.assertTrue(pipe.insertItem(0,new ItemStack(Items.WHEAT),false).getCount()==1
                        && pipe.extractItem(0,64,true).getCount()==7,
                        "All faces reject input and expose output to pipes");
            }
            var player=h.makeMockPlayer(GameType.SURVIVAL);
            var menu=new MachineMenu(1,player.getInventory(),port);
            player.getInventory().setItem(9,new ItemStack(Items.WHEAT,5));
            h.assertTrue(!menu.slots.getFirst().mayPlace(new ItemStack(Items.WHEAT))
                    && menu.quickMoveStack(player,16).isEmpty() && port.count(wheat)==7,
                    "Manual placement and shift-click cannot insert into output slots");
            h.assertTrue(port.automation(Direction.UP).extractItem(0,64,false).getCount()==7 && port.count(wheat)==0,
                    "Pipe extraction removes the real buffered count exactly once");
            h.succeed();
        });
    }

    @GameTest(template="empty", templateNamespace=Agristics.ID, timeoutTicks=240)
    public static void altarAutomaticOutputUsesConfiguredNeighborInsteadOfNetwork(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        f.drive().getInternalInventory().setItemDirect(0,AEItems.ITEM_CELL_1K.stack());
        var pos=f.power().above();
        h.getLevel().setBlockAndUpdate(pos,Agristics.ME_INFUSION.get().defaultBlockState());
        var altar=(MEInfusionAltar)h.getLevel().getBlockEntity(pos);
        h.getLevel().setBlockAndUpdate(pos.north(),Blocks.CHEST.defaultBlockState());
        h.runAtTickTime(100,()->{
            h.assertTrue(altar.getMainNode().isActive(),"The altar is connected to a network with writable disk capacity");
            altar.configureSide(Direction.NORTH.ordinal(),3);
            altar.toggleAutoOutput();
            altar.items.setStackInSlot(1,new ItemStack(Items.DIAMOND,12));
            altar.step();
            var chest=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,pos.north(),Direction.SOUTH);
            var diamond=AEItemKey.of(new ItemStack(Items.DIAMOND));
            h.assertTrue(chest.getStackInSlot(0).getCount()==12 && altar.items.getStackInSlot(1).isEmpty()
                    && altar.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks().get(diamond)==0,
                    "Manual automatic output sends items only to the configured adjacent container");
            altar.configureSide(Direction.NORTH.ordinal(),1);
            altar.items.setStackInSlot(1,new ItemStack(Items.DIAMOND,3));
            altar.step();
            h.assertTrue(altar.items.getStackInSlot(1).getCount()==3,
                    "Disabling all output faces retains the result even with free ME disk capacity");
            h.succeed();
        });
    }

    @GameTest(template="empty", templateNamespace=Agristics.ID, timeoutTicks=1200)
    public static void moduleChangesAndSeedCollectionKeepActualNetworkCounts(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        for(var item:List.of(Items.WHEAT_SEEDS,Items.CARROT,Items.BAMBOO,Items.CACTUS,Items.OAK_SAPLING)) {
            var key=AEItemKey.of(new ItemStack(item));
            f.seeds().add(key,32);
            f.vault().storeProduced(key,7,false);
        }
        f.seeds().toggleAutoCollect();
        h.runAtTickTime(100,()->{
            for(var key:f.seeds().keys())
                f.controller().configure(io.github.sudabake.appliedagristics.Cultivations.id(key.toStack(1),h.getLevel()),true,-1);
            storagePhase(h,f,0);
        });
    }

    private static void storagePhase(GameTestHelper h,PlanterFixture f,int phase) {
        f.modules(phase%5,phase%5,false);
        f.controller().step();
        f.seeds().step();
        var service=f.controller().getMainNode().getGrid().getStorageService();
        service.invalidateCache();
        var fresh=service.getInventory().getAvailableStacks();
        var cached=service.getCachedInventory();
        for(var key:f.seeds().keys()) {
            long actual=listedCount(f,key);
            h.assertTrue(fresh.get(key)==actual && cached.get(key)==actual,
                    "Seed listing matches real unreserved inventory after module changes, phase="+phase+" item="+key
                    +" actual="+actual+" fresh="+fresh.get(key)+" cached="+cached.get(key));
        }
        if(phase==79) {
            f.controller().configureAll(false);
            f.controller().step();
            service.invalidateCache();
            for(var key:f.seeds().keys())
                h.assertTrue(service.getCachedInventory().get(key)==listedCount(f,key),"Pausing restores the exact complete seed inventory");
            h.succeed();
        } else {
            var shell=f.controller().getBlockPos().above(2);
            var previous=h.getLevel().getBlockState(shell);
            h.getLevel().setBlockAndUpdate(shell,Blocks.AIR.defaultBlockState());
            f.controller().structure.refresh();
            h.assertTrue(!f.controller().structure.formed(),"Removing a shell block breaks the actual structure");
            h.getLevel().setBlockAndUpdate(shell,previous);
            f.controller().structure.refresh();
            f.access().ensureNode();
            h.runAfterDelay(5,()->storagePhase(h,f,phase+1));
        }
    }

    private static long listedCount(PlanterFixture fixture,AEItemKey key) {
        long count=fixture.seeds().count(key)-fixture.seeds().used(key)+fixture.output().count(key);
        var registries=fixture.controller().getLevel().registryAccess();
        for(var raw:fixture.vault().saveWithFullMetadata(registries).getList("essences",10)) {
            var entry=(net.minecraft.nbt.CompoundTag)raw;
            if(key.equals(AEItemKey.fromTag(registries,entry.getCompound("key")))) count+=entry.getLong("count");
        }
        return count;
    }
}
