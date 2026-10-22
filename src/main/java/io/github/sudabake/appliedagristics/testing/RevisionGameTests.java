package io.github.sudabake.appliedagristics.testing;

import appeng.api.config.Actionable;
import appeng.api.stacks.*;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.security.IActionSource;
import appeng.core.definitions.*;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@PrefixGameTestTemplate(false)
public final class RevisionGameTests {
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void survivalRecipesLoadAndCraftAllEquipment(GameTestHelper h) {
        var level=h.getLevel(); var registries=level.registryAccess();
        var recipes=level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
        var derived=Set.of(Agristics.BIO_SEED.get(),Agristics.BIO_ESSENCE.get(),Agristics.CULTURE.get(),Agristics.EXTRACT.get());
        int checked=0;
        for(var entry:Agristics.ITEMS.getEntries()) {
            var item=entry.get(); if(derived.contains(item)) continue;
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            var holder=recipes.stream().filter(r->r.id().equals(id)).findFirst().orElseThrow(()->new IllegalStateException("Missing survival recipe: "+id));
            var recipe=holder.value(); var ingredients=recipe.getIngredients();
            var stacks=new ArrayList<ItemStack>();
            for(var ingredient:ingredients) {
                h.assertTrue(ingredient.isEmpty() || ingredient.getItems().length>0,"Ingredient or tag resolves for "+id);
                stacks.add(ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getItems()[0].copyWithCount(1));
            }
            int width=3,height=3;
            if(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) { width=shaped.getWidth();height=shaped.getHeight(); }
            else while(stacks.size()<9) stacks.add(ItemStack.EMPTY);
            var input=net.minecraft.world.item.crafting.CraftingInput.of(width,height,stacks);
            h.assertTrue(recipe.matches(input,level),"Real recipe accepts its ingredients: "+id);
            var actual=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,level).orElseThrow();
            h.assertTrue(actual.id().equals(id),"Workbench selects the intended recipe without a collision: "+id);
            int expected=item==Agristics.NUTRIENT.get()?8:item==Agristics.CAPSULE.get()||item==Agristics.SAMPLER.get()||item==Agristics.PLANTER_FRAME.get().asItem()?4:1;
            var result=actual.value().assemble(input,registries);
            h.assertTrue(result.is(item)&&result.getCount()==expected,"Survival result and batch count: "+id);
            checked++;
        }
        h.assertTrue(checked==17,"All fourteen blocks and three tools/consumables are craftable");
        org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("SURVIVAL_RECIPES_PASSED equipment=17 actualWorkbench=true ingredientTags=true batchCounts=true");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void revalidationKeepsNetworkAndSeedClaims(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        var seed=AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS)); f.seeds().add(seed,1);
        h.runAtTickTime(100,()->{
            var controller=f.controller(); var crop=Cultivations.seed(seed.toStack(1),h.getLevel(),controller.getBlockPos());
            controller.configure(crop.getId(),true,-1); controller.step();
            controller.updateWorkVisual(); h.assertTrue(controller.workingVisual() && controller.getUpdateTag(h.getLevel().registryAccess()).getBoolean("working"),"Production synchronizes a working visual");
            var node=f.access().getMainNode().getNode(); var task=controller.tasks.get(crop.getId()); double progress=task.progress;
            h.assertTrue(node!=null && node.isActive() && task.occupied==1,"Fixture has an active network and working seed");
            var outside=controller.getBlockPos().north();
            for (int i=0;i<8;i++) {
                h.getLevel().setBlockAndUpdate(outside,Agristics.PLANTER_FRAME.get().defaultBlockState());
                h.getLevel().removeBlock(outside,false);
            }
            h.assertTrue(h.getLevel().getBlockState(controller.getBlockPos()).getValue(PlanterPartBlock.FORMED) && f.access().getMainNode().getNode()==node && task.occupied==1 && task.progress==progress,"Nearby placement never clears skin, network, claims or progress");
            // Real neighbor notifications and valid in-structure module changes must validate without tearing down.
            var module=controller.getBlockPos().south(2);
            for (int i=0;i<8;i++) io.github.sudabake.appliedagristics.multiblock.PlanterStructure.changed(h.getLevel(),module);
            controller.structure.refresh();
            h.assertTrue(f.access().getMainNode().getNode()==node && task.occupied==1 && task.progress==progress,"Repeated valid notifications preserve the same active node and seed claim");
            h.getLevel().setBlockAndUpdate(module,Agristics.SPEED_MODULE.get().defaultBlockState()); controller.structure.refresh();
            h.assertTrue(controller.structure.speedModules()==1 && f.access().getMainNode().getNode()==node && task.occupied==1 && task.progress==progress,"Valid module replacement updates its effect without disconnecting");
            h.getLevel().removeBlock(module,false);
            h.assertTrue(!controller.structure.formed() && !f.access().connectedStructure() && task.occupied==0 && f.seeds().used(seed)==0,"A genuinely missing shell stops service and releases seeds");
            h.assertTrue(!controller.workingVisual(),"Breaking the structure immediately stops the flowing lights");
            h.assertTrue(!h.getLevel().getBlockState(controller.getBlockPos()).getValue(PlanterPartBlock.FORMED),"A broken structure clears its formed skin");
            h.getLevel().setBlockAndUpdate(module,Agristics.PLANTER_FRAME.get().defaultBlockState()); controller.structure.refresh(); f.access().ensureNode();
            h.assertTrue(controller.structure.formed() && f.access().connectedStructure(),"Repair restores the formed structure and access binding");
        });
        h.runAtTickTime(180,()->{ h.assertTrue(f.access().getMainNode().isActive(),"Repaired structure reconnects to AE"); h.succeed(); });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void ordinaryShellStacksAndDropsInventoryOnce(GameTestHelper h) {
        var pos=new BlockPos(2,1,2); h.setBlock(pos,Agristics.EARLY.get()); EarlyPlanter machine=h.getBlockEntity(pos);
        machine.items.setStackInSlot(0,new ItemStack(Items.WHEAT_SEEDS)); machine.items.setStackInSlot(1,new ItemStack(Blocks.FARMLAND)); machine.items.setStackInSlot(2,new ItemStack(Items.WHEAT,7));
        var shell=Block.getDrops(machine.getBlockState(),h.getLevel(),machine.getBlockPos(),machine).getFirst();
        h.assertTrue(!shell.has(DataComponents.BLOCK_ENTITY_DATA) && ItemStack.isSameItemSameComponents(shell,new ItemStack(Agristics.EARLY.get())),"Ordinary machine shell is data-free and stacks with a new shell");
        h.getLevel().destroyBlock(machine.getBlockPos(),true);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(machine.getBlockPos()).inflate(1));
        int wheat=0,seeds=0,soil=0,shells=0;
        for (var entity:drops) { var stack=entity.getItem(); if (stack.is(Items.WHEAT)) wheat+=stack.getCount(); if (stack.is(Items.WHEAT_SEEDS)) seeds+=stack.getCount(); if (stack.is(Blocks.FARMLAND.asItem())) soil+=stack.getCount(); if (stack.is(Agristics.EARLY.get().asItem())) shells+=stack.getCount(); }
        h.assertTrue(wheat==7 && seeds==1 && soil==1 && shells==1,"Normal breaking drops actual inventory exactly once plus one bare shell");
        h.assertTrue(!Agristics.NETWORK.get().preservesContents() && !Agristics.INPUT_PORT.get().preservesContents() && !Agristics.OUTPUT_PORT.get().preservesContents() && Agristics.SEEDS.get().preservesContents() && Agristics.ESSENCE_VAULT.get().preservesContents(),"Only the two agreed bulk storages retain data"); h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void soilPacketAndOpenTrayCollision(GameTestHelper h) {
        var pos=new BlockPos(1,1,1); h.setBlock(pos,Agristics.EARLY.get()); EarlyPlanter machine=h.getBlockEntity(pos);
        h.assertTrue(machine.getUpdateTag(h.getLevel().registryAccess()).isEmpty(),"Empty planter has no fixed soil display");
        h.assertTrue(machine.items.insertItem(1,new ItemStack(Blocks.DIRT),false).isEmpty(),"Soil slot accepts actual dirt for display");
        var tag=machine.getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(ItemStack.parseOptional(h.getLevel().registryAccess(),tag.getCompound("displaySoil")).is(Items.DIRT),"Display packet reflects the actual inserted soil");
        var shape=machine.getBlockState().getCollisionShape(h.getLevel(),machine.getBlockPos());
        h.assertTrue(shape.max(Direction.Axis.Y)==10.0/16 && shape.toAabbs().stream().noneMatch(box->box.contains(.5,.5,.5))
            && shape.toAabbs().stream().anyMatch(box->box.contains(.02,.5,.5)),"Planter collision follows the new low rim while the central tray remains open");
        machine.items.setStackInSlot(1,ItemStack.EMPTY); h.assertTrue(machine.getUpdateTag(h.getLevel().registryAccess()).isEmpty(),"Removing soil clears the tray display"); h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void creativeBreakKeepsPortableStorage(GameTestHelper h) {
        var pos=new BlockPos(2,1,2); h.setBlock(pos,Agristics.ESSENCE_VAULT.get()); EssenceVault vault=h.getBlockEntity(pos);
        var crop=Crops.all().getFirst(); vault.storeProduced(AEItemKey.of(new ItemStack(crop.getEssenceItem())),10_000_000_000L,false);
        var block=Agristics.ESSENCE_VAULT.get(); var state=vault.getBlockState();
        block.playerWillDestroy(h.getLevel(),vault.getBlockPos(),state,h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE));
        h.getLevel().removeBlock(vault.getBlockPos(),false);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(vault.getBlockPos()).inflate(1));
        h.assertTrue(drops.size()==1 && drops.getFirst().getItem().is(block.asItem()),"Creative break emits exactly one portable storage");
        var saved=drops.getFirst().getItem().get(DataComponents.BLOCK_ENTITY_DATA);
        h.assertTrue(saved!=null && saved.copyTag().getList("essences",10).getCompound(0).getLong("count")==10_000_000_000L,"Creative break preserves the full essence count"); h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void bulkStoragesReplaceBothFinitePorts(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        h.getLevel().setBlockAndUpdate(f.input().getBlockPos(),Agristics.PLANTER_FRAME.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(f.output().getBlockPos(),Agristics.PLANTER_FRAME.get().defaultBlockState());
        f.controller().structure.refresh(); f.access().ensureNode(); var crop=Crops.all().getFirst(); f.seeds().add(AEItemKey.of(new ItemStack(crop.getSeedsItem())),1);
        h.runAtTickTime(100,()->{
            var machine=f.controller(); h.assertTrue(machine.structure.formed() && machine.getMainNode().isActive(),"Bulk seed bay and essence vault form without redundant finite ports");
            machine.configure(crop.getId(),true,1); for (int i=0;i<40;i++) machine.step();
            var inventory=machine.getMainNode().getGrid().getStorageService().getInventory();
            h.assertTrue(inventory.getAvailableStacks().get(AEItemKey.of(new ItemStack(crop.getEssenceItem())))>=1,"Essence is produced into the vault without a finite output port");
            f.seeds().add(AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS)),1); var ordinary=Cultivations.seed(new ItemStack(Items.WHEAT_SEEDS),h.getLevel(),machine.getBlockPos());
            machine.configure(ordinary.getId(),true,-1); machine.step();
            h.assertTrue(machine.tasks.get(ordinary.getId()).status.equals("requires_output_port"),"Ordinary produce pauses with a clear reason when the only output accepts essence"); h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void aePatternFillsEightRealPedestalsAndReturnsResult(GameTestHelper h) {
        var pos=new BlockPos(4,1,4); h.setBlock(pos,Agristics.ME_INFUSION.get()); h.setBlock(pos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block()); h.setBlock(pos.west(2),AEBlocks.DRIVE.block());
        appeng.blockentity.storage.DriveBlockEntity drive=h.getBlockEntity(pos.west(2)); drive.getInternalInventory().setItemDirect(0,AEItems.ITEM_CELL_1K.stack()); MEInfusionAltar altar=h.getBlockEntity(pos);
        for (var p:altar.pedestalPositions()) h.getLevel().setBlockAndUpdate(p,com.blakebr0.mysticalagriculture.init.ModBlocks.INFUSION_PEDESTAL.get().defaultBlockState());
        h.runAtTickTime(100,()->{
            var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(com.blakebr0.mysticalagriculture.init.ModRecipeTypes.INFUSION.get()).stream().map(r->r.value()).filter(r->r.getIngredients().size()==8 && !r.getAltarIngredient().isEmpty()).findFirst().orElseThrow();
            var ingredients=new LinkedHashMap<AEItemKey,Long>(); ingredients.merge(AEItemKey.of(recipe.getAltarIngredient().getItems()[0]),1L,Long::sum);
            for (var ingredient:recipe.getIngredients()) if (!ingredient.isEmpty()) ingredients.merge(AEItemKey.of(ingredient.getItems()[0]),1L,Long::sum);
            var output=recipe.getResultItem(h.getLevel().registryAccess());
            var encoded=PatternDetailsHelper.encodeProcessingPattern(ingredients.entrySet().stream().map(e->new GenericStack(e.getKey(),e.getValue())).toList(),List.of(new GenericStack(AEItemKey.of(output),output.getCount())));
            var pattern=PatternDetailsHelper.decodePattern(encoded,h.getLevel()); var counters=new KeyCounter[pattern.getInputs().length];
            for (int i=0;i<counters.length;i++) { counters[i]=new KeyCounter(); var input=pattern.getInputs()[i]; var stack=input.getPossibleInputs()[0]; counters[i].add(stack.what(),stack.amount()*input.getMultiplier()); }
            var missing=altar.pedestalPositions().getFirst(); h.getLevel().removeBlock(missing,false);
            h.assertTrue(!altar.pushPattern(pattern,counters,Direction.NORTH) && altar.items.getStackInSlot(0).isEmpty(),"Missing real pedestal rejects the whole push without taking materials");
            h.getLevel().setBlockAndUpdate(missing,com.blakebr0.mysticalagriculture.init.ModBlocks.INFUSION_PEDESTAL.get().defaultBlockState());
            h.assertTrue(altar.pushPattern(pattern,counters,Direction.NORTH),"Native AE processing pattern is accepted");
            h.assertTrue(!altar.items.getStackInSlot(0).isEmpty() && altar.pedestalPositions().stream().allMatch(p->h.getLevel().getBlockEntity(p) instanceof com.blakebr0.mysticalagriculture.tileentity.InfusionPedestalTileEntity),"Center and eight real pedestal slots hold the recipe");
            h.assertTrue(!altar.pushPattern(pattern,counters,Direction.NORTH),"A running altar rejects duplicate pushes");
            for (int i=2;i<6;i++) altar.items.setStackInSlot(i,AEItems.SPEED_CARD.stack());
            for (int i=0;i<8;i++) altar.step();
            h.assertTrue(altar.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks().get(AEItemKey.of(output))>=output.getCount(),"Ordered result returns to AE even with manual auto-output disabled"); h.succeed();
        });
    }
}
