package io.github.sudabake.appliedagristics.testing;

import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.*;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.logic.*;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@PrefixGameTestTemplate(false)
public final class RemainingWorkGameTests {
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void fallbackReproductionAndIdentity(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,1,1)); var cow=ResourceLocation.parse("minecraft:cow");
        var seed=BioData.withIdentity(Agristics.BIO_SEED.get(),cow);
        var result=Harvests.networkBatch(h.getLevel(),pos,seed,100000,new HashMap<>());
        h.assertTrue(result.get(AEItemKey.of(BioData.withIdentity(Agristics.BIO_ESSENCE.get(),cow)))==100000,"Biological essence yield is unchanged");
        long count=result.getOrDefault(AEItemKey.of(seed),0L);
        h.assertTrue(count>9200 && count<10800,"Zero-reproduction biological seeds gain ten-percent independent opportunities");
        h.assertTrue(NativeSeedYield.reproduces(h.getLevel(),new ItemStack(Items.WHEAT_SEEDS),new ItemStack(Blocks.FARMLAND)),"Native wheat can reproduce even if an individual roll gives no seed");
        h.assertTrue(!NativeSeedYield.reproduces(h.getLevel(),new ItemStack(Items.MELON_SEEDS),new ItemStack(Blocks.FARMLAND)),"Harvesting stem fruit does not produce seeds");
        var config=com.blakebr0.mysticalagriculture.config.ModConfigs.SECONDARY_SEED_DROPS; boolean previous=config.get();
        try {
            config.set(false); var crop=Crops.all().getFirst(); var stack=new ItemStack(crop.getSeedsItem());
            var batch=Harvests.networkBatch(h.getLevel(),pos,stack,100000,new HashMap<>());
            long extra=batch.getOrDefault(AEItemKey.of(stack),0L);
            h.assertTrue(extra>9200 && extra<10800,"MA with disabled natural extras receives the same fallback");
        } finally { config.set(previous); }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void displayPacketsKeepOnlySeedAndClear(GameTestHelper h) {
        h.setBlock(1,1,1,Agristics.EARLY.get()); EarlyPlanter machine=h.getBlockEntity(new BlockPos(1,1,1));
        var seed=BioData.withIdentity(Agristics.BIO_SEED.get(),ResourceLocation.parse("minecraft:wither")); machine.items.setStackInSlot(0,seed);
        var tag=machine.getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(tag.size()==1 && ItemStack.isSameItemSameComponents(ItemStack.parseOptional(h.getLevel().registryAccess(),tag.getCompound("displaySeed")),seed),"Chunk and update packets preserve the species without sending production inventory");
        machine.items.setStackInSlot(0,ItemStack.EMPTY); h.assertTrue(machine.getUpdateTag(h.getLevel().registryAccess()).isEmpty(),"Removing the seed clears the display packet"); h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void declaredCropOverridesReloadAndComponents(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager(); var originals=new ArrayList<>(manager.getRecipes());
        var result=new ItemStack(Items.DIAMOND,3); result.set(DataComponents.CUSTOM_NAME,Component.literal("Crop output"));
        var recipe=new PlantingRecipe(Ingredient.of(Items.APPLE),Ingredient.of(Blocks.DIRT),List.of(result),Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE,7),false,40,false);
        var recipes=new ArrayList<RecipeHolder<?>>(originals); recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(Agristics.ID,"test/declared"),recipe));
        var pos=new BlockPos(1,1,1); h.setBlock(pos,Agristics.EARLY.get()); EarlyPlanter machine=h.getBlockEntity(pos);
        try {
            manager.replaceRecipes(recipes); CropSources.invalidate();
            var seed=new ItemStack(Items.APPLE);
            h.assertTrue(Harvests.seed(seed,h.getLevel()) && !Harvests.seed(seed),"Declared non-block seed is recognized by the shared system");
            machine.items.insertItem(0,seed,false); machine.items.insertItem(1,new ItemStack(Blocks.DIRT),false); machine.step();
            h.assertTrue(machine.items.getStackInSlot(2).isEmpty(),"Explicit forty-tick crop does not finish early"); machine.step();
            h.assertTrue(machine.items.getStackInSlot(2).getCount()==3 && ItemStack.isSameItemSameComponents(machine.items.getStackInSlot(2),result),"Explicit output components survive basic harvest");
            var batch=Harvests.networkBatch(h.getLevel(),machine.getBlockPos(),seed,100000,new HashMap<>());
            h.assertTrue(batch.get(AEItemKey.of(result))==300000,"Declared fixed outputs batch without sampling");
            h.assertTrue(ItemStack.isSameItemSameComponents(Cultivations.seed(seed,h.getLevel(),machine.getBlockPos()).essenceStack(),result),"Stock target identity retains output components");
            var codec=PlantingRecipe.CODEC.codec(); var ops=h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var decoded=codec.parse(ops,codec.encodeStart(ops,recipe).getOrThrow()).getOrThrow();
            h.assertTrue(decoded.ticks()==40 && ItemStack.isSameItemSameComponents(decoded.outputs().getFirst(),result),"Pack recipe codec retains timing and components");
        } finally { manager.replaceRecipes(originals); CropSources.invalidate(); }
        h.assertTrue(!Harvests.seed(new ItemStack(Items.APPLE),h.getLevel()),"Removing a recipe invalidates cached recognition"); h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void optionalBotanyUsesActualHarvest(GameTestHelper h) {
        if (!net.neoforged.fml.ModList.get().isLoaded("botanypots")) { h.succeed(); return; }
        var seed=new ItemStack(Items.CACTUS); var source=CropSources.find(h.getLevel(),seed);
        h.assertTrue(source!=null && source.acceptsSoil(new ItemStack(Blocks.SAND)),"Botany Pots contributes cactus and real soil requirements");
        var pos=h.absolutePos(new BlockPos(2,1,1));
        h.assertTrue(source.canGrow(h.getLevel(),pos,new ItemStack(Blocks.SAND)),"Native Botany callback context can grow");
        h.assertTrue(!source.canGrow(h.getLevel(),pos,new ItemStack(Blocks.STONE)),"Wrong soil is rejected");
        var drops=Harvests.networkBatch(h.getLevel(),pos,seed,64,new HashMap<>());
        h.assertTrue(drops.getOrDefault(AEItemKey.of(new ItemStack(Items.CACTUS)),0L)>0,"Actual callback yields the supported cactus harvest");
        h.assertTrue(Harvests.maximumBatch(h.getLevel(),seed)==64,"Opaque callbacks are bounded and are never sampled then multiplied");
        h.assertTrue(CropSources.find(h.getLevel(),new ItemStack(Crops.all().getFirst().getSeedsItem()))==null,"MA native provider precedes broad Botany packs");
        org.slf4j.LoggerFactory.getLogger("AppliedAgristicsTests").info("BOTANY_POTS_COMPAT_PASSED cactus=true actualHarvest=true nativePriority=true"); h.succeed();
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void componentSeedVariantsKeepSeparateTasks(GameTestHelper h) {
        var fixture=PlanterFixture.install(h.getLevel(),h.absolutePos(new BlockPos(2,1,1)),true);
        var first=new ItemStack(Items.WHEAT_SEEDS); first.set(DataComponents.CUSTOM_NAME,Component.literal("Variety A"));
        var second=new ItemStack(Items.WHEAT_SEEDS); second.set(DataComponents.CUSTOM_NAME,Component.literal("Variety B"));
        fixture.seeds().add(AEItemKey.of(first),1); fixture.seeds().add(AEItemKey.of(second),1);
        var a=Cultivations.id(first,h.getLevel()); var b=Cultivations.id(second,h.getLevel());
        h.assertTrue(!a.equals(b),"Different seed components get different cultivation identities");
        CropSources.invalidate(); h.assertTrue(a.equals(Cultivations.id(ItemStack.parseOptional(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)first.save(h.getLevel().registryAccess())),h.getLevel())),"Identity remains stable across serialization and cache reload");
        h.runAtTickTime(100,()->{
            var controller=fixture.controller(); controller.configureAll(true); controller.step();
            h.assertTrue(controller.tasks.get(a).occupied==1 && controller.tasks.get(b).occupied==1,"Both component variants reserve their own seed");
            h.assertTrue(controller.snapshot(0,"").getList("rows",10).size()==2,"Owned list keeps the variants separate");
            h.succeed();
        });
    }
}
