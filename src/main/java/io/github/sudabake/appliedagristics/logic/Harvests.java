package io.github.sudabake.appliedagristics.logic;

import io.github.sudabake.appliedagristics.Crops;
import com.blakebr0.mysticalagriculture.config.ModConfigs;
import com.blakebr0.mysticalagriculture.lib.ModCrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import java.util.*;

/** Scoped read-only plant/soil view: no blocks, neighbors, scheduled ticks or world data are changed. */
public final class Harvests {
    private record View(Level level, Map<BlockPos, BlockState> states) {}
    private static final ThreadLocal<View> VIEW = new ThreadLocal<>();
    public static BlockState virtualState(Level level, BlockPos pos) {
        var view = VIEW.get(); return view != null && view.level == level ? view.states.get(pos) : null;
    }
    public static BlockState mature(ItemStack seed) {
        if (!(seed.getItem() instanceof BlockItem item)) return null;
        var block = item.getBlock();
        // Farmer's Delight seedlings transform into a different block before yielding fruit.
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        if (id.toString().equals("farmersdelight:budding_tomatoes"))
            block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("farmersdelight:tomatoes"));
        var state = block.defaultBlockState();
        if (state.getBlock() instanceof CropBlock crop) return crop.getStateForAge(crop.getMaxAge());
        for (var property : state.getProperties()) if (property instanceof IntegerProperty age && age.getName().equals("age"))
            return state.setValue(age, Collections.max(age.getPossibleValues()));
        return null;
    }
    public static boolean seed(ItemStack seed) { return io.github.sudabake.appliedagristics.biology.BioData.seed(seed) || mature(seed) != null; }
    public static boolean seed(ItemStack seed,Level level) { return seed(seed) || CropSources.find(level,seed)!=null; }
    public static <T> T inField(ServerLevel level, BlockPos machine, BlockState plant, ItemStack soil, java.util.function.Supplier<T> operation) {
        if (!(soil.getItem() instanceof BlockItem ground)) throw new IllegalArgumentException("Missing soil");
        var old = VIEW.get();
        var groundState = ground.getBlock().defaultBlockState();
        if (groundState.hasProperty(FarmBlock.MOISTURE)) groundState = groundState.setValue(FarmBlock.MOISTURE, 7);
        var states = new HashMap<BlockPos, BlockState>(); states.put(machine, groundState); states.put(machine.above(), plant);
        if (plant.getBlock() instanceof com.blakebr0.mysticalagriculture.api.crop.ICropProvider provider && provider.getCrop().getCruxBlock() != null)
            states.put(machine.below(), provider.getCrop().getCruxBlock().defaultBlockState());
        VIEW.set(new View(level, states));
        try { return operation.get(); } finally { if (old == null) VIEW.remove(); else VIEW.set(old); }
    }
    public static boolean canGrow(ServerLevel level, BlockPos machine, ItemStack seed, ItemStack soil) {
        return canGrow(level, machine, seed, soil, ItemStack.EMPTY);
    }
    public static boolean canGrow(ServerLevel level, BlockPos machine, ItemStack seed, ItemStack soil, ItemStack crux) {
        var source=CropSources.find(level,seed); if (source!=null) return source.canGrow(level,machine,soil);
        if (io.github.sudabake.appliedagristics.biology.BioData.seed(seed)) return Crops.farmland(soil);
        var mature = mature(seed); if (mature == null || !(soil.getItem() instanceof BlockItem ground)) return false;
        var crop = Crops.seed(seed);
        if (crop != null) {
            if (crop.getCruxBlock() != null && level.getBlockState(machine.below()).getBlock() != crop.getCruxBlock()
                && (!(crux.getItem() instanceof BlockItem item) || item.getBlock() != crop.getCruxBlock())) return false;
            if (ModConfigs.REQUIRES_EFFECTIVE_FARMLAND.get() && crop != ModCrops.INFERIUM && !crop.getTier().isEffectiveFarmland(ground.getBlock())) return false;
            if (!crop.getRequiredBiomes().isEmpty()) {
                var biome = level.getBiome(machine).getKey(); if (biome == null || !crop.getRequiredBiomes().contains(biome.location())) return false;
            }
        }
        return inField(level, machine, mature, soil, () -> mature.canSurvive(level, machine.above()));
    }
    public static boolean crux(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && io.github.sudabake.appliedagristics.Crops.all().stream().anyMatch(c -> c.getCruxBlock() == item.getBlock());
    }
    public static ItemStack networkSoil() {
        return new ItemStack(com.blakebr0.mysticalagriculture.block.InfusedFarmlandBlock.FARMLANDS.stream()
            .max(Comparator.comparingInt(f -> f.getTier().getValue())).orElseThrow());
    }
    public static ItemStack networkSoil(Level level,ItemStack seed) { var source=CropSources.find(level,seed); return source==null?networkSoil():source.networkSoil(); }
    public static boolean canGrowNetwork(ServerLevel level,BlockPos pos,ItemStack seed) {
        var source=CropSources.find(level,seed); if (source!=null) return source.canGrow(level,pos,source.networkSoil());
        var crop=Crops.seed(seed); return canGrow(level,pos,seed,networkSoil(),crop!=null && crop.getCruxBlock()!=null ? new ItemStack(crop.getCruxBlock()) : ItemStack.EMPTY);
    }
    public static long maximumBatch(Level level,ItemStack seed) { var source=CropSources.find(level,seed); return source==null ? Integer.MAX_VALUE : source.maximumBatch(); }
    /** Standard MA crops use exact expected yield with saved fractional remainders.
     * Unknown native loot implementations use at most 64 rolls per batch, scaled by cycles. */
    public static Map<appeng.api.stacks.AEItemKey, Long> networkBatch(ServerLevel level, BlockPos pos, ItemStack seed, long cycles,
            Map<appeng.api.stacks.AEItemKey, Double> fractions) {
        if (cycles<=0) return Map.of();
        var source=CropSources.find(level,seed);
        if (source!=null) {
            var result=new LinkedHashMap<appeng.api.stacks.AEItemKey,Long>();
            if (source.fixedOutputs()) {
                for (var drop:source.harvest(level,pos,source.networkSoil())) result.merge(appeng.api.stacks.AEItemKey.of(drop),Math.multiplyExact(cycles,drop.getCount()),Math::addExact);
            } else {
                // Actual callbacks are settled exactly, in bounded batches. Caller keeps unprocessed work.
                if (cycles>source.maximumBatch()) throw new IllegalArgumentException("Provider batch exceeds exact callback budget");
                for (long i=0;i<cycles;i++) for (var drop:source.harvest(level,pos,source.networkSoil())) result.merge(appeng.api.stacks.AEItemKey.of(drop),(long)drop.getCount(),Math::addExact);
            }
            addFallback(level,seed,source.networkSoil(),cycles,result); return result;
        }
        if (io.github.sudabake.appliedagristics.biology.BioData.seed(seed)) {
            var result=new LinkedHashMap<appeng.api.stacks.AEItemKey,Long>();
            result.put(appeng.api.stacks.AEItemKey.of(io.github.sudabake.appliedagristics.biology.BioData.withIdentity(io.github.sudabake.appliedagristics.Agristics.BIO_ESSENCE.get(),io.github.sudabake.appliedagristics.biology.BioData.identity(seed))),cycles);
            addFallback(level,seed,networkSoil(),cycles,result); return result;
        }
        var expected = new LinkedHashMap<appeng.api.stacks.AEItemKey, Double>(); var crop = Crops.seed(seed); var soil = networkSoil(); var plant = mature(seed);
        if (crop != null && (plant.getBlock().getClass() == com.blakebr0.mysticalagriculture.block.MysticalCropBlock.class
                || plant.getBlock().getClass() == com.blakebr0.mysticalagriculture.block.InferiumCropBlock.class)) {
            var ground = ((BlockItem)soil.getItem()).getBlock();
            double chance = Math.clamp(crop.getSecondaryChance(ground), 0, 1);
            double yield = 1 + chance;
            if (crop == ModCrops.INFERIUM) {
                int tier = ((com.blakebr0.mysticalagriculture.api.farmland.IEssenceFarmland)ground).getTier().getValue();
                yield = (int)(.5 * tier + .5) + (tier > 1 && tier % 2 == 0 ? .5 : 0);
            }
            expected.put(appeng.api.stacks.AEItemKey.of(new ItemStack(crop.getEssenceItem())), yield * cycles);
            if (ModConfigs.SECONDARY_SEED_DROPS.get()) expected.put(appeng.api.stacks.AEItemKey.of(seed), chance * cycles);
            if (crop != ModCrops.INFERIUM) expected.put(appeng.api.stacks.AEItemKey.of(new ItemStack(com.blakebr0.mysticalagriculture.init.ModItems.FERTILIZED_ESSENCE.get())),
                Math.clamp(ModConfigs.FERTILIZED_ESSENCE_DROP_CHANCE.get(), 0, 1) * cycles);
        } else {
            int samples = (int)Math.min(64, cycles);
            for (int i = 0; i < samples; i++) for (var drop : nativeHarvest(level, pos, seed, soil))
                expected.merge(appeng.api.stacks.AEItemKey.of(drop), drop.getCount() * (double)cycles / samples, Double::sum);
        }
        var result = new LinkedHashMap<appeng.api.stacks.AEItemKey, Long>();
        expected.forEach((key, amount) -> {
            double total = amount + fractions.getOrDefault(key, 0.0); long whole = (long)Math.floor(total + 1e-9);
            fractions.put(key, Math.max(0, total - whole)); if (whole > 0) result.put(key, whole);
        }); addFallback(level,seed,soil,cycles,result); return result;
    }
    public static List<ItemStack> harvest(ServerLevel level, BlockPos machine, ItemStack seed, ItemStack soil) {
        var drops=new ArrayList<>(nativeHarvest(level,machine,seed,soil));
        if (!NativeSeedYield.reproduces(level,seed,soil) && level.random.nextDouble()<SeedReproduction.FALLBACK_CHANCE) drops.add(seed.copyWithCount(1));
        return drops;
    }
    private static void addFallback(ServerLevel level,ItemStack seed,ItemStack soil,long cycles,Map<appeng.api.stacks.AEItemKey,Long> result) {
        if (!NativeSeedYield.reproduces(level,seed,soil)) {
            long extra=SeedReproduction.roll(cycles,level.random::nextDouble,level.random::nextGaussian);
            if (extra>0) result.merge(appeng.api.stacks.AEItemKey.of(seed.copyWithCount(1)),extra,Math::addExact);
        }
    }
    private static List<ItemStack> nativeHarvest(ServerLevel level, BlockPos machine, ItemStack seed, ItemStack soil) {
        var source=CropSources.find(level,seed); if (source!=null) return source.harvest(level,machine,soil);
        if (io.github.sudabake.appliedagristics.biology.BioData.seed(seed)) return java.util.List.of(io.github.sudabake.appliedagristics.biology.BioData.withIdentity(io.github.sudabake.appliedagristics.Agristics.BIO_ESSENCE.get(), io.github.sudabake.appliedagristics.biology.BioData.identity(seed)));
        var mature = mature(seed);
        if (mature==null) return List.of();
        return inField(level, machine, mature, soil, () -> {
            // Stems keep their plant: harvest their fruit, not the broken stem's seeds.
            if (mature.getBlock() instanceof StemBlock stem) {
                var fruit = ((io.github.sudabake.appliedagristics.mixin.StemAccessor) stem).agrFruit();
                var block = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK).get(fruit);
                return new ArrayList<>(Block.getDrops(block.defaultBlockState(), level, machine.above(), null));
            }
            var drops = new ArrayList<>(Block.getDrops(mature, level, machine.above(), null));
            // Reserve exactly one matching seed for replanting; surplus seeds remain harvest output.
            reserveSeed(drops,seed); return drops;
        });
    }
    public static void reserveSeed(List<ItemStack> drops,ItemStack seed) {
        var reserved=false;
        for (var drop:drops) if (ItemStack.isSameItemSameComponents(drop,seed)) { drop.shrink(1);reserved=true;break; }
        // Native block loot has no copy of a planted stack's name/components. The original
        // seed stays in the machine, so debit one plain seed rather than granting it for free.
        // Never use a different component-bearing variety as the replacement.
        if(!reserved && !seed.getComponentsPatch().isEmpty())
            for(var drop:drops) if(drop.is(seed.getItem()) && drop.getComponentsPatch().isEmpty()) {drop.shrink(1);break;}
        drops.removeIf(ItemStack::isEmpty);
    }
    private Harvests() {}
}
