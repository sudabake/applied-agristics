package io.github.sudabake.appliedagristics.logic;

import io.github.sudabake.appliedagristics.Crops;
import io.github.sudabake.appliedagristics.biology.BioLootCatalog;
import io.github.sudabake.appliedagristics.mixin.SaplingAccessor;
import io.github.sudabake.appliedagristics.mixin.TreeGrowerAccessor;
import io.github.sudabake.appliedagristics.mixin.MegaTreeGrowerAccessor;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Reads native tree definitions without placing a tree or changing the real world. */
public final class NativeTrees {
    private NativeTrees() {}

    public static CropSource find(Level level,ItemStack seed) {
        if (!(level instanceof ServerLevel) || !(seed.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof SaplingBlock sapling)) return null;
        var grower=((SaplingAccessor)sapling).agristicsTreeGrower();
        var features=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var keys = new LinkedHashSet<net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>>>();
        var featureRandom = RandomSource.create(742019L);
        for (int sample = 0; sample < 64; sample++) {
            keys.add(((TreeGrowerAccessor) grower).agristicsTree(featureRandom, false));
            keys.add(((TreeGrowerAccessor) grower).agristicsTree(featureRandom, true));
            if (grower instanceof MegaTreeGrowerAccessor mega) keys.add(mega.agristicsMegaTree(featureRandom));
        }
        keys.remove(null);
        BlockStateProvider primary=null;
        ResourceLocation table=null;
        var logs=new LinkedHashMap<net.minecraft.world.item.Item,ItemStack>();
        var knownDrops=new HashMap<BlockState,List<ItemStack>>();
        var random=RandomSource.create(742019L);
        for (var key:keys) {
            var feature=features.get(key);
            if (feature==null || !(feature.config() instanceof TreeConfiguration tree)) continue;
            for(int sample=0;sample<64;sample++) {
                var state=tree.trunkProvider.getState(random,BlockPos.ZERO);
                var drops=knownDrops.computeIfAbsent(state,value->displayDrops((ServerLevel)level,value));
                for(var stack:drops) {
                    if (primary==null) primary=tree.trunkProvider;
                    if (table==null) table=state.getBlock().getLootTable();
                    logs.putIfAbsent(stack.getItem(),stack);
                }
            }
        }
        return primary==null ? null : new Source(seed.copyWithCount(1),primary,List.copyOf(logs.values()),sapling.defaultBlockState(),table);
    }

    private static List<ItemStack> displayDrops(ServerLevel level,BlockState state) {
        var table=state.getBlock().getLootTable();
        var possible=BioLootCatalog.possibleTableItems(level,table);
        var blockItem=new ItemStack(state.getBlock());
        if(!blockItem.isEmpty() && possible.contains(blockItem.getItem())) return List.of(blockItem);
        // Resolve state-dependent replacements without advancing world RNG or running global loot modifiers.
        var params=new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE,state)
                .withParameter(LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.ZERO)
                .withParameter(LootContextParams.TOOL,ItemStack.EMPTY).create(LootContextParamSets.BLOCK);
        var context=new LootContext.Builder(params).withOptionalRandomSeed(742019L).create(table);
        var result=new ArrayList<ItemStack>();
        level.getServer().getLootData().getLootTable(table).getRandomItemsRaw(context,
                stack->{if(!stack.isEmpty()) result.add(stack.copyWithCount(1));});
        if(result.isEmpty()) possible.stream().sorted(Comparator.comparing(item->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()))
                .map(ItemStack::new).forEach(result::add);
        return List.copyOf(result);
    }

    private record Source(ItemStack seed,BlockStateProvider trunk,List<ItemStack> logs,BlockState display,
                          ResourceLocation table) implements CropSource {
        @Override public ItemStack product() { return logs.get(0).copy(); }
        @Override public List<ItemStack> previewOutputs(ServerLevel level) {
            var result=new ArrayList<>(logs.stream().map(ItemStack::copy).toList());
            result.add(seed.copy());
            return result;
        }
        @Override public ItemStack networkSoil() { return Harvests.networkSoil(); }
        @Override public int ticks() { return 2400; }
        @Override public boolean acceptsSoil(ItemStack soil) { return Crops.farmland(soil); }
        @Override public boolean canGrow(ServerLevel level,BlockPos pos,ItemStack soil) {
            return acceptsSoil(soil) && soil.getItem() instanceof BlockItem
                    && Harvests.inField(level,pos,display,soil,()->display.canSurvive(level,pos.above()));
        }
        @Override public boolean reproduces() { return true; }
        @Override public ResourceLocation cachedLootTable() { return table; }
        @Override public List<ItemStack> harvest(ServerLevel level,BlockPos pos,ItemStack soil) {
            var state=trunk.getState(level.random,pos);
            int blocks=1+level.random.nextInt(4);
            var result=new ArrayList<ItemStack>();
            for(int block=0;block<blocks;block++)
                result.addAll(Harvests.inField(level,pos,state,soil,()->Block.getDrops(state,level,pos.above(),null)));
            if (level.random.nextDouble()<.05) result.add(seed.copy());
            return result;
        }
    }
}
