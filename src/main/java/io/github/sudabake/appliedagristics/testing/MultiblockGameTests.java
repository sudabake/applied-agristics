package io.github.sudabake.appliedagristics.testing;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Opt-in world tests for real inventories, network boundaries and native altar recipes. */
@PrefixGameTestTemplate(false)
public final class MultiblockGameTests {
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void earlyPortsWithoutBulkStorage(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),false);
        f.input().items.setStackInSlot(0,new ItemStack(Items.WHEAT_SEEDS));
        h.runAtTickTime(100,()->{
            var a=f.controller(); h.assertTrue(a.structure.formed() && a.getMainNode().isActive(),"Basic structure forms without late seed bay or essence vault");
            h.assertTrue(h.getLevel().getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST,a.getBlockPos())==null,"Controller does not connect directly");
            h.assertTrue(h.getLevel().getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST,f.input().getBlockPos())==null,"Input port does not connect directly");
            var crop=Cultivations.seed(new ItemStack(Items.WHEAT_SEEDS),h.getLevel(),a.getBlockPos()); a.configure(crop.getId(),true,1); a.step();
            h.assertTrue(f.input().used(AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS)))==1,"Input seed is reserved");
            h.assertTrue(f.input().automation(Direction.DOWN).extractItem(0,64,true).isEmpty(),"Automation cannot steal a working input seed");
            for (int i=0;i<40;i++) a.step();
            h.assertTrue(f.output().count(AEItemKey.of(new ItemStack(Items.WHEAT)))>=1,"Ordinary produce enters output port");
            var network=a.getMainNode().getGrid().getStorageService().getInventory(); var source=IActionSource.ofMachine(a);
            h.assertTrue(network.extract(AEItemKey.of(new ItemStack(Items.WHEAT)),1,Actionable.MODULATE,source)==1,"AE can extract output buffer");
            h.assertTrue(network.insert(AEItemKey.of(new ItemStack(Items.WHEAT)),1,Actionable.MODULATE,source)==0,"AE cannot deposit into output buffer");
            a.configure(crop.getId(),false,1); h.assertTrue(f.input().automation(Direction.DOWN).extractItem(0,1,true).getCount()==1,"Stopping releases input seed"); h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void configuredPortsAndFullOutput(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),false);
        h.getLevel().setBlockAndUpdate(f.output().getBlockPos().north(),Blocks.CHEST.defaultBlockState());
        h.getLevel().setBlockAndUpdate(f.input().getBlockPos().north(),Blocks.CHEST.defaultBlockState());
        h.runAtTickTime(100,()->{
            var port=f.output(); port.items.setStackInSlot(0,new ItemStack(Items.WHEAT,12)); port.toggleAutoOutput(); port.step();
            var chest=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,port.getBlockPos().north(),Direction.SOUTH);
            h.assertTrue(chest!=null && chest.getStackInSlot(0).is(Items.WHEAT) && chest.getStackInSlot(0).getCount()==12,"Configured auto output pushes to neighboring inventory");
            h.assertTrue(port.items.getSlots()==16 && port.items.getStackInSlot(0).isEmpty(),"Output has sixteen slots and moves actual items once");
            h.assertTrue(port.automation(Direction.NORTH).insertItem(0,new ItemStack(Items.DIRT),true).getCount()==1,"Output rejects external insertion");
            var save=port.saveWithFullMetadata(h.getLevel().registryAccess()); var restored=new ItemPort(port.getBlockPos(),port.getBlockState(),false); restored.setLevel(h.getLevel()); restored.loadWithComponents(save,h.getLevel().registryAccess());
            h.assertTrue(restored.snapshot(0,"").getBoolean("autoOutput"),"Automatic output survives serialization");
            var input=f.input().automation(Direction.NORTH);
            h.assertTrue(input.insertItem(0,new ItemStack(Items.WHEAT_SEEDS,10),false).isEmpty()
                && input.insertItem(1,new ItemStack(Items.DIRT,5),false).getCount()==5,"Seed storage accepts piped seeds and rejects dirt without IO settings");
            for (int i=0;i<16;i++) port.items.setStackInSlot(i,new ItemStack(Items.DIRT,64));
            f.input().items.setStackInSlot(0,new ItemStack(Items.WHEAT_SEEDS)); var crop=Cultivations.seed(new ItemStack(Items.WHEAT_SEEDS),h.getLevel(),f.controller().getBlockPos());
            f.controller().configure(crop.getId(),true,-1); f.controller().step();
            h.assertTrue(f.controller().tasks.get(crop.getId()).status.equals("output_full") && f.controller().tasks.get(crop.getId()).progress==0,"Full finite output pauses production"); h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void wrenchRetainsLargeStorageAndReconnects(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        h.runAtTickTime(100,()->{
            var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,AEItems.CERTUS_QUARTZ_WRENCH.stack());
            var pos=f.output().getBlockPos(); var facing=f.output().outputFace();
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.NORTH,pos,false);
            var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos,hit);
            MachineWrench.wrench(event);
            h.assertTrue(event.isCanceled() && f.output().outputFace()==facing,"Wrench does not alter the fixed output face of a formed array");
            var essence=AEItemKey.of(new ItemStack(Crops.all().getFirst().getEssenceItem()));
            f.vault().storeProduced(essence,10_000_000_000L,false); var expected=f.vault().saveWithFullMetadata(h.getLevel().registryAccess()).getList("essences",10);
            pos=f.vault().getBlockPos(); player.setShiftKeyDown(true);
            hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.NORTH,pos,false);
            MachineWrench.wrench(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos,hit));
            h.assertTrue(h.getLevel().isEmptyBlock(pos) && !f.controller().structure.formed() && !f.access().connectedStructure(),"Dismantling a member removes network service");
            ItemStack saved=ItemStack.EMPTY; int count=0;
            for (var stack:player.getInventory().items) if (stack.is(Agristics.ESSENCE_VAULT.get().asItem())) { saved=stack; count+=stack.getCount(); }
            h.assertTrue(count==1 && saved.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA),"Dismantling returns exactly one data-bearing block");
            h.getLevel().setBlockAndUpdate(pos,Agristics.ESSENCE_VAULT.get().defaultBlockState());
            h.assertTrue(BlockItem.updateCustomBlockEntityTag(h.getLevel(),player,pos,saved),"Placement restores the saved block data");
            var replacement=(EssenceVault)h.getLevel().getBlockEntity(pos);
            h.assertTrue(replacement.saveWithFullMetadata(h.getLevel().registryAccess()).getList("essences",10).equals(expected),"Ten billion essence survives wrench removal and replacement exactly");
            f.controller().structure.refresh(); f.access().ensureNode();
            h.assertTrue(f.controller().structure.formed() && f.access().connectedStructure(),"Replacing the member reforms and reconnects the array"); h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=120)
    public static void allFourFacingsHaveConsistentSkinCells(GameTestHelper h) {
        var level=h.getLevel(); var center=h.absolutePos(new BlockPos(6,1,6));
        for (var facing:Direction.Plane.HORIZONTAL) {
            var right=facing.getClockWise(); var back=facing.getOpposite(); var origin=center.below().relative(right,-1);
            var positions=new java.util.ArrayList<BlockPos>();
            for (int y=0;y<4;y++) for (int z=0;z<3;z++) for (int x=0;x<3;x++) {
                var pos=origin.relative(right,x).relative(back,z).above(y); positions.add(pos);
                var block=x==1 && z==1 && y>0 && y<3 ? Agristics.CULTIVATION_CHAMBER.get() : Agristics.PLANTER_FRAME.get();
                level.setBlockAndUpdate(pos,block.defaultBlockState());
            }
            level.setBlockAndUpdate(center.relative(right),Agristics.NETWORK_ACCESS.get().defaultBlockState());
            level.setBlockAndUpdate(center.relative(right,-1).below(),Agristics.INPUT_PORT.get().defaultBlockState());
            level.setBlockAndUpdate(center.below(),Agristics.OUTPUT_PORT.get().defaultBlockState());
            level.setBlockAndUpdate(center,Agristics.NETWORK.get().defaultBlockState().setValue(PlanterPartBlock.FACING,facing));
            var controller=(NetworkPlanter)level.getBlockEntity(center); controller.structure.refresh();
            h.assertTrue(controller.structure.formed(),"Array forms facing "+facing);
            for (var outside:java.util.List.of(center.relative(facing),origin.relative(right,-1),origin.relative(right,3),origin.relative(back,3),center.below(2),center.above(3))) {
                level.setBlockAndUpdate(outside,Agristics.PLANTER_FRAME.get().defaultBlockState());
                h.assertTrue(level.getBlockState(center).getValue(PlanterPartBlock.FORMED),"External frame placement keeps the formed skin facing "+facing);
                controller.structure.refresh();
                level.removeBlock(outside,false);
                h.assertTrue(level.getBlockState(center).getValue(PlanterPartBlock.FORMED) && controller.structure.formed(),"External frame removal keeps the structure facing "+facing);
            }
            for (int i=0;i<positions.size();i++) {
                var state=level.getBlockState(positions.get(i));
                h.assertTrue(state.getValue(PlanterPartBlock.FORMED) && state.getValue(PlanterPartBlock.CELL)==i && state.getValue(PlanterPartBlock.FACING)==facing,"Skin cells match structure orientation "+facing+" index "+i);
            }
            for (var pos:positions) level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void meInfusionUsesRealPedestalsAndFourCards(GameTestHelper h) {
        var pos=new BlockPos(3,1,3); h.setBlock(pos,Agristics.ME_INFUSION.get()); h.setBlock(pos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        MEInfusionAltar altar=h.getBlockEntity(pos);
        for (var pedestal:altar.pedestalPositions()) h.getLevel().setBlockAndUpdate(pedestal,com.blakebr0.mysticalagriculture.init.ModBlocks.INFUSION_PEDESTAL.get().defaultBlockState());
        h.runAtTickTime(100,()->{
            var holder=h.getLevel().getRecipeManager().getAllRecipesFor(com.blakebr0.mysticalagriculture.init.ModRecipeTypes.INFUSION.get()).stream()
                .filter(r->!r.value().getAltarIngredient().isEmpty() && r.value().getAltarIngredient().getItems().length>0 && r.value().getIngredients().size()==8).findFirst().orElseThrow();
            altar.items.setStackInSlot(0,holder.value().getAltarIngredient().getItems()[0].copyWithCount(1));
            for (int i=0;i<8;i++) {
                var ingredient=holder.value().getIngredients().get(i);
                var pedestal=(com.blakebr0.mysticalagriculture.tileentity.InfusionPedestalTileEntity)h.getLevel().getBlockEntity(altar.pedestalPositions().get(i));
                pedestal.getInventory().setStackInSlot(0,ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.getItems()[0].copyWithCount(1));
            }
            for (int i=2;i<6;i++) altar.items.insertItem(i,AEItems.SPEED_CARD.stack(2),false);
            h.assertTrue(altar.accelerationCards()==4 && altar.speed()==16,"Four separate slots accept one speed card each");
            for (int i=0;i<6;i++) altar.step(); h.assertTrue(altar.items.getStackInSlot(1).isEmpty(),"Six accelerated ticks do not finish a 100-tick infusion");
            altar.step(); h.assertTrue(ItemStack.isSameItemSameComponents(altar.items.getStackInSlot(1),holder.value().getResultItem(h.getLevel().registryAccess())),"Seventh accelerated tick crafts the native infusion result"); h.succeed();
        });
    }
}
