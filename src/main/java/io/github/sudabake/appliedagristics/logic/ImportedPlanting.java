package io.github.sudabake.appliedagristics.logic;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.sudabake.appliedagristics.Crops;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Uses the public recipe codec to read optional machine recipes after pack overrides and conditions. */
public final class ImportedPlanting {
    private static final Map<RecipeManager,List<Definition>> CACHE=new WeakHashMap<>();
    private record Drop(ItemStack stack,double chance) {}
    private record Definition(Ingredient input,Ingredient soil,List<Drop> drops,BlockState display,int ticks,boolean debitSeed) {}

    private ImportedPlanting() {}
    public static synchronized void invalidate() { CACHE.clear(); }
    public static List<ItemStack> seeds(Level level) {
        return definitions(level).stream().flatMap(recipe->Arrays.stream(recipe.input.getItems()))
                .map(ItemStack::copy).toList();
    }
    public static CropSource find(Level level,ItemStack seed) {
        return definitions(level).stream().filter(recipe->recipe.input.test(seed)).findFirst()
                .map(recipe->(CropSource)new Source(recipe,seed.copyWithCount(1))).orElse(null);
    }
    private static synchronized List<Definition> definitions(Level level) {
        return CACHE.computeIfAbsent(level.getRecipeManager(),ignored->{
            var result=new ArrayList<Definition>();
            var ops=level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            for(var holder:level.getRecipeManager().getRecipes().stream().sorted(Comparator.comparing(recipe->recipe.id().toString())).toList()) {
                var type=BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer()).toString();
                if (!type.equals("immersiveengineering:cloche") && !type.equals("mekmm:planting")) continue;
                try {
                    var json=Recipe.CODEC.encodeStart(ops,holder.value()).getOrThrow().getAsJsonObject();
                    boolean mek=type.equals("mekmm:planting");
                    var input=Ingredient.CODEC_NONEMPTY.parse(ops,json.get(mek?"item_input":"input")).getOrThrow();
                    var soil=json.has("soil")?Ingredient.CODEC_NONEMPTY.parse(ops,json.get("soil")).getOrThrow():Ingredient.EMPTY;
                    var drops=new ArrayList<Drop>();
                    if(mek) {
                        drops.add(new Drop(ItemStack.CODEC.parse(ops,json.get("main_output")).getOrThrow(),1));
                        if(json.has("secondary_output")) drops.add(new Drop(
                                ItemStack.CODEC.parse(ops,json.get("secondary_output")).getOrThrow(),json.get("secondary_chance").getAsDouble()));
                    } else {
                        for(var entry:json.getAsJsonArray("results")) {
                            var object=entry.getAsJsonObject();
                            var output=object.has("output")?object.get("output"):object;
                            drops.add(new Drop(ItemStack.CODEC.parse(ops,output).getOrThrow(),object.has("chance")?object.get("chance").getAsDouble():1));
                        }
                    }
                    if(drops.isEmpty() || drops.stream().anyMatch(drop->drop.stack.isEmpty()
                            || !Double.isFinite(drop.chance) || drop.chance<0 || drop.chance>1)) continue;
                    drops.removeIf(drop->drop.chance==0);
                    if(drops.isEmpty()) continue;
                    var display=Blocks.WHEAT.defaultBlockState();
                    if(json.has("render")) {
                        var render=json.getAsJsonObject("render");
                        if(render.has("block")) display=BuiltInRegistries.BLOCK.get(ResourceLocation.parse(render.get("block").getAsString())).defaultBlockState();
                    } else if(input.getItems().length>0 && input.getItems()[0].getItem() instanceof BlockItem block) display=block.getBlock().defaultBlockState();
                    int ticks=json.has("time")?json.get("time").getAsInt():200;
                    result.add(new Definition(input,soil,List.copyOf(drops),display,Math.max(20,ticks),mek));
                } catch(RuntimeException exception) {
                    com.mojang.logging.LogUtils.getLogger().warn("Cannot import planting recipe {} ({})",holder.id(),exception.getMessage());
                }
            }
            return List.copyOf(result);
        });
    }
    private record Source(Definition recipe,ItemStack seed) implements CropSource {
        @Override public ItemStack product() { return recipe.drops.getFirst().stack.copy(); }
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
            int returned=recipe.drops.stream().filter(drop->drop.chance>0 && ItemStack.isSameItemSameComponents(drop.stack,seed))
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
