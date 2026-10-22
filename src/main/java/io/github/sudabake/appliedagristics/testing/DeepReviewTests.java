package io.github.sudabake.appliedagristics.testing;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.*;
import appeng.core.definitions.*;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.block.*;
import io.github.sudabake.appliedagristics.logic.Harvests;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@PrefixGameTestTemplate(false)
public class DeepReviewTests {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("DeepReview");
    @GameTest(template="empty",templateNamespace=Agristics.ID)
    public static void namedSeedReplantAccounting(GameTestHelper h) {
        var seed=new ItemStack(Items.WHEAT_SEEDS);seed.set(DataComponents.CUSTOM_NAME,Component.literal("Named variety"));
        var ordinary=new ArrayList<>(List.of(new ItemStack(Items.WHEAT_SEEDS,5)));
        var renamed=new ArrayList<>(List.of(new ItemStack(Items.WHEAT_SEEDS,5)));
        Harvests.reserveSeed(ordinary,new ItemStack(Items.WHEAT_SEEDS));Harvests.reserveSeed(renamed,seed);
        LOG.info("NAMED_REPLANT ordinary={} renamed={}",ordinary.getFirst().getCount(),renamed.getFirst().getCount());
        h.assertTrue(ordinary.getFirst().getCount()==4 && renamed.getFirst().getCount()==4,"Renaming a seed must not remove the replant debit");h.succeed();
    }
    private static MEInfusionAltar altar(GameTestHelper h) {
        var pos=new BlockPos(4,1,4);h.setBlock(pos,Agristics.ME_INFUSION.get());h.setBlock(pos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());h.setBlock(pos.west(2),AEBlocks.DRIVE.block());
        appeng.blockentity.storage.DriveBlockEntity drive=h.getBlockEntity(pos.west(2));drive.getInternalInventory().setItemDirect(0,AEItems.ITEM_CELL_1K.stack());
        MEInfusionAltar a=h.getBlockEntity(pos);
        for(var p:a.pedestalPositions())h.getLevel().setBlockAndUpdate(p,com.blakebr0.mysticalagriculture.init.ModBlocks.INFUSION_PEDESTAL.get().defaultBlockState());
        for(int i=2;i<6;i++)a.items.setStackInSlot(i,AEItems.SPEED_CARD.stack());return a;
    }
    private static appeng.api.crafting.IPatternDetails pattern(GameTestHelper h,ItemStack output,Map<AEItemKey,Long> inputs) {
        var encoded=PatternDetailsHelper.encodeProcessingPattern(inputs.entrySet().stream().map(e->new GenericStack(e.getKey(),e.getValue())).toList(),List.of(new GenericStack(AEItemKey.of(output),output.getCount())));
        return PatternDetailsHelper.decodePattern(encoded,h.getLevel());
    }
    private static KeyCounter[] counters(appeng.api.crafting.IPatternDetails pattern) {
        var result=new KeyCounter[pattern.getInputs().length];
        for(int i=0;i<result.length;i++){result[i]=new KeyCounter();var input=pattern.getInputs()[i];var stack=input.getPossibleInputs()[0];result[i].add(stack.what(),stack.amount()*input.getMultiplier());}return result;
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void shortInfusionRecipes(GameTestHelper h) {
        var a=altar(h);
        h.runAtTickTime(100,()->{
            var manager=h.getLevel().getRecipeManager();var original=new ArrayList<>(manager.getRecipes());
            var recipe=new com.blakebr0.mysticalagriculture.crafting.recipe.InfusionRecipe(Ingredient.of(Items.EMERALD),NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.COBBLESTONE)),new ItemStack(Items.DIAMOND),false);
            var changed=new ArrayList<RecipeHolder<?>>(original);changed.add(new RecipeHolder<>(ResourceLocation.parse("appliedagristics:review/short_infusion"),recipe));
            try {
                manager.replaceRecipes(changed);
                a.items.setStackInSlot(0,new ItemStack(Items.EMERALD));
                var pedestal=(com.blakebr0.mysticalagriculture.tileentity.InfusionPedestalTileEntity)h.getLevel().getBlockEntity(a.pedestalPositions().getFirst());pedestal.getInventory().setStackInSlot(0,new ItemStack(Items.COBBLESTONE));
                for(int i=0;i<8;i++)a.step();
                boolean manual=a.items.getStackInSlot(1).is(Items.DIAMOND);String status=a.status;
                a.items.setStackInSlot(0,ItemStack.EMPTY);a.items.setStackInSlot(1,ItemStack.EMPTY);pedestal.getInventory().setStackInSlot(0,ItemStack.EMPTY);
                var p=pattern(h,new ItemStack(Items.DIAMOND),Map.of(AEItemKey.of(new ItemStack(Items.EMERALD)),1L,AEItemKey.of(new ItemStack(Items.COBBLESTONE)),1L));
                boolean ae=a.pushPattern(p,counters(p),Direction.NORTH);
                LOG.info("SHORT_INFUSION manual={} status={} ae={} ingredientCount={}",manual,status,ae,recipe.getIngredients().size());
                h.assertTrue(manual && ae,"Short recipes work by hand and through AE processing patterns");
            } finally {manager.replaceRecipes(original);}
            h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void externalExtractionLeavesDeliveryFlag(GameTestHelper h) {
        var a=altar(h);
        h.runAtTickTime(100,()->{
            var r=h.getLevel().getRecipeManager().getAllRecipesFor(com.blakebr0.mysticalagriculture.init.ModRecipeTypes.INFUSION.get()).stream().map(x->x.value()).filter(x->x.getIngredients().size()==8 && !x.getAltarIngredient().isEmpty()).findFirst().orElseThrow();
            var inputs=new LinkedHashMap<AEItemKey,Long>();inputs.merge(AEItemKey.of(r.getAltarIngredient().getItems()[0]),1L,Long::sum);
            for(var i:r.getIngredients())inputs.merge(AEItemKey.of(i.getItems()[0]),1L,Long::sum);
            var p=pattern(h,r.getResultItem(h.getLevel().registryAccess()),inputs);h.assertTrue(a.pushPattern(p,counters(p),Direction.NORTH),"AE push works");
            for(int i=0;i<7;i++)a.step();
            var extracted=a.automation(Direction.DOWN).extractItem(1,64,false);h.assertTrue(!extracted.isEmpty(),"External hopper retrieves the AE order result before export tick");a.step();
            boolean sticky=a.saveWithFullMetadata(h.getLevel().registryAccess()).getBoolean("craftingDelivery");
            a.items.setStackInSlot(0,r.getAltarIngredient().getItems()[0].copyWithCount(1));
            for(int i=0;i<8;i++)((com.blakebr0.mysticalagriculture.tileentity.InfusionPedestalTileEntity)h.getLevel().getBlockEntity(a.pedestalPositions().get(i))).getInventory().setStackInSlot(0,r.getIngredients().get(i).getItems()[0].copyWithCount(1));
            for(int i=0;i<7;i++)a.step();h.assertTrue(!a.items.getStackInSlot(1).isEmpty(),"Manual craft produces result");a.step();
            boolean exported=a.items.getStackInSlot(1).isEmpty();
            LOG.info("STICKY_DELIVERY flagAfterExtraction={} manualResultAutoExported={} autoOutput={}",sticky,exported,a.snapshot(0,"").getBoolean("autoOutput"));
            h.assertTrue(!sticky && !exported && !a.snapshot(0,"").getBoolean("autoOutput"),"External extraction clears delivery state; later manual results respect disabled auto-output");h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void transferredOutputComponents(GameTestHelper h) {
        var a=altar(h);
        h.runAtTickTime(100,()->{
            var manager=h.getLevel().getRecipeManager();var original=new ArrayList<>(manager.getRecipes());
            var parts=NonNullList.withSize(8,Ingredient.of(Items.COBBLESTONE));
            var recipe=new com.blakebr0.mysticalagriculture.crafting.recipe.InfusionRecipe(Ingredient.of(Items.EMERALD),parts,new ItemStack(Items.DIAMOND),true);
            var changed=new ArrayList<RecipeHolder<?>>(original);changed.add(new RecipeHolder<>(ResourceLocation.parse("appliedagristics:review/transfer"),recipe));
            try {
                manager.replaceRecipes(changed);var center=new ItemStack(Items.EMERALD);center.set(DataComponents.CUSTOM_NAME,Component.literal("Inherited name"));
                var all=new ArrayList<ItemStack>();all.add(center);for(int i=0;i<8;i++)all.add(new ItemStack(Items.COBBLESTONE));
                var output=recipe.assemble(CraftingInput.of(3,3,all),h.getLevel().registryAccess());
                var p=pattern(h,output,Map.of(AEItemKey.of(center),1L,AEItemKey.of(new ItemStack(Items.COBBLESTONE)),8L));
                boolean accepted=a.pushPattern(p,counters(p),Direction.NORTH);
                LOG.info("TRANSFERRED_OUTPUT accepted={} actualHasName={} staticHasName={}",accepted,output.has(DataComponents.CUSTOM_NAME),recipe.getResultItem(h.getLevel().registryAccess()).has(DataComponents.CUSTOM_NAME));
                h.assertTrue(accepted && output.has(DataComponents.CUSTOM_NAME),"AE accepts actual output components transferred from center");
            } finally {manager.replaceRecipes(original);}h.succeed();
        });
    }
    @GameTest(template="empty",templateNamespace=Agristics.ID,timeoutTicks=240)
    public static void shortInfusionReturnsContainersToPhysicalSlots(GameTestHelper h) {
        var a=altar(h);
        h.runAtTickTime(100,()->{
            var manager=h.getLevel().getRecipeManager();var original=new ArrayList<>(manager.getRecipes());
            var recipe=new com.blakebr0.mysticalagriculture.crafting.recipe.InfusionRecipe(Ingredient.of(Items.MILK_BUCKET),NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.MILK_BUCKET)),new ItemStack(Items.DIAMOND),false);
            var changed=new ArrayList<RecipeHolder<?>>(original);changed.add(new RecipeHolder<>(ResourceLocation.parse("appliedagristics:review/containers"),recipe));
            try {
                manager.replaceRecipes(changed);a.items.setStackInSlot(0,new ItemStack(Items.MILK_BUCKET));
                var pedestal=(com.blakebr0.mysticalagriculture.tileentity.InfusionPedestalTileEntity)h.getLevel().getBlockEntity(a.pedestalPositions().getFirst());
                pedestal.getInventory().setStackInSlot(0,new ItemStack(Items.MILK_BUCKET));
                for(int i=0;i<7;i++)a.step();
                h.assertTrue(a.items.getStackInSlot(0).is(Items.BUCKET) && a.items.getStackInSlot(1).is(Items.DIAMOND) && pedestal.getInventory().getStackInSlot(0).is(Items.BUCKET),"Short recipe returns center and pedestal buckets to their original physical slots");
            } finally {manager.replaceRecipes(original);}h.succeed();
        });
    }
}

