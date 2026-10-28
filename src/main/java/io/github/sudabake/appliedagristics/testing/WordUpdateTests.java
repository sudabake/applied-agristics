package io.github.sudabake.appliedagristics.testing;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.*;
import io.github.sudabake.appliedagristics.block.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@PrefixGameTestTemplate(false)
public final class WordUpdateTests {
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=200)
    public static void meStorageTakesOverAndReturnsFiniteClaims(GameTestHelper h) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),false);
        f.modules(0,4,false);
        var seed=AEItemKey.of(new ItemStack(Items.WHEAT_SEEDS));
        for (int i=0;i<4;i++) f.input().items.setStackInSlot(i,seed.toStack(64));
        h.runAtTickTime(100,()->{
            var a=f.controller();var crop=Cultivations.seed(seed.toStack(1),h.getLevel(),a.getBlockPos());a.configure(crop.getId(),true,0);a.step();
            h.assertTrue(a.parallel()==256 && a.tasks.get(crop.getId()).occupied==256,"Four modules occupy 256 actual seeds in four finite slots");
            h.assertTrue(f.input().automation(Direction.UP).extractItem(0,64,true).isEmpty(),"Active finite seeds cannot be extracted");
            var pos=a.getBlockPos().west();h.getLevel().setBlockAndUpdate(pos,Agristics.SEEDS.get().defaultBlockState());
            a.structure.refresh();a.step();
            h.assertTrue(a.seedStores().size()==1 && a.seedStores().getFirst() instanceof SeedStorage,"An installed ME seed store takes precedence even when empty");
            h.assertTrue(f.input().used(seed)==0 && f.input().automation(Direction.DOWN).extractItem(0,64,true).getCount()==64,"Switching source releases every old finite claim");
            var bank=(SeedStorage)h.getLevel().getBlockEntity(pos);bank.add(seed,16);
            h.runAtTickTime(130,()->{
                a.step();
                h.assertTrue(a.tasks.get(crop.getId()).occupied==16 && bank.used(seed)==16 && f.input().count(seed)==256,"Only ME seeds now contribute parallel occupancy");
                h.getLevel().setBlockAndUpdate(pos,Agristics.PLANTER_FRAME.get().defaultBlockState());a.structure.refresh();
                h.runAtTickTime(160,()->{
                    a.step();h.assertTrue(a.tasks.get(crop.getId()).occupied==256,"Removing the ME bay falls back to finite seed storage");h.succeed();
                });
            });
        });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void vaultPriorityIsMountedAndPersisted(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);h.setBlock(pos,Agristics.ESSENCE_VAULT.get());EssenceVault vault=h.getBlockEntity(pos);
        var key=AEItemKey.of(new ItemStack(Crops.all().getFirst().getEssenceItem()));vault.storeProduced(key,100,false);vault.setPriority(-1234);
        int[] mounted={0};
        vault.mountInventories((storage,priority)->{
            mounted[0]++;h.assertTrue(priority==-1234,"Real AE mount receives the configured priority");
            h.assertTrue(storage.insert(key,1,Actionable.SIMULATE,IActionSource.empty())==0,"Priority cannot allow AE insertion");
        });
        var saved=vault.saveWithFullMetadata(h.getLevel().registryAccess());var copy=new EssenceVault(vault.getBlockPos(),vault.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(mounted[0]==1 && copy.getPriority()==-1234 && copy.snapshot(0,"").getInt("priority")==-1234,"Priority survives save and screen synchronization");h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void fieldPlantingPreservesSpeciesAndDrops(GameTestHelper h) {
        var soil=h.absolutePos(new BlockPos(2,1,2));h.getLevel().setBlockAndUpdate(soil,Blocks.FARMLAND.defaultBlockState());
        var cow=ResourceLocation.parse("minecraft:cow");var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(soil.getCenter());
        player.setItemInHand(InteractionHand.MAIN_HAND,BioData.withIdentity(Agristics.BIO_SEED.get(),cow).copyWithCount(2));
        var hit=new BlockHitResult(soil.getCenter(),Direction.UP,soil,false);
        var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));var pos=soil.above();
        h.assertTrue(result.consumesAction() && player.getMainHandItem().getCount()==1 && h.getLevel().getBlockState(pos).is(Agristics.BIO_CROP.get()),"Ordinary item use plants one identified seed on farmland");
        var crop=(BioCropEntity)h.getLevel().getBlockEntity(pos);h.assertTrue(cow.equals(crop.identity()),"Placement transfers species to plant");
        var saved=crop.saveWithFullMetadata(h.getLevel().registryAccess());crop.loadWithComponents(saved,h.getLevel().registryAccess());
        var immature=Block.getDrops(crop.getBlockState(),h.getLevel(),pos,crop);
        h.assertTrue(immature.size()==1 && cow.equals(BioData.identity(immature.getFirst())) && immature.getFirst().is(Agristics.BIO_SEED.get()),"Immature plant returns only its own species seed");
        h.getLevel().setBlockAndUpdate(pos,Agristics.BIO_CROP.get().getStateForAge(7));crop=(BioCropEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(cow.equals(crop.identity()),"Changing age preserves block entity identity");
        var drops=Block.getDrops(crop.getBlockState(),h.getLevel(),pos,crop);
        h.assertTrue(drops.stream().allMatch(s->cow.equals(BioData.identity(s))) && drops.stream().filter(s->s.is(Agristics.BIO_ESSENCE.get())).mapToInt(ItemStack::getCount).sum()==1,"Mature field harvest yields correctly identified essence and replanting seeds");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void requestedRecipesMatchActualGrids(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        var sampler=CraftingInput.of(2,2,List.of(ItemStack.EMPTY,new ItemStack(Items.WHITE_WOOL),new ItemStack(Items.STICK),ItemStack.EMPTY));
        var sampleRecipe=manager.getRecipeFor(RecipeType.CRAFTING,sampler,h.getLevel()).orElseThrow().value();
        h.assertTrue(sampleRecipe.assemble(sampler,h.getLevel().registryAccess()).is(Agristics.SAMPLER.get()),"Diagonal wool/stick pattern makes sampler");
        var capsule=CraftingInput.of(3,3,List.of(ItemStack.EMPTY,new ItemStack(Items.IRON_NUGGET),ItemStack.EMPTY,new ItemStack(Items.GLASS),ItemStack.EMPTY,new ItemStack(Items.GLASS),new ItemStack(Items.IRON_NUGGET),ItemStack.EMPTY,ItemStack.EMPTY));
        var capsuleRecipe=manager.getRecipeFor(RecipeType.CRAFTING,capsule,h.getLevel()).orElseThrow().value();var output=capsuleRecipe.assemble(capsule,h.getLevel().registryAccess());
        h.assertTrue(output.is(Agristics.CAPSULE.get()) && output.getCount()==1,"Requested capsule slots produce exactly one");
        var storage=CraftingInput.of(1,3,List.of(new ItemStack(Items.HOPPER),new ItemStack(Agristics.PLANTER_FRAME.get()),new ItemStack(Items.CHEST)));
        h.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING,storage,h.getLevel()).orElseThrow().value().assemble(storage,h.getLevel().registryAccess()).is(Agristics.INPUT_PORT.get().asItem()),"Hopper/frame/chest produces finite seed storage");h.succeed();
    }
}
