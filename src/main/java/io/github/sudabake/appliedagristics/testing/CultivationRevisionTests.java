package io.github.sudabake.appliedagristics.testing;

import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.logic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class CultivationRevisionTests {
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void formedRotationPreservesNetworkClaimsAndDirections(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        var seed=AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(),ResourceLocation.parse("minecraft:cow")));
        f.seeds().add(seed,1);
        h.runAtTickTime(100,()-> {
            var a=f.controller();var id=Cultivations.id(seed.toStack(1),h.getLevel());a.configure(id,true,-1);a.step();
            var task=a.tasks.get(id);double progress=task.progress;var node=a.getMainNode().getNode();
            var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND,AEItems.CERTUS_QUARTZ_WRENCH.stack());
            for(var part:List.of(f.access(),f.input(),f.output(),f.seeds(),f.vault())) {
                var pos=part.getBlockPos();var start=PlanterOrientation.facing(part.getBlockState());
                int rotations=PlanterOrientation.property(part.getBlockState()).getPossibleValues().size();
                var seen=EnumSet.noneOf(Direction.class);
                for(int n=0;n<rotations;n++) {
                    var hit=new BlockHitResult(pos.getCenter(),Direction.NORTH,pos,false);
                    MachineWrench.wrench(new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,pos,hit));
                    a.structure.refresh();seen.add(PlanterOrientation.front(part.getBlockState()));
                    h.assertTrue(a.structure.formed() && a.getMainNode().getNode()==node,"Port rotation retains the existing AE node");
                    h.assertTrue(task.occupied==1 && task.progress==progress && task.enabled,"Rotation preserves seed claims, progress and enablement");
                }
                h.assertTrue(seen.size()==rotations && PlanterOrientation.facing(part.getBlockState())==start,"Formed wrench rotation visits every supported face");
            }
            var accessPos=f.access().getBlockPos();
            h.getLevel().setBlockAndUpdate(accessPos,PlanterOrientation.withFacing(f.access().getBlockState(),Direction.UP));
            var missing=a.getBlockPos().above(2).west();var saved=h.getLevel().getBlockState(missing);
            h.getLevel().removeBlock(missing,false);a.structure.refresh();
            h.assertTrue(!a.structure.formed() && PlanterOrientation.front(f.access().getBlockState())==Direction.UP,"Breaking formation preserves upward port direction");
            h.getLevel().setBlockAndUpdate(missing,saved);a.structure.refresh();
            h.assertTrue(a.structure.formed() && PlanterOrientation.front(f.access().getBlockState())==Direction.UP,"Repair preserves the manually selected direction");
            var controllerFacing=PlanterOrientation.facing(a.getBlockState());
            MachineWrench.wrench(new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,a.getBlockPos(),
                    new BlockHitResult(a.getBlockPos().getCenter(),Direction.NORTH,a.getBlockPos(),false)));
            h.assertTrue(PlanterOrientation.facing(a.getBlockState())==controllerFacing,"Formed controller retains its structure anchor direction");
            h.succeed();
        });
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void nativeTreesProduceNetLootAndCompletePreviews(GameTestHelper h) {
        for(var seedItem:List.of(Items.OAK_SAPLING,Items.SPRUCE_SAPLING,Items.BIRCH_SAPLING,Items.JUNGLE_SAPLING,
                Items.ACACIA_SAPLING,Items.DARK_OAK_SAPLING,Items.CHERRY_SAPLING,Items.MANGROVE_PROPAGULE,
                Items.AZALEA,Items.FLOWERING_AZALEA,Items.CRIMSON_FUNGUS,Items.WARPED_FUNGUS)) {
            var stack=new ItemStack(seedItem);var source=CropSources.find(h.getLevel(),stack);
            h.assertTrue(source!=null && source.maximumBatch()==64 && source.ticks()==2400,"Every native tree shares the same cycle and bounded harvest path: "+seedItem);
            var before=stack.copy();var batch=Harvests.networkBatch(h.getLevel(),h.absolutePos(BlockPos.ZERO),stack,64,new HashMap<>());
            long logs=batch.getOrDefault(AEItemKey.of(source.product()),0L);
            h.assertTrue(logs>=64 && logs<=256,"Tree harvest yields one to four logs per completed cycle");
            h.assertTrue(batch.getOrDefault(AEItemKey.of(stack),0L)<=64 && ItemStack.isSameItemSameComponents(stack,before),"Retained saplings are untouched and never duplicated into every harvest");
            var encoded=stack.save(h.getLevel().registryAccess());
            h.assertTrue(Harvests.seed(ItemStack.parseOptional(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)encoded),h.getLevel()),"Tree seed recognition survives serialization");
        }
        var oak=CropSources.find(h.getLevel(),new ItemStack(Items.OAK_SAPLING));
        h.assertTrue(oak.previewOutputs(h.getLevel()).stream().anyMatch(stack->stack.is(Items.APPLE)),"Rare apple output appears without rolling a harvest");
        var catalog=PlantingCatalog.serialize(h.getLevel());
        var entry=catalog.stream().map(net.minecraft.nbt.CompoundTag.class::cast).filter(tag->
                ItemStack.parseOptional(h.getLevel().registryAccess(),tag.getCompound("seed")).is(Items.OAK_SAPLING)).findFirst().orElseThrow();
        h.assertTrue(entry.getList("outputs",10).size()>=3,"JEI lists saplings, logs and apples from the same definition");
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void vanillaPlantCoverageAndNetReplanting(GameTestHelper h) {
        var plants=List.of(Items.WHEAT_SEEDS,Items.BEETROOT_SEEDS,Items.CARROT,Items.POTATO,Items.NETHER_WART,
                Items.COCOA_BEANS,Items.SWEET_BERRIES,Items.MELON_SEEDS,Items.PUMPKIN_SEEDS,
                Items.SUGAR_CANE,Items.CACTUS,Items.BAMBOO,Items.KELP,Items.GLOW_BERRIES,
                Items.BROWN_MUSHROOM,Items.RED_MUSHROOM,Items.CHORUS_FLOWER,Items.TORCHFLOWER_SEEDS,Items.PITCHER_POD);
        for(var item:plants) {
            var seed=new ItemStack(item);
            h.assertTrue(Harvests.seed(seed,h.getLevel()) && Cultivations.seed(seed,h.getLevel(),h.absolutePos(BlockPos.ZERO))!=null,"Native planting entry resolves for "+item);
        }
        if (!net.neoforged.fml.ModList.get().isLoaded("botanypots"))
            for(var item:List.of(Items.SUGAR_CANE,Items.CACTUS,Items.BAMBOO,Items.KELP,Items.GLOW_BERRIES,Items.BROWN_MUSHROOM,Items.RED_MUSHROOM)) {
                var seed=new ItemStack(item);var drops=Harvests.networkBatch(h.getLevel(),h.absolutePos(BlockPos.ZERO),seed,64,new HashMap<>());
                h.assertTrue(drops.size()==1 && drops.get(AEItemKey.of(seed))==64,"Declared self-propagating plants yield net produce once without extra fallback seeds");
            }
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void treePowerPreviewMatchesSettledPower(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);f.modules(4,4,false);
        var seed=AEItemKey.of(new ItemStack(Items.OAK_SAPLING));f.seeds().add(seed,256);
        h.runAtTickTime(100,()-> {
            var a=f.controller();a.configure(Cultivations.id(seed.toStack(1),h.getLevel()),true,-1);a.step();
            var row=a.snapshot(0,"").getList("rows",10).getCompound(0);
            h.assertTrue(Math.abs(row.getDouble("power")-2343.75)<.00001 && Math.abs(row.getDouble("requiredPower")-row.getDouble("power"))<.00001,"Tree actual cost and current power preview share the same batch limit");
            h.assertTrue(Math.abs(row.getDouble("fullLoadPower")-5000)<.00001 && Math.abs(row.getDouble("nextSpeedPower")-row.getDouble("power"))<.00001,"Full-load explanation is separate and saturated upgrades do not invent extra demand");
            var logs=AEItemKey.of(new ItemStack(Items.OAK_LOG));long before=f.producedCount(logs);
            for(int step=0;step<400;step++) a.step();
            long produced=f.producedCount(logs)-before;
            h.assertTrue(Math.abs(produced-64000)<2000,"A hundred simulated seconds preserves approximately 640 logs per second");
            com.mojang.logging.LogUtils.getLogger().info("TREE_YIELD_PROFILE simulatedTicks=2000 logs={} logsPerSecond={} power={}",produced,produced/100.0,row.getDouble("power"));
            h.succeed();
        });
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void basicTreeHarvestRetainsSeedAndBlockedOutput(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);h.setBlock(pos,Agristics.EARLY.get());EarlyPlanter machine=h.getBlockEntity(pos);
        machine.items.setStackInSlot(0,new ItemStack(Items.OAK_SAPLING));
        machine.items.setStackInSlot(1,new ItemStack(Blocks.FARMLAND));
        for(int slot=2;slot<11;slot++) machine.items.setStackInSlot(slot,new ItemStack(Items.STONE,64));
        for(int step=0;step<121;step++) machine.step();
        h.assertTrue(machine.items.getStackInSlot(0).is(Items.OAK_SAPLING) && machine.items.getStackInSlot(0).getCount()==1,
                "A blocked basic tree harvest retains the planted sapling");
        var saved=machine.saveWithFullMetadata(h.getLevel().registryAccess());
        var pending=saved.getList("pending",10).copy();machine.step();
        h.assertTrue(machine.saveWithFullMetadata(h.getLevel().registryAccess()).getList("pending",10).equals(pending),"Blocked output does not reroll or mutate the paid harvest");
        for(int slot=2;slot<11;slot++) machine.items.setStackInSlot(slot,ItemStack.EMPTY);
        machine.step();int logs=0;
        for(int slot=2;slot<11;slot++) if(machine.items.getStackInSlot(slot).is(Items.OAK_LOG)) logs+=machine.items.getStackInSlot(slot).getCount();
        h.assertTrue(logs>=1 && logs<=4 && machine.items.getStackInSlot(0).getCount()==1,"Unblocking delivers one net tree harvest and keeps the planted material");
        h.succeed();
    }
}
