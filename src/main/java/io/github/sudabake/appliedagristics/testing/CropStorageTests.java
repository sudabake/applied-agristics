package io.github.sudabake.appliedagristics.testing;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.logic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@PrefixGameTestTemplate(false)
public final class CropStorageTests {
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void ordinarySeedsWithOutput(GameTestHelper h) { routing(h,false,false); }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void ordinarySeedsWithCropStorage(GameTestHelper h) { routing(h,false,true); }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void meSeedsWithOutput(GameTestHelper h) { routing(h,true,false); }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void meSeedsWithCropStorage(GameTestHelper h) { routing(h,true,true); }

    private static PlanterFixture install(GameTestHelper h,boolean meSeeds,boolean cropStorage) {
        var f=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        if(!meSeeds)h.getLevel().setBlockAndUpdate(f.seeds().getBlockPos(),Agristics.PLANTER_FRAME.get().defaultBlockState());
        if(!cropStorage)h.getLevel().setBlockAndUpdate(f.vault().getBlockPos(),Agristics.PLANTER_FRAME.get().defaultBlockState());
        f.controller().structure.refresh();f.access().ensureNode();return f;
    }
    private static void withRecipe(GameTestHelper h,List<ItemStack> outputs,Runnable check) {
        var manager=h.getLevel().getRecipeManager();var original=new ArrayList<>(manager.getRecipes());
        var recipes=new ArrayList<RecipeHolder<?>>(original);
        recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(Agristics.ID,"test/crop_storage"),
            new PlantingRecipe(Ingredient.of(Items.COOKIE),Ingredient.of(Blocks.DIRT),outputs,Blocks.WHEAT.defaultBlockState(),false,200,true)));
        try{manager.replaceRecipes(recipes);CropSources.invalidate();check.run();}
        finally{manager.replaceRecipes(original);CropSources.invalidate();}
    }
    private static void routing(GameTestHelper h,boolean meSeeds,boolean cropStorage) {
        var f=install(h,meSeeds,cropStorage);
        h.runAtTickTime(100,()->withRecipe(h,List.of(new ItemStack(Items.DIAMOND,2),new ItemStack(Items.COOKIE,3)),()->{
            var seed=AEItemKey.of(new ItemStack(Items.COOKIE));var product=AEItemKey.of(new ItemStack(Items.DIAMOND));
            var bank=meSeeds?(io.github.sudabake.appliedagristics.multiblock.SeedBank)f.seeds():f.input();bank.add(seed,1);
            var a=f.controller();var id=Cultivations.id(seed.toStack(1),h.getLevel());a.configure(id,true,-1);a.tasks.get(id).progress=200;a.step();
            h.assertTrue(bank.count(seed)==(meSeeds?4:1),"Only the local ME bank automatically receives new seeds");
            h.assertTrue(f.producedCount(seed)==(meSeeds?0:3) && f.producedCount(product)==2,"All harvest counts are conserved across the four configurations");
            if(cropStorage)h.assertTrue(f.output().count(seed)==0 && f.output().count(product)==0,"Crop storage precedes output ports for all harvested items");
            var network=a.getMainNode().getGrid().getStorageService().getInventory();var source=IActionSource.ofMachine(a);
            h.assertTrue(network.insert(product,1,Actionable.SIMULATE,source)==0,"AE cannot deposit external items into crop outputs");
            h.assertTrue(network.extract(product,2,Actionable.MODULATE,source)==2 && network.extract(product,1,Actionable.MODULATE,source)==0,"AE extracts the real crop output exactly once without a disk");
            h.succeed();
        }));
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void edibleSeedsOnlyFillNeededParallel(GameTestHelper h) {
        var f=install(h,true,true);f.modules(0,1,false);
        h.runAtTickTime(100,()->withRecipe(h,List.of(new ItemStack(Items.COOKIE,3)),()->{
            var key=AEItemKey.of(new ItemStack(Items.COOKIE));f.seeds().add(key,1);
            var a=f.controller();var id=Cultivations.id(key.toStack(1),h.getLevel());a.configure(id,true,-1);
            a.tasks.get(id).progress=200;a.step();
            h.assertTrue(f.seeds().count(key)==4 && a.tasks.get(id).occupied==4 && f.producedCount(key)==0,"First three edible harvests fill the missing parallel slots");
            a.tasks.get(id).progress=200;a.step();
            h.assertTrue(f.seeds().count(key)==4 && f.producedCount(key)==3,"Further edible harvests remain available as crop products without conversion");
            h.succeed();
        }));
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void legacyVaultItemKeepsAllCountsAndPriority(GameTestHelper h) {
        var pos=new BlockPos(1,1,1);h.setBlock(pos,Agristics.ESSENCE_VAULT.get());EssenceVault vault=h.getBlockEntity(pos);
        var essence=AEItemKey.of(new ItemStack(Crops.all().getFirst().getEssenceItem()));var wheat=AEItemKey.of(new ItemStack(Items.WHEAT));
        var old=new CompoundTag();var entries=new ListTag();var entry=new CompoundTag();
        entry.put("key",essence.toTag(h.getLevel().registryAccess()));entry.putLong("count",10_000_000_000L);entries.add(entry);
        old.put("essences",entries);old.putInt("priority",37);vault.loadWithComponents(old,h.getLevel().registryAccess());
        h.assertTrue(vault.storeProduced(wheat,11,false)==11,"Legacy essence inventory can receive new non-essence crops");
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var drop=Block.getDrops(vault.getBlockState(),h.getLevel(),vault.getBlockPos(),vault,player,ItemStack.EMPTY).getFirst();
        var next=new BlockPos(2,1,1);h.setBlock(next,Agristics.ESSENCE_VAULT.get());var absolute=h.absolutePos(next);
        h.assertTrue(BlockItem.updateCustomBlockEntityTag(h.getLevel(),player,absolute,drop),"Existing portable block identity restores without registry migration");
        EssenceVault restored=h.getBlockEntity(next);var counter=new appeng.api.stacks.KeyCounter();restored.mountInventories((storage,priority)->storage.getAvailableStacks(counter));
        h.assertTrue(counter.get(essence)==10_000_000_000L && counter.get(wheat)==11 && restored.getPriority()==37,"Old essence counts, new crops and priority survive dismantling and replacing");
        h.assertTrue(restored.saveWithFullMetadata(h.getLevel().registryAccess()).getList("essences",10).size()==2,"The old NBT inventory format is still supported");h.succeed();
    }

    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void legacyIoFlagDoesNotLosePaidBatches(GameTestHelper h) {
        var f=install(h,true,true);
        h.runAtTickTime(100,()->{
            var a=f.controller();var wheat=AEItemKey.of(new ItemStack(Items.WHEAT));
            var id=Cultivations.id(new ItemStack(Items.WHEAT_SEEDS),h.getLevel());
            var old=a.saveWithFullMetadata(h.getLevel().registryAccess());old.putBoolean("directNetworkOutput",true);
            var batch=new ListTag();var item=new CompoundTag();item.put("key",wheat.toTag(h.getLevel().registryAccess()));item.putLong("count",5);batch.add(item);
            var tasks=new ListTag();var task=new CompoundTag();task.putString("id",id.toString());task.putBoolean("enabled",false);task.putLong("target",111);task.putDouble("work",123.5);task.put("batch",batch);tasks.add(task);old.put("tasks",tasks);
            a.loadWithComponents(old,h.getLevel().registryAccess());a.step();
            h.assertTrue(f.producedCount(wheat)==5 && a.tasks.get(id).progress==123.5 && a.tasks.get(id).target==111,"Legacy paid output, work and stock target survive the removed IO option");
            var saved=a.saveWithFullMetadata(h.getLevel().registryAccess());
            h.assertTrue(!saved.contains("directNetworkOutput") && !a.snapshot(0,"").contains("directNetworkOutput"),"Retired setting is absent from saves and UI snapshots");
            a.loadWithComponents(saved,h.getLevel().registryAccess());a.step();
            h.assertTrue(f.producedCount(wheat)==5,"Loading the upgraded state cannot publish the paid batch twice");h.succeed();
        });
    }
}
