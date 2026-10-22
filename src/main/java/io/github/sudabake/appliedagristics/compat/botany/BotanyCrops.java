package io.github.sudabake.appliedagristics.compat.botany;

import io.github.sudabake.appliedagristics.logic.*;
import net.darkhax.botanypots.common.api.context.BotanyPotContext;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil;
import net.darkhax.botanypots.common.impl.Helpers;
import net.darkhax.botanypots.common.impl.data.recipe.crop.BasicCrop;
import net.darkhax.botanypots.common.impl.data.recipe.crop.BlockDerivedCrop;
import net.darkhax.botanypots.common.impl.data.display.types.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Optional API bridge. Uses actual onHarvest callbacks; never scales a sampled custom callback. */
public final class BotanyCrops {
    public static CropSource find(Level level,ItemStack seed) {
        for (var holder:level.getRecipeManager().getAllRecipesFor(Crop.TYPE.get())) {
            var crop=holder.value();
            // Auto-derived recipes duplicate native crop loot. Keep existing native balance there.
            var nativePlant=Harvests.mature(seed);
            if (crop instanceof BlockDerivedCrop && nativePlant!=null && (nativePlant.getBlock() instanceof net.minecraft.world.level.block.BushBlock || nativePlant.getBlock() instanceof net.minecraft.world.level.block.CropBlock)) continue;
            var probe=new Context(level,BlockPos.ZERO,ItemStack.EMPTY,seed,crop,null,0);
            if (crop instanceof BasicCrop basic ? !basic.getBasicProperties().input().test(seed) : !crop.couldMatch(seed,probe,level)) continue;
            if (!(crop instanceof BasicCrop basic)) return null; // Custom soil discovery requires an explicit planting recipe.
            var properties=basic.getBasicProperties();
            ItemStack soil=ItemStack.EMPTY;
            for (var candidate:properties.soil().getItems()) {
                var context=new Context(level,BlockPos.ZERO,candidate,seed,crop,null,0);
                if (Soil.getSoil(level,context,candidate)!=null) { soil=candidate.copyWithCount(1); break; }
            }
            var products=properties.drops().stream().flatMap(p->p.getDisplayItems().stream()).filter(s->!s.isEmpty()).toList();
            var product=products.stream().filter(s->!ItemStack.isSameItemSameComponents(s,seed)).findFirst().orElse(products.isEmpty()?ItemStack.EMPTY:products.getFirst());
            if (product.isEmpty()) product=seed.copyWithCount(1);
            boolean reproduces=products.stream().anyMatch(s->ItemStack.isSameItemSameComponents(s,seed));
            return new Source(crop,seed.copyWithCount(1),soil,product.copyWithCount(1),reproduces,Math.max(20,properties.growTime()),state(properties.display()));
        }
        return null;
    }
    private static BlockState state(List<net.darkhax.botanypots.common.api.data.display.types.Display> displays) {
        for (int i=displays.size()-1;i>=0;i--) {
            var display=displays.get(i);
            if (display instanceof SimpleDisplayState simple) return simple.getState();
            if (display instanceof PhasedDisplayState phased) { var found=state(phased.getDisplayPhases()); if (found!=null) return found; }
        }
        return null;
    }
    private record Source(Crop crop,ItemStack seed,ItemStack networkSoil,ItemStack product,boolean reproduces,int ticks,BlockState display) implements CropSource {
        private Context context(Level level,BlockPos pos,ItemStack soil) {
            var probe=new Context(level,pos,soil,seed,crop,null,ticks);
            var match=Soil.getSoil(level,probe,soil);
            if (match==null) return null;
            var context=new Context(level,pos,soil,seed,crop,match.value(),ticks);
            return new Context(level,pos,soil,seed,crop,match.value(),Math.max(20,Helpers.getRequiredGrowthTicks(context,level,crop,match.value())));
        }
        @Override public int ticks(Level level,BlockPos pos,ItemStack soil) { var context=context(level,pos,soil); return context==null ? ticks : context.requiredGrowthTicks; }
        @Override public boolean acceptsSoil(ItemStack soil) { return crop instanceof BasicCrop basic && basic.getBasicProperties().soil().test(soil); }
        @Override public boolean canGrow(ServerLevel level,BlockPos pos,ItemStack soil) {
            var context=context(level,pos,soil);
            return context!=null && acceptsSoil(soil) && context.soil.matches(context,level) && crop.matches(context,level) && crop.isGrowthSustained(context,level) && crop.canHarvest(context,level);
        }
        @Override public List<ItemStack> harvest(ServerLevel level,BlockPos pos,ItemStack soil) {
            var context=context(level,pos,soil); if (context==null || !canGrow(level,pos,soil)) return List.of();
            var result=new ArrayList<ItemStack>();
            int rolls=Helpers.getLootRolls(context,level,crop,context.soil);
            for (int i=0;i<rolls;i++) crop.onHarvest(context,level,stack->{ if (!stack.isEmpty()) result.add(stack.copy()); });
            return result;
        }
    }
    private record Context(Level level,BlockPos pos,ItemStack soilItem,ItemStack seedItem,Crop crop,Soil soil,int requiredGrowthTicks) implements BotanyPotContext {
        @Override public ItemStack getSoilItem() { return soilItem; }
        @Override public ItemStack getSeedItem() { return seedItem; }
        @Override public ItemStack getHarvestItem() { return ItemStack.EMPTY; }
        @Override public LootParams createLootParams(BlockState state) { return new LootParams.Builder((ServerLevel)level).withParameter(LootContextParams.BLOCK_STATE,state==null?level.getBlockState(pos):state).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,ItemStack.EMPTY).create(LootContextParamSets.BLOCK); }
        @Override public void runFunction(ResourceLocation id) {
            if (level instanceof ServerLevel server) server.getServer().getCommands().performPrefixedCommand(server.getServer().createCommandSourceStack().withLevel(server).withPosition(Vec3.atCenterOf(pos)).withSuppressedOutput(),"function "+id);
        }
        @Override public Player getPlayer() { return null; }
        @Override public ItemStack getInteractionItem() { return ItemStack.EMPTY; }
        @Override public int getRequiredGrowthTicks() { return requiredGrowthTicks; }
        @Override public boolean isServerThread() { return !level.isClientSide; }
        @Override public Crop getCrop() { return crop; }
        @Override public Soil getSoil() { return soil; }
        @Override public ItemStack getItem(int slot) { return slot==0?soilItem:slot==1?seedItem:ItemStack.EMPTY; }
        @Override public int size() { return 3; }
    }
    private BotanyCrops() {}
}
