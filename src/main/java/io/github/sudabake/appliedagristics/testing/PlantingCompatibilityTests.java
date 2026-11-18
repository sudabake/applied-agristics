package io.github.sudabake.appliedagristics.testing;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.logic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class PlantingCompatibilityTests {
    @GameTest(template="empty", templateNamespace=Agristics.ID)
    public static void nativeAndModdedSaplingsUseTheirActualTreeDefinitions(GameTestHelper h) {
        var names=new LinkedHashSet<>(List.of("minecraft:oak_sapling","occultism:otherworld_sapling",
                "ars_nouveau:green_archwood_sapling","ars_nouveau:blue_archwood_sapling",
                "ars_nouveau:red_archwood_sapling","ars_nouveau:purple_archwood_sapling"));
        for(var item:BuiltInRegistries.ITEM)
            if(item instanceof BlockItem block && block.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock)
                names.add(BuiltInRegistries.ITEM.getKey(item).toString());
        var tested=new ArrayList<String>();
        var pos=h.absolutePos(new BlockPos(1,1,1));
        var before=h.getLevel().getBlockState(pos);
        for(var name:names) {
            var item=BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(name)).orElse(null);
            if(item==null) continue;
            var seed=new ItemStack(item);
            var source=CropSources.find(h.getLevel(),seed);
            h.assertTrue(source!=null && source.ticks()==2400 && source.maximumBatch()==64,
                    "Native tree definitions resolve without requiring another growing-machine mod: "+name);
            if(name.equals("occultism:otherworld_sapling_natural"))
                h.assertTrue(source.product().is(Items.OAK_LOG),"Uncovered-state loot rules replace the disguised natural trunk item");
            h.assertTrue(Harvests.seed(seed,h.getLevel()) && source.canGrow(h.getLevel(),pos,Harvests.networkSoil()),
                    "The seed bank and basic planter accept the same identified tree: "+name);
            long logs=0;
            for(int sample=0;sample<64;sample++)
                for(var drop:source.harvestNetwork(h.getLevel(),pos))
                    if(drop.is(source.product().getItem())) logs+=drop.getCount();
            h.assertTrue(logs>=64 && logs<=256 && h.getLevel().getBlockState(pos)==before,
                    "Tree harvests provide one to four actual logs without placing blocks: "+name);
            tested.add(name+" -> "+BuiltInRegistries.ITEM.getKey(source.product().getItem()));
        }
        h.assertTrue(NativeTrees.find(h.getLevel(),new ItemStack(Items.OAK_SAPLING))!=null,
                "The generic tree reader handles the vanilla TreeGrower format too");
        com.mojang.logging.LogUtils.getLogger().info("TREE_COMPATIBILITY_PASSED {}",tested);
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace=Agristics.ID)
    public static void allLoadedExternalPlantingInputsReachSharedCatalogAndSeedRecognition(GameTestHelper h) {
        var seeds=ImportedPlanting.seeds(h.getLevel());
        var allInputs=new ArrayList<>(seeds);
        var ops=h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        for(var holder:h.getLevel().getRecipeManager().getRecipes()) {
            var type=BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer()).toString();
            if(!type.equals("immersiveengineering:cloche") && !type.equals("mekmm:planting")) continue;
            var json=net.minecraft.world.item.crafting.Recipe.CODEC.encodeStart(ops,holder.value()).getOrThrow().getAsJsonObject();
            var input=net.minecraft.world.item.crafting.Ingredient.CODEC_NONEMPTY.parse(ops,
                    json.get(type.equals("mekmm:planting")?"item_input":"input")).getOrThrow();
            allInputs.addAll(Arrays.asList(input.getItems()));
        }
        var catalog=PlantingCatalog.serialize(h.getLevel());
        var listed=new HashSet<AEItemKey>();
        for(var raw:catalog) listed.add(AEItemKey.of(ItemStack.parseOptional(h.getLevel().registryAccess(),
                ((net.minecraft.nbt.CompoundTag)raw).getCompound("seed"))));
        for(var seed:allInputs) {
            h.assertTrue(Harvests.seed(seed,h.getLevel()),"Every loaded external recipe input is accepted: "+seed);
            h.assertTrue(listed.contains(AEItemKey.of(seed)),"JEI lists every recognized external recipe input: "+seed);
        }
        if(net.neoforged.fml.ModList.get().isLoaded("immersiveengineering")) {
            var hemp=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("immersiveengineering:seed")));
            var imported=ImportedPlanting.find(h.getLevel(),hemp);
            h.assertTrue(imported!=null && imported.ticks()==800
                    && BuiltInRegistries.ITEM.getKey(imported.product().getItem()).toString().equals("immersiveengineering:hemp_fiber"),
                    "The live cloche recipe retains its actual input, output and processing time");
        }
        if(net.neoforged.fml.ModList.get().isLoaded("mekmm")) {
            var imported=ImportedPlanting.find(h.getLevel(),new ItemStack(Items.OAK_SAPLING));
            h.assertTrue(imported!=null && imported.product().is(Items.OAK_LOG)
                    && CropSources.find(h.getLevel(),new ItemStack(Items.OAK_SAPLING)).ticks()==2400,
                    "MekMM recipes are read while built-in tree balance keeps priority");
        }
        com.mojang.logging.LogUtils.getLogger().info("EXTERNAL_PLANTING_COMPATIBILITY_PASSED recipeInputs={} distinctSeeds={} catalog={}",
                seeds.size(),seeds.stream().map(AEItemKey::of).distinct().count(),catalog.size());
        h.succeed();
    }
}
