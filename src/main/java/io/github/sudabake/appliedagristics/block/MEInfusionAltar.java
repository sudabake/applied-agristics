package io.github.sudabake.appliedagristics.block;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.blakebr0.mysticalagriculture.init.ModRecipeTypes;
import com.blakebr0.mysticalagriculture.tileentity.InfusionPedestalTileEntity;
import io.github.sudabake.appliedagristics.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import java.util.*;

/** Replacement center for the ordinary MA infusion altar: eight real external pedestals. */
public final class MEInfusionAltar extends AEProcessingMachine {
    // The public MA altar layout: cardinal distance 3; diagonals distance 2.
    private static final int[][] OFFSETS={{3,0},{0,3},{-3,0},{0,-3},{2,2},{2,-2},{-2,2},{-2,-2}};
    private int progress;
    private ResourceLocation recipeId;
    private List<ItemStack> workInput=List.of();
    private com.blakebr0.mysticalagriculture.tileentity.InfusionAltarTileEntity visualView;

    public MEInfusionAltar(BlockPos pos,BlockState state) { super(Agristics.INFUSION_BE.get(),pos,state,6,1,2); }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox() { return new net.minecraft.world.phys.AABB(worldPosition).inflate(4); }
    @Override protected boolean valid(int slot,ItemStack stack) { return slot==0 || slot>=2 && stack.is(appeng.core.definitions.AEItems.SPEED_CARD.asItem()); }
    @Override protected int slotLimit(int slot) { return slot==0 || slot>=2 ? 1 : 64; }
    @Override protected void inventoryChanged(int slot) {
        if (slot==0) reset();
        if (slot==1 && items.getStackInSlot(1).isEmpty()) craftingDelivery=false;
        if (slot<2 && level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }
    public com.blakebr0.mysticalagriculture.tileentity.InfusionAltarTileEntity visualView() {
        if (visualView==null) visualView=new com.blakebr0.mysticalagriculture.tileentity.InfusionAltarTileEntity(worldPosition,com.blakebr0.mysticalagriculture.init.ModBlocks.INFUSION_ALTAR.get().defaultBlockState());
        visualView.setLevel(level);
        visualView.getInventory().getStacks().set(0,items.getStackInSlot(0));
        visualView.getInventory().getStacks().set(1,items.getStackInSlot(1));
        return visualView;
    }
    @Override public CompoundTag getUpdateTag() {
        var tag=new CompoundTag(); tag.put("center",items.getStackInSlot(0).save(new CompoundTag())); tag.put("result",items.getStackInSlot(1).save(new CompoundTag())); return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag) { items.setStackInSlot(0,ItemStack.of(tag.getCompound("center"))); items.setStackInSlot(1,ItemStack.of(tag.getCompound("result"))); }
    private void reset() { progress=0; recipeId=null; workInput=List.of(); }
    public List<BlockPos> pedestalPositions() { return Arrays.stream(OFFSETS).map(o -> worldPosition.offset(o[0],0,o[1])).toList(); }
    @Override public void step() {
        deliverOutput();
        if (!items.getStackInSlot(1).isEmpty()) { status="output_full"; return; }
        if (!getMainNode().isActive()) { status="offline"; return; }
        if (items.getStackInSlot(0).isEmpty()) { reset(); status="missing_inputs"; return; }
        var pedestals=new ArrayList<InfusionPedestalTileEntity>();
        var inputItems=new ArrayList<ItemStack>(); inputItems.add(items.getStackInSlot(0).copy());
        for (var pos:pedestalPositions()) {
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof InfusionPedestalTileEntity pedestal)) {
                reset(); status="missing_infusion_pedestals"; return;
            }
            pedestals.add(pedestal); inputItems.add(pedestal.getInventory().getStackInSlot(0).copy());
        }
        var input=infusionInputs(inputItems);
        var holder=level.getRecipeManager().getRecipeFor(ModRecipeTypes.INFUSION.get(), input, level).orElseGet(() ->
            level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.INFUSION.get()).stream()
                .filter(recipe -> recipe.getIngredients().size() < input.getContainerSize() && matchesInfusion(recipe, input))
                .findFirst().orElse(null));
        if (holder==null) { reset(); status="missing_inputs"; return; }
        boolean same=holder.getId().equals(recipeId) && workInput.size()==inputItems.size();
        if (same) for (int i=0;i<inputItems.size();i++) if (!ItemStack.matches(inputItems.get(i),workInput.get(i))) { same=false; break; }
        if (!same) { progress=0; recipeId=holder.getId(); workInput=inputItems.stream().map(ItemStack::copy).toList(); }
        status="infusing"; progress=Math.min(Balance.ALTAR_TICKS.get(),progress+speed()); setChanged();
        if (progress<Balance.ALTAR_TICKS.get()) {
            for (var pedestal:pedestals) InfusionEffects.converge((net.minecraft.server.level.ServerLevel)level,worldPosition,pedestal.getBlockPos(),pedestal.getInventory().getStackInSlot(0));
            return;
        }
        // Calculate every result before mutating the real pedestal inventories.
        var recipe=holder; var output=recipe.assemble(input,level.registryAccess()); var remaining = recipe instanceof com.blakebr0.cucumber.crafting.ISpecialRecipe special
                ? special.getRemainingItems(new net.minecraftforge.items.wrapper.InvWrapper(input)) : recipe.getRemainingItems(input);
        if (output.isEmpty() || remaining.size()!=input.getContainerSize()) { reset(); status="missing_inputs"; return; }
        if (output.getCount()>output.getMaxStackSize()) { status="output_full"; return; }
        if (remaining.get(0).getCount()>1) { status="output_full"; return; }
        for (int i=0;i<8;i++) {
            var rest=remaining.get(i+1);
            if (rest.getCount()>pedestals.get(i).getInventory().getSlotLimit(0)) { status="output_full"; return; }
        }
        for (int i=0;i<8;i++) {
            pedestals.get(i).getInventory().setStackInSlot(0,remaining.get(i+1).copy());
            InfusionEffects.finishPedestal((net.minecraft.server.level.ServerLevel)level,pedestals.get(i).getBlockPos());
        }
        items.setStackInSlot(0,remaining.get(0).copy()); items.setStackInSlot(1,output.copy()); reset(); setChanged();
        InfusionEffects.finishCenter((net.minecraft.server.level.ServerLevel)level,worldPosition);
    }
    @Override public boolean acceptsPlans() {
        if (level==null || !getMainNode().isActive() || !items.getStackInSlot(0).isEmpty() || !items.getStackInSlot(1).isEmpty()) return false;
        for (var pos:pedestalPositions()) if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof InfusionPedestalTileEntity pedestal) || !pedestal.getInventory().getStackInSlot(0).isEmpty()) return false;
        return true;
    }
    @Override public boolean pushPattern(appeng.api.crafting.IPatternDetails pattern,appeng.api.stacks.KeyCounter[] inputs,Direction side) {
        if (!acceptsPlans() || pattern==null || pattern.getOutputs().length!=1) return false;
        var pool=new LinkedHashMap<AEItemKey,Long>(); long total=0;
        for (var counter:inputs) for (var entry:counter) {
            if (!(entry.getKey() instanceof AEItemKey key) || entry.getLongValue()<=0 || entry.getLongValue()>9) return false;
            total+=entry.getLongValue(); if (total>9) return false; pool.merge(key,entry.getLongValue(),Long::sum);
        }
        for (var holder:level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.INFUSION.get())) {
            var recipe=holder; var output=recipe.getResultItem(level.registryAccess()); var requested=pattern.getPrimaryOutput();
            if (!(requested.what() instanceof AEItemKey key) || key.getItem()!=output.getItem() || requested.amount()!=output.getCount() || recipe.getIngredients().isEmpty() || recipe.getIngredients().size()>9) continue;
            for (var center:List.copyOf(pool.keySet())) {
                if (!recipe.getIngredients().get(0).test(center.toStack(1))) continue;
                var remaining=new LinkedHashMap<>(pool); remaining.put(center,remaining.get(center)-1);
                var arranged=new ArrayList<ItemStack>(Collections.nCopies(8,ItemStack.EMPTY));
                if (!arrange(recipe.getIngredients().subList(1, recipe.getIngredients().size()),0,remaining,arranged) || remaining.values().stream().anyMatch(n->n!=0)) continue;
                var all=new ArrayList<ItemStack>(); all.add(center.toStack(1)); all.addAll(arranged);
                var input = infusionInputs(all);
                if (!matchesInfusion(recipe, input)) continue;
                var actual=recipe.assemble(input,level.registryAccess());
                if (!key.matches(actual) || actual.getCount()!=requested.amount()) continue;
                // Publish all nine physical slots only after a complete recipe match. Failed pushes mutate nothing.
                for (int i=0;i<8;i++) ((InfusionPedestalTileEntity)level.getBlockEntity(pedestalPositions().get(i))).getInventory().setStackInSlot(0,arranged.get(i).copy());
                items.setStackInSlot(0,center.toStack(1)); craftingDelivery=true; setChanged(); return true;
            }
        }
        return false;
    }
    private static net.minecraft.world.SimpleContainer infusionInputs(List<ItemStack> items) {
        return new net.minecraft.world.SimpleContainer(items.toArray(ItemStack[]::new));
    }
    private boolean matchesInfusion(com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe recipe,
                                    net.minecraft.world.Container input) {
        int size = recipe.getIngredients().size();
        if (size >= input.getContainerSize()) return recipe.matches(input, level);
        if (size == 0) return false;
        // MA 7's matcher requires a grid of the same length as the ingredient list.
        // Keep the physical grid for assembly and route remainders back to its original slots.
        var compact = new net.minecraft.world.SimpleContainer(size);
        compact.setItem(0, input.getItem(0));
        int slot = 1;
        for (int i = 1; i < input.getContainerSize(); i++) {
            var stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (slot == size) return false;
            compact.setItem(slot++, stack);
        }
        return recipe.matches(compact, level);
    }
    private static boolean arrange(List<net.minecraft.world.item.crafting.Ingredient> ingredients,int index,Map<AEItemKey,Long> pool,List<ItemStack> result) {
        if (index==ingredients.size()) return true;
        var ingredient=ingredients.get(index);
        if (ingredient.isEmpty()) { result.set(index,ItemStack.EMPTY); return arrange(ingredients,index+1,pool,result); }
        for (var key:pool.keySet()) if (pool.get(key)>0 && ingredient.test(key.toStack(1))) {
            pool.put(key,pool.get(key)-1); result.set(index,key.toStack(1));
            if (arrange(ingredients,index+1,pool,result)) return true;
            pool.put(key,pool.get(key)+1);
        }
        result.set(index,ItemStack.EMPTY); return false;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putInt("progress",progress);
        if (recipeId!=null) tag.putString("recipe",recipeId.toString());
        var saved=new net.minecraft.nbt.ListTag(); for (var stack:workInput) saved.add(stack.save(new CompoundTag())); tag.put("workInput",saved);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); progress=Math.max(0,tag.getInt("progress")); recipeId=ResourceLocation.tryParse(tag.getString("recipe"));
        var saved=new ArrayList<ItemStack>(); for (var raw:tag.getList("workInput",10)) saved.add(ItemStack.of((CompoundTag)raw)); workInput=List.copyOf(saved);
    }
    @Override public CompoundTag snapshot(int page,String search) {
        var tag=super.snapshot(page,search); tag.putInt("progress",progress); tag.putInt("duration",Balance.ALTAR_TICKS.get());
        return tag;
    }
}
