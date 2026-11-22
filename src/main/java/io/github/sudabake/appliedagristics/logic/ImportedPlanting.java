package io.github.sudabake.appliedagristics.logic;

import io.github.sudabake.appliedagristics.Crops;
import java.util.*;
import java.util.function.Predicate;
import net.minecraftforge.fml.ModList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Reads loaded optional machine recipes after pack overrides and conditions. */
public final class ImportedPlanting {
    private static final Map<RecipeManager,List<Definition>> CACHE=new WeakHashMap<>();
    private record Drop(ItemStack stack,double chance) {}
    private record Definition(Predicate<ItemStack> input, List<ItemStack> seeds, Ingredient soil,
                              List<Drop> drops, BlockState display, int ticks, boolean debitSeed) {}

    private ImportedPlanting() {}
    public static synchronized void invalidate() { CACHE.clear(); }
    public static List<ItemStack> seeds(Level level) {
        return definitions(level).stream().flatMap(recipe->recipe.seeds.stream())
                .map(ItemStack::copy).toList();
    }
    public static CropSource find(Level level,ItemStack seed) {
        return definitions(level).stream().filter(recipe->recipe.input.test(seed)).findFirst()
                .map(recipe->(CropSource)new Source(recipe,seed.copyWithCount(1))).orElse(null);
    }
    private static synchronized List<Definition> definitions(Level level) {
        return CACHE.computeIfAbsent(level.getRecipeManager(),ignored->{
            var result = new ArrayList<Definition>();
            boolean cloche = ModList.get().isLoaded("immersiveengineering");
            boolean mek = ModList.get().isLoaded("mekmm");
            for (var recipe : level.getRecipeManager().getRecipes().stream()
                    .sorted(Comparator.comparing(Recipe::getId)).toList()) {
                var serializer = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
                if (serializer == null) continue;
                Definition entry = switch (serializer.toString()) {
                    case "immersiveengineering:cloche" -> cloche ? Cloche.read(recipe) : null;
                    case "mekmm:planting" -> mek ? Mek.read(recipe) : null;
                    default -> null;
                };
                if (entry != null && !entry.seeds.isEmpty() && !entry.drops.isEmpty()) result.add(entry);
            }
            return List.copyOf(result);
        });
    }

    private static final class Cloche {
        static Definition read(Recipe<?> recipe) {
            if (!(recipe instanceof blusunrize.immersiveengineering.api.crafting.ClocheRecipe cloche)) return null;
            var drops = cloche.outputs.stream().map(output -> output.get()).filter(stack -> !stack.isEmpty())
                    .map(stack -> new Drop(stack.copy(), 1)).toList();
            return new Definition(cloche.seed::test, List.of(cloche.seed.getItems()), cloche.soil, drops,
                    cloche.renderReference.getBlock().defaultBlockState(), cloche.time, false);
        }
    }

    private static final class Mek {
        static Definition read(Recipe<?> recipe) {
            if (!(recipe instanceof com.jerry.mekmm.api.recipes.PlantingRecipe planter)) return null;
            var input = planter.getItemInput();
            var seeds = input.getRepresentations();
            var drops = new ArrayList<Drop>();
            if (!planter.getMainOutput().isEmpty()) drops.add(new Drop(planter.getMainOutput().copy(), 1));
            var extra = planter.getSecondaryOutput();
            double chance = planter.getSecondaryChance();
            if (!extra.isEmpty() && chance > 0 && chance <= 1) drops.add(new Drop(extra.copy(), chance));
            var display = seeds.isEmpty() || !(seeds.get(0).getItem() instanceof BlockItem block)
                    ? Blocks.WHEAT.defaultBlockState() : block.getBlock().defaultBlockState();
            return new Definition(input::testType, List.copyOf(seeds), Ingredient.EMPTY, List.copyOf(drops),
                    display, 200, true);
        }
    }

    private record Source(Definition recipe,ItemStack seed) implements CropSource {
        @Override public ItemStack product() { return recipe.drops.get(0).stack.copy(); }
        @Override public List<ItemStack> previewOutputs(ServerLevel level) { return recipe.drops.stream().filter(drop->drop.chance>0).map(drop->drop.stack.copyWithCount(1)).toList(); }
        @Override public ItemStack networkSoil() {
            var choices=recipe.soil.getItems();
            return choices.length==0?Harvests.networkSoil():choices[0].copyWithCount(1);
        }
        @Override public BlockState display() { return recipe.display; }
        @Override public int ticks() { return recipe.ticks; }
        @Override public boolean acceptsSoil(ItemStack soil) { return recipe.soil.isEmpty()?Crops.farmland(soil):recipe.soil.test(soil); }
        @Override public boolean canGrow(ServerLevel level,BlockPos pos,ItemStack soil) { return acceptsSoil(soil); }
        @Override public boolean reproduces() {
            int returned=recipe.drops.stream().filter(drop->drop.chance>0 && ItemStack.isSameItemSameTags(drop.stack,seed))
                    .mapToInt(drop->drop.stack.getCount()).sum();
            return returned>(recipe.debitSeed?1:0);
        }
        @Override public boolean fixedOutputs() { return recipe.drops.stream().allMatch(drop->drop.chance==1); }
        @Override public int maximumBatch() { return fixedOutputs()?Integer.MAX_VALUE:64; }
        @Override public List<ItemStack> harvest(ServerLevel level,BlockPos pos,ItemStack soil) {
            var result=new ArrayList<ItemStack>();
            for(var drop:recipe.drops)
                if(drop.chance==1 || drop.chance>0 && level.random.nextDouble()<drop.chance) result.add(drop.stack.copy());
            // MekMM spends one input each operation. Reserve its returned seed as net harvest does.
            if(recipe.debitSeed) Harvests.reserveSeed(result,seed);
            return result;
        }
    }
}
