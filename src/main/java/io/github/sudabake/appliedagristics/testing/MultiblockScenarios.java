package io.github.sudabake.appliedagristics.testing;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** World scenarios for the local seed banks and read-only outputs of the assembled array. */
public final class MultiblockScenarios {
    private static PlanterFixture fixture(GameTestHelper h) { return PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true); }
    public static void ordinaryOwnedList(GameTestHelper h) {
        var f=fixture(h); var seeds=f.seeds(); var planter=f.controller();
        h.runAtTickTime(100,()->{
            h.assertTrue(planter.getMainNode().isActive(),"Formed structure connects through network access");
            h.assertTrue(planter.snapshot(0,"").getList("rows",10).isEmpty(),"No ghost crops without owned seeds");
            var inputs=new ArrayList<>(List.of(Items.WHEAT_SEEDS,Items.CARROT,Items.POTATO,Items.BEETROOT_SEEDS,Items.MELON_SEEDS,Items.PUMPKIN_SEEDS));
            if (net.neoforged.fml.ModList.get().isLoaded("farmersdelight")) for (var name:List.of("cabbage_seeds","tomato_seeds","onion")) inputs.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("farmersdelight:"+name)));
            for (var item:inputs) seeds.add(AEItemKey.of(new ItemStack(item)),1);
            var rows=planter.snapshot(0,"").getList("rows",10); var next=planter.snapshot(1,"").getList("rows",10);
            h.assertTrue(rows.size()==4 && rows.getCompound(1).getString("id").equals(next.getCompound(0).getString("id")),"Owned list scrolls by one row");
            planter.configureAll(true); h.assertTrue(planter.tasks.size()==inputs.size(),"Enable includes all owned crops");
            planter.configureAll(false); h.assertTrue(planter.tasks.values().stream().noneMatch(t->t.enabled),"Disable includes offscreen crops");
            for (var item:inputs) { var crop=Cultivations.seed(new ItemStack(item),h.getLevel(),planter.getBlockPos()); planter.configure(crop.getId(),true,1); }
            for (int i=0;i<44;i++) planter.step();
            var inventory=planter.getMainNode().getGrid().getStorageService().getInventory();
            for (var item:inputs) { var crop=Cultivations.seed(new ItemStack(item),h.getLevel(),planter.getBlockPos()); h.assertTrue(inventory.getAvailableStacks().get(AEItemKey.of(crop.essenceStack()))>=1,"Produced crop visible through local outputs: "+item); }
            var wheat=AEItemKey.of(new ItemStack(Items.WHEAT)); inventory.extract(wheat,Long.MAX_VALUE,Actionable.MODULATE,IActionSource.ofMachine(planter));
            for (int i=0;i<40;i++) planter.step(); h.assertTrue(inventory.getAvailableStacks().get(wheat)>=1,"Consumed wheat restocks"); h.succeed();
        });
    }
    public static void reservationAndTargets(GameTestHelper h) {
        var f=fixture(h); var crop=Crops.all().stream().filter(c->c!=com.blakebr0.mysticalagriculture.lib.ModCrops.INFERIUM).findFirst().orElseThrow();
        var seed=AEItemKey.of(new ItemStack(crop.getSeedsItem())); var essence=AEItemKey.of(new ItemStack(crop.getEssenceItem())); f.seeds().add(seed,1);
        h.runAtTickTime(100,()->{
            var a=f.controller(); var inventory=a.getMainNode().getGrid().getStorageService().getInventory(); var source=IActionSource.ofMachine(a);
            a.configure(crop.getId(),true,2); for (int i=0;i<85;i++) a.step();
            h.assertTrue(inventory.getAvailableStacks().get(essence)==2 && f.seeds().used(seed)==1,"Stock target pauses with one reserved seed");
            h.assertTrue(inventory.getAvailableStacks().get(seed)==0 && inventory.extract(seed,1,Actionable.SIMULATE,source)==0,"Reserved seed is absent from AE available stock");
            inventory.extract(essence,1,Actionable.MODULATE,source); for (int i=0;i<40;i++) a.step();
            h.assertTrue(inventory.getAvailableStacks().get(essence)==2,"Consumption restarts production");
            a.configure(crop.getId(),false,2); h.assertTrue(inventory.getAvailableStacks().get(seed)==1,"Stopping returns the seed to available stock");
            h.assertTrue(inventory.insert(essence,1,Actionable.MODULATE,source)==0,"AE cannot deposit into output-only essence storage");
            f.vault().storeProduced(essence,10_000_000_000L,false);
            var saved=f.vault().saveWithFullMetadata(h.getLevel().registryAccess()); var restored=new EssenceVault(f.vault().getBlockPos(),f.vault().getBlockState()); restored.setLevel(h.getLevel()); restored.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(restored.saveWithFullMetadata(h.getLevel().registryAccess()).getList("essences",10).equals(saved.getList("essences",10)),"Large essence counts survive serialization");
            var drops=net.minecraft.world.level.block.Block.getDrops(f.vault().getBlockState(),h.getLevel(),f.vault().getBlockPos(),f.vault());
            h.assertTrue(drops.size()==1 && drops.getFirst().has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA),"Vault dismantles to one data-bearing block");
            h.getLevel().setBlockAndUpdate(a.getBlockPos().above(2),Blocks.AIR.defaultBlockState());
            h.assertTrue(!a.structure.formed() && !f.access().connectedStructure(),"Breaking shell removes network access immediately"); h.succeed();
        });
    }
    public static void modulesAndConversion(GameTestHelper h) {
        var f=fixture(h); f.modules(2,2,true); var a=f.controller();
        var crop=Crops.all().stream().filter(c->c!=com.blakebr0.mysticalagriculture.lib.ModCrops.INFERIUM).findFirst().orElseThrow();
        var seed=AEItemKey.of(new ItemStack(crop.getSeedsItem())); f.seeds().add(seed,2);
        h.runAtTickTime(100,()->{
            a.configure(crop.getId(),true,-1); a.step(); var task=a.tasks.get(crop.getId());
            h.assertTrue(a.parallel()==16 && Math.abs(a.speed()-io.github.sudabake.appliedagristics.logic.ProductionMath.moduleSpeed(2,Balance.SPEED_MAX.get()))<1e-8 && a.conversion() && task.occupied==2,"Physical modules determine speed, parallel and conversion");
            h.assertTrue(f.seeds().count(seed)==2,"Conversion does not consume existing seeds");
            f.seeds().add(seed,14); a.step(); h.assertTrue(task.occupied==16 && f.seeds().count(seed)==16,"Newly supplied seeds fill parallel; surplus remains");
            var saved=a.saveWithFullMetadata(h.getLevel().registryAccess());
            var restored=new NetworkPlanter(a.getBlockPos(),a.getBlockState()); restored.setLevel(h.getLevel()); restored.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(restored.tasks.get(crop.getId()).progress==task.progress,"Partial work survives serialization");
            a.configure(crop.getId(),false,-1); h.assertTrue(f.seeds().used(seed)==0,"Disabling releases local seed claims"); h.succeed();
        });
    }
    public static void manyCrops(GameTestHelper h) {
        var f=fixture(h); var a=f.controller();
        h.runAtTickTime(100,()->{
            var crops=Crops.all().stream().filter(c->c.getCruxBlock()==null && c.getRequiredBiomes().isEmpty()).toList();
            for (var crop:crops) { f.seeds().add(AEItemKey.of(new ItemStack(crop.getSeedsItem())),1); a.configure(crop.getId(),true,1_000_000); }
            for (int i=0;i<40;i++) a.step(); long start=System.nanoTime();
            for (int i=0;i<400;i++) a.step(); double ms=(System.nanoTime()-start)/1_000_000.;
            org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("MULTIBLOCK_BATCH_BENCHMARK crops={} batches=400 totalMs={}",crops.size(),ms);
            var inventory=a.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks();
            for (var crop:crops) h.assertTrue(inventory.get(AEItemKey.of(new ItemStack(crop.getEssenceItem())))>=10,"Each crop produces into local bulk storage"); h.succeed();
        });
    }
    public static void biologicalStockIsolation(GameTestHelper h) {
        var f=fixture(h); var a=f.controller(); var cow=ResourceLocation.parse("minecraft:cow"); var pig=ResourceLocation.parse("minecraft:pig");
        for (var entity:List.of(cow,pig)) f.seeds().add(AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(),entity)),1);
        h.runAtTickTime(100,()->{
            a.configure(BioData.cropId(cow),true,1); a.configure(BioData.cropId(pig),true,2);
            for (int i=0;i<85;i++) a.step(); var inventory=a.getMainNode().getGrid().getStorageService().getInventory();
            var c=AEItemKey.of(BioData.withIdentity(Agristics.BIO_ESSENCE.get(),cow)); var p=AEItemKey.of(BioData.withIdentity(Agristics.BIO_ESSENCE.get(),pig));
            h.assertTrue(inventory.getAvailableStacks().get(c)==1 && inventory.getAvailableStacks().get(p)==2,"Biological essence components retain separate stock targets");
            inventory.extract(c,1,Actionable.MODULATE,IActionSource.ofMachine(a)); for (int i=0;i<40;i++) a.step();
            h.assertTrue(inventory.getAvailableStacks().get(c)==1 && inventory.getAvailableStacks().get(p)==2,"Only consumed species resumes"); h.succeed();
        });
    }
    private MultiblockScenarios() {}
}

