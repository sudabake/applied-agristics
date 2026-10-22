package io.github.sudabake.appliedagristics.logic;

import appeng.api.stacks.AEItemKey;
import io.github.sudabake.appliedagristics.*;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Cached priority: explicit pack recipe, native biological/MA, optional Botany Pots, native fallback. */
@EventBusSubscriber(modid=Agristics.ID)
public final class CropSources {
    private static final Map<RecipeManager,Map<AEItemKey,Optional<CropSource>>> CACHE=new WeakHashMap<>();
    public static void invalidate() { synchronized(CACHE) { CACHE.clear(); } NativeSeedYield.invalidate(); Cultivations.invalidate(); }
    @SubscribeEvent public static void reload(net.neoforged.neoforge.event.OnDatapackSyncEvent event) { invalidate(); }
    @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event) { invalidate(); }
    public static CropSource find(Level level,ItemStack seed) {
        if (level==null || seed.isEmpty()) return null;
        synchronized(CACHE) {
            return CACHE.computeIfAbsent(level.getRecipeManager(),k->new HashMap<>()).computeIfAbsent(AEItemKey.of(seed.copyWithCount(1)),k->Optional.ofNullable(resolve(level,seed.copyWithCount(1)))).orElse(null);
        }
    }
    private static CropSource resolve(Level level,ItemStack seed) {
        for (var holder:level.getRecipeManager().getAllRecipesFor(Agristics.PLANTING_TYPE.get()).stream().sorted(Comparator.comparing(h->h.id().toString())).toList()) {
            var recipe=holder.value();
            if (recipe.seed().test(seed) && !recipe.outputs().isEmpty()) return new Declared(recipe,seed);
        }
        if (BioData.seed(seed) || Crops.seed(seed)!=null) return null;
        return net.neoforged.fml.ModList.get().isLoaded("botanypots") ? io.github.sudabake.appliedagristics.compat.botany.BotanyCrops.find(level,seed) : null;
    }
    public static BlockState display(Level level,ItemStack seed) { var source=find(level,seed); return source==null ? Harvests.mature(seed) : source.display(); }
    public static boolean soil(Level level,ItemStack seed,ItemStack soil) { var source=find(level,seed); return source==null ? Crops.farmland(soil) : source.acceptsSoil(soil); }
    public static int ticks(Level level,ItemStack seed,int fallback) { var source=find(level,seed); return source==null ? fallback : source.ticks(); }
    public static int ticks(Level level,BlockPos pos,ItemStack seed,ItemStack soil,int fallback) { var source=find(level,seed); return source==null ? fallback : source.ticks(level,pos,soil); }
    private record Declared(PlantingRecipe recipe,ItemStack seed) implements CropSource {
        @Override public ItemStack product() { return recipe.outputs().getFirst().copy(); }
        @Override public BlockState display() { return recipe.display(); }
        @Override public int ticks() { return recipe.ticks(); }
        @Override public ItemStack networkSoil() { return recipe.soil().isEmpty() ? Harvests.networkSoil() : recipe.soil().getItems().length==0 ? ItemStack.EMPTY : recipe.soil().getItems()[0].copyWithCount(1); }
        @Override public boolean acceptsSoil(ItemStack soil) { return recipe.soil().isEmpty() ? Crops.farmland(soil) : recipe.soil().test(soil); }
        @Override public boolean canGrow(ServerLevel level,BlockPos pos,ItemStack soil) {
            return acceptsSoil(soil) && (!recipe.useLoot() || soil.getItem() instanceof BlockItem && Harvests.inField(level,pos,display(),soil,()->display().canSurvive(level,pos.above())));
        }
        @Override public boolean reproduces() { return recipe.reproduces() || !recipe.useLoot() && recipe.outputs().stream().anyMatch(s->ItemStack.isSameItemSameComponents(s,seed)); }
        @Override public List<ItemStack> harvest(ServerLevel level,BlockPos pos,ItemStack soil) {
            if (!recipe.useLoot()) return recipe.outputs().stream().map(ItemStack::copy).toList();
            var drops=Harvests.inField(level,pos,display(),soil,()->new ArrayList<>(Block.getDrops(display(),level,pos.above(),null)));
            Harvests.reserveSeed(drops,seed); return drops;
        }
        @Override public int maximumBatch() { return fixedOutputs() ? Integer.MAX_VALUE : 64; }
        @Override public boolean fixedOutputs() { return !recipe.useLoot(); }
    }
    private CropSources() {}
}
