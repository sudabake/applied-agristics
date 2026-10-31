package io.github.sudabake.appliedagristics.testing;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class Alpha18Tests {
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void machinesAcceptPickaxeAndBoundSelectionCost(GameTestHelper h) {
        for(var holder:Agristics.BLOCKS.getEntries()) {
            if (holder.get()==Agristics.BIO_CROP.get()) continue;
            var state=holder.get().defaultBlockState();
            h.assertTrue(new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(state)>1,"Pickaxe must work for "+holder.getId());
        }
        for(var facing:Direction.Plane.HORIZONTAL) {
            var shape=MachineShapes.get("cell_cultivator",facing,false);
            int boxes=shape.toAabbs().size();
            h.assertTrue(boxes<=24 && shape.max(Direction.Axis.Y)>1.28,"Simple outline preserves the protruding tank");
            long begin=System.nanoTime();
            for(int i=0;i<10000;i++) shape.clip(new Vec3(-1,.8,.5),new Vec3(2,.8,.5),BlockPos.ZERO);
            org.slf4j.LoggerFactory.getLogger("Alpha18Tests").info("CULTIVATOR_PICK_BENCH facing={} boxes={} rays=10000 ms={}",facing,boxes,(System.nanoTime()-begin)/1e6);
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void spawnerSampleDoesNotModifyCage(GameTestHelper h) {
        var relative=new BlockPos(1,1,1);h.setBlock(relative,Blocks.SPAWNER);var pos=h.absolutePos(relative);
        SpawnerBlockEntity cage=h.getBlockEntity(relative);
        cage.getSpawner().setEntityId(EntityType.ZOMBIE,h.getLevel(),h.getLevel().random,pos);
        var before=cage.saveWithoutMetadata(h.getLevel().registryAccess());
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Agristics.SAMPLER.get(),3));
        h.assertTrue(BioData.sampleSpawner(player,InteractionHand.MAIN_HAND,pos).consumesAction(),"Blank swab samples spawner");
        h.assertTrue(player.getMainHandItem().getCount()==2 && BioData.identity(player.getMainHandItem())==null,"Only one blank swab is used");
        int samples=0;
        for(var item:player.getInventory().items) if(ResourceLocation.parse("minecraft:zombie").equals(BioData.identity(item))) samples+=item.getCount();
        h.assertTrue(samples==1,"Exactly one correctly labeled sample");
        h.assertTrue(before.equals(cage.saveWithoutMetadata(h.getLevel().registryAccess())),"Spawner settings and delay must be unchanged");
        player.setItemInHand(InteractionHand.MAIN_HAND,BioData.withIdentity(Agristics.SAMPLER.get(),ResourceLocation.parse("minecraft:cow")));
        h.assertTrue(BioData.sampleSpawner(player,InteractionHand.MAIN_HAND,pos)==InteractionResult.PASS,"Labeled samples cannot be overwritten");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        h.assertTrue(BioData.sampleSpawner(player,InteractionHand.MAIN_HAND,pos)==InteractionResult.PASS,"Other items keep ordinary interactions");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void ordinaryOutputRoutesWithoutLoss(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        h.getLevel().setBlockAndUpdate(f.output().getBlockPos(),Agristics.PLANTER_FRAME.get().defaultBlockState());
        f.drive().getInternalInventory().setItemDirect(0,AEItems.ITEM_CELL_1K.stack());
        h.runAtTickTime(100,()->{
            var a=f.controller();h.assertTrue(a.structure.formed(),"Vault-only output is a valid structure");
            var key=AEItemKey.of(new ItemStack(Items.WHEAT));var source=IActionSource.ofMachine(a);
            var target=a.structure.outputs();var network=a.getMainNode().getGrid().getStorageService().getInventory();
            h.assertTrue(target.insert(key,11,Actionable.SIMULATE,source)==11 && network.getAvailableStacks().get(key)==0,"Simulation cannot duplicate or insert items");
            h.assertTrue(target.insert(key,11,Actionable.MODULATE,source)==11 && network.getAvailableStacks().get(key)==11,"Crop storage receives ordinary produce and exposes its exact count to AE");
            h.assertTrue(ItemStack.isSameItemSameComponents(f.drive().getInternalInventory().getStackInSlot(0),AEItems.ITEM_CELL_1K.stack()),"ME disk components remain empty while crop storage holds the output");
            h.assertTrue(network.extract(key,11,Actionable.MODULATE,source)==11,"AE can retrieve it exactly once");
            var save=a.saveWithFullMetadata(h.getLevel().registryAccess());var restored=new NetworkPlanter(a.getBlockPos(),a.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(save,h.getLevel().registryAccess());
            h.assertTrue(!restored.saveWithFullMetadata(h.getLevel().registryAccess()).contains("directNetworkOutput"),"Retired direct-output mode is absent from saved controller data");
            f.drive().getInternalInventory().setItemDirect(0,ItemStack.EMPTY);
            h.assertTrue(target.insert(key,11,Actionable.MODULATE,source)==11,"Crop storage accepts output even with no ME disk");
            f.seeds().add(AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS)),1);
            var crop=Cultivations.seed(new ItemStack(Items.WHEAT_SEEDS),h.getLevel(),a.getBlockPos());a.configure(crop.getId(),true,-1);a.step();
            h.assertTrue(a.tasks.get(crop.getId()).progress>0 && !a.tasks.get(crop.getId()).status.equals("network_output_full"),"No disk or retired IO flag can block local crop storage");h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void cropFilterAndThreeColumnSeedScroll(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        h.runAtTickTime(100,()->{
            var a=f.controller();var bank=f.seeds();
            bank.add(AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS)),2);
            var crop=Crops.all().getFirst();bank.add(AEItemKey.of(new ItemStack(crop.getSeedsItem())),2);
            var ordinary=Cultivations.seed(new ItemStack(Items.WHEAT_SEEDS),h.getLevel(),a.getBlockPos());
            a.configure(ordinary.getId(),true,-1);a.configure(crop.getId(),true,-1);
            var rows=a.snapshot(0,"hide_ordinary").getList("rows",10);
            h.assertTrue(rows.size()==1 && rows.getCompound(0).getString("id").equals(crop.getId().toString()),"Hiding ordinary crops keeps essence crops visible");
            h.assertTrue(a.tasks.get(crop.getId()).enabled && a.tasks.get(ordinary.getId()).enabled,"Filtering must not disable hidden production");
            for(var id:java.util.List.of("cow","pig","sheep","chicken","zombie","skeleton","creeper","spider","enderman","witch","blaze","ghast","slime","rabbit","fox"))
                bank.add(AEItemKey.of(BioData.withIdentity(Agristics.BIO_SEED.get(),ResourceLocation.parse("minecraft:"+id))),1);
            var first=bank.snapshot(0,"");var second=bank.snapshot(1,"");
            var r0=first.getList("rows",10);var r1=second.getList("rows",10);
            h.assertTrue(r0.size()==12 && r1.size()==12 && first.getInt("pages")==3,"Three columns by four rows with three scroll positions for seventeen types");
            h.assertTrue(r0.getCompound(3).getCompound("seed").equals(r1.getCompound(0).getCompound("seed")),"Scrolling advances by exactly one visual row");
            h.assertTrue(bank.snapshot(10000,"").getList("rows",10).size()==11,"End scroll stays inside the list");h.succeed();
        });
    }
}
